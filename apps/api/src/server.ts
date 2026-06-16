import { serve } from "@hono/node-server";
import { Hono } from "hono";
import { cors } from "hono/cors";
import {
  notmidOpenApiDocument,
  resolveNotmidPathStack,
  type NotmidCapturePublishRequest,
  type NotmidProfileSettingsUpdateRequest,
  type NotmidSendThreadMessageRequest,
  type NotmidSignInRequest,
  type NotmidStartThreadRequest,
} from "@notmid/contracts";
import {
  authStatusForContext,
  canUseFakeSignIn,
  fakeSignInResponse,
  isAuthorizedWrite,
  parseAuthProvider,
} from "./authPolicy";
import {
  errorResponse,
  logRequest,
  logUnhandledError,
  pathFromUrl,
  resolveRequestId,
} from "./diagnostics";
import {
  applyApiSecurityHeaders,
  applyRateLimitHeaders,
  authRequiredResponse,
  createRuntimeFirebaseTokenVerifier,
  createRuntimeRepository,
  currentRequestId,
  isMutableApiRequest,
  optionalRepositoryActorForAuthContext,
  rateLimitKeyForContext,
  readJsonObject,
  readRuntimeConfig,
  repositoryActorForAuthContext,
  respondProtectedWriteResult,
  respondRepositoryResult,
  respondThreadInvite,
  resolveApiAuthContextForRequest,
  type NotmidApiEnv,
} from "./serverSupport";
import { createFixedWindowRateLimiter } from "./rateLimit";
import type { Context } from "hono";

const app = new Hono<NotmidApiEnv>();
const runtimeConfig = readRuntimeConfig();
const configValidationOnly = process.env.NOTMID_VALIDATE_CONFIG_ONLY === "true";

if (configValidationOnly) {
  console.log(
    `notmid API config valid for ${runtimeConfig.nodeEnv} with ${runtimeConfig.authMode} auth and ${runtimeConfig.dataBackend} data`,
  );
  process.exit(0);
}

const repository = createRuntimeRepository(runtimeConfig);
const firebaseTokenVerifier = createRuntimeFirebaseTokenVerifier(runtimeConfig);
const mutationRateLimiter = createFixedWindowRateLimiter(runtimeConfig.mutationRateLimit);
const threadInviteDeps = {
  repository,
  resolveAuthContext: resolveApiAuthContext,
};

function resolveApiAuthContext(context: Context<NotmidApiEnv>) {
  return resolveApiAuthContextForRequest(context, {
    authMode: runtimeConfig.authMode,
    firebaseTokenVerifier,
  });
}

app.use("*", async (context, next) => {
  const requestId = resolveRequestId(context.req.header("x-request-id"));
  const startedAt = performance.now();

  context.set("requestId", requestId);
  context.set("requestStartedAt", startedAt);
  context.header("x-request-id", requestId);

  await next();

  context.header("x-request-id", requestId);
  logRequest({
    durationMs: Math.round(performance.now() - startedAt),
    method: context.req.method,
    path: pathFromUrl(context.req.url),
    requestId,
    status: context.res.status,
  });
});

app.use("*", async (context, next) => {
  applyApiSecurityHeaders(context);
  await next();
  applyApiSecurityHeaders(context);
});

app.use(
  "*",
  cors({
    origin: runtimeConfig.webOrigin,
    allowMethods: ["GET", "POST", "PATCH", "OPTIONS"],
    allowHeaders: ["content-type", "authorization", "x-request-id"],
  }),
);

app.use("*", async (context, next) => {
  if (!isMutableApiRequest(context)) {
    await next();
    return;
  }

  const decision = mutationRateLimiter.check(rateLimitKeyForContext(context));
  applyRateLimitHeaders(context, decision);

  if (!decision.allowed) {
    return context.json(
      errorResponse(
        "rate_limited",
        "Too many mutating API requests. Try again later.",
        currentRequestId(context),
      ),
      429,
    );
  }

  await next();
});

app.get("/health", (context) =>
  context.json({
    ok: true,
    requestId: context.get("requestId"),
    service: "notmid-api",
    mode: runtimeConfig.authMode,
  }),
);

if (runtimeConfig.diagnosticFailureEnabled) {
  app.get("/__notmid/diagnostic-error", () => {
    throw new Error("Notmid diagnostic failure");
  });
}

app.get("/openapi.json", (context) => context.json(notmidOpenApiDocument));

app.get("/v1/auth/status", async (context) => {
  const authContext = await resolveApiAuthContext(context);

  return context.json(authStatusForContext(authContext));
});

app.post("/v1/auth/fake-sign-in", async (context) => {
  const mode = runtimeConfig.authMode;

  if (!canUseFakeSignIn(mode)) {
    return context.json(
      {
        error: {
          code: "fake_auth_disabled",
          message: "Local fake sign-in is only available when NOTMID_AUTH_MODE=fake.",
        },
      },
      409,
    );
  }

  const requestBody = await readJsonObject<Partial<NotmidSignInRequest>>(context);

  if (!requestBody.ok) {
    return requestBody.response;
  }

  const request = requestBody.value;
  const provider = parseAuthProvider(request.provider ?? "fake");

  if (!provider) {
    return context.json(
      { error: { code: "invalid_auth_provider", message: "Unsupported auth provider." } },
      400,
    );
  }

  return context.json(fakeSignInResponse({ ...request, provider }));
});

app.get("/v1/profile/settings", async (context) => {
  const authContext = await resolveApiAuthContext(context);

  if (!isAuthorizedWrite(authContext)) {
    return context.json(
      {
        error: {
          code: "auth_required",
          message: "Profile settings require an authenticated notmid session.",
        },
      },
      401,
    );
  }

  return context.json(
    await repository.getProfileSettings(repositoryActorForAuthContext(authContext)),
  );
});

app.patch("/v1/profile/settings", async (context) => {
  const authContext = await resolveApiAuthContext(context);

  if (!isAuthorizedWrite(authContext)) {
    return authRequiredResponse(
      context,
      authContext,
      "profile_settings_update",
      "Profile settings require an authenticated notmid session.",
    );
  }

  const requestBody = await readJsonObject<Partial<NotmidProfileSettingsUpdateRequest>>(context);

  if (!requestBody.ok) {
    return requestBody.response;
  }

  const result = await repository.updateProfileSettings(
    requestBody.value,
    repositoryActorForAuthContext(authContext),
  );

  return respondProtectedWriteResult(context, result, authContext, "profile_settings_update");
});

app.get("/v1/capture/draft", async (context) => context.json(await repository.getCaptureDraft()));

app.post("/v1/capture/publish", async (context) => {
  const authContext = await resolveApiAuthContext(context);

  if (!isAuthorizedWrite(authContext)) {
    return authRequiredResponse(
      context,
      authContext,
      "capture_publish",
      "Publishing a receipt requires an authenticated notmid session.",
    );
  }

  const requestBody = await readJsonObject<Partial<NotmidCapturePublishRequest>>(context);

  if (!requestBody.ok) {
    return requestBody.response;
  }

  const result = await repository.publishCapture(
    requestBody.value,
    repositoryActorForAuthContext(authContext),
  );

  return respondProtectedWriteResult(context, result, authContext, "capture_publish");
});

app.get("/v1/feed", async (context) => context.json(await repository.getFeed()));

app.get("/v1/map", async (context) => context.json(await repository.getMap()));

app.get("/v1/clips/:clipId", async (context) => {
  const result = await repository.getClip(context.req.param("clipId"));

  return respondRepositoryResult(context, result);
});

app.post("/v1/clips/:clipId/save", async (context) => {
  const authContext = await resolveApiAuthContext(context);

  if (!isAuthorizedWrite(authContext)) {
    return authRequiredResponse(
      context,
      authContext,
      "clip_save",
      "Saving a clip requires an authenticated notmid session.",
    );
  }

  const result = await repository.saveClip(
    context.req.param("clipId"),
    repositoryActorForAuthContext(authContext),
  );

  return respondProtectedWriteResult(context, result, authContext, "clip_save");
});

app.get("/v1/places/:placeId", async (context) => {
  const result = await repository.getPlace(context.req.param("placeId"));

  return respondRepositoryResult(context, result);
});

app.get("/v1/inbox/threads", async (context) => {
  const authContext = await resolveApiAuthContext(context);

  return context.json(await repository.getInbox(optionalRepositoryActorForAuthContext(authContext)));
});

app.post("/v1/inbox/threads", async (context) => {
  const authContext = await resolveApiAuthContext(context);

  if (!isAuthorizedWrite(authContext)) {
    return authRequiredResponse(
      context,
      authContext,
      "thread_start",
      "Starting a chat requires an authenticated notmid session.",
    );
  }

  const requestBody = await readJsonObject<Partial<NotmidStartThreadRequest>>(context);

  if (!requestBody.ok) {
    return requestBody.response;
  }

  const result = await repository.startThread(
    requestBody.value,
    repositoryActorForAuthContext(authContext),
  );

  return respondProtectedWriteResult(context, result, authContext, "thread_start");
});

app.get("/v1/inbox/threads/:threadId", async (context) => {
  const authContext = await resolveApiAuthContext(context);
  const result = await repository.getThread(
    context.req.param("threadId"),
    optionalRepositoryActorForAuthContext(authContext),
  );

  return respondRepositoryResult(context, result);
});

app.get("/v1/inbox/threads/:threadId/detail", async (context) => {
  const authContext = await resolveApiAuthContext(context);
  const result = await repository.getThreadDetail(
    context.req.param("threadId"),
    optionalRepositoryActorForAuthContext(authContext),
  );

  return respondRepositoryResult(context, result);
});

app.post("/v1/inbox/threads/:threadId/invite/accept", async (context) => {
  return respondThreadInvite(context, "accept", "thread_invite_accept", threadInviteDeps);
});

app.post("/v1/inbox/threads/:threadId/invite/reject", async (context) => {
  return respondThreadInvite(context, "reject", "thread_invite_reject", threadInviteDeps);
});

app.post("/v1/inbox/threads/:threadId/messages", async (context) => {
  const authContext = await resolveApiAuthContext(context);

  if (!isAuthorizedWrite(authContext)) {
    return authRequiredResponse(
      context,
      authContext,
      "thread_message_send",
      "Sending a chat message requires an authenticated notmid session.",
    );
  }

  const requestBody = await readJsonObject<Partial<NotmidSendThreadMessageRequest>>(context);

  if (!requestBody.ok) {
    return requestBody.response;
  }

  const result = await repository.sendThreadMessage(
    context.req.param("threadId"),
    requestBody.value,
    repositoryActorForAuthContext(authContext),
  );

  return respondProtectedWriteResult(context, result, authContext, "thread_message_send");
});

app.get("/v1/deeplinks/resolve", (context) => {
  const url = context.req.query("url") ?? "/notmid";
  return context.json(resolveNotmidPathStack(url));
});

app.notFound((context) =>
  context.json(errorResponse("route_not_found", "Route not found.", currentRequestId(context)), 404),
);

app.onError((error, context) => {
  const requestId = currentRequestId(context);

  context.header("x-request-id", requestId);
  logUnhandledError(error, {
    method: context.req.method,
    path: pathFromUrl(context.req.url),
    requestId,
    status: 500,
  });

  return context.json(
    errorResponse(
      "internal_error",
      "The API request failed. Use the request id when reporting this issue.",
      requestId,
    ),
    500,
  );
});

serve(
  {
    fetch: app.fetch,
    port: runtimeConfig.port,
  },
  (info) => {
    console.log(`notmid API listening on http://localhost:${info.port}`);
  },
);
