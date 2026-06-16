import type { NotmidAuthMode, NotmidChatInviteDecision } from "@notmid/contracts";
import type { Context } from "hono";
import {
  isAuthorizedWrite,
  resolveAuthContext,
  type NotmidApiAuthContext,
} from "./authPolicy";
import { errorResponse, logAuditEvent, pathFromUrl, resolveRequestId } from "./diagnostics";
import { createFirebaseTokenVerifier, type FirebaseIdTokenVerifier } from "./firebaseTokenVerifier";
import {
  createFixtureNotmidRepository,
  localFixtureActor,
  type NotmidRepository,
  type NotmidRepositoryActor,
  type NotmidRepositoryResult,
} from "./notmidRepository";
import { createPostgresNotmidRepository } from "./postgresNotmidRepository";
import { createRuntimePostgresQueryClient } from "./postgresQueryClient";
import type { NotmidRateLimitDecision } from "./rateLimit";
import { createRuntimeConfig, type NotmidApiRuntimeConfig } from "./runtimeConfig";

export type NotmidApiEnv = {
  Variables: {
    requestId: string;
    requestStartedAt: number;
  };
};

export type JsonObjectReadResult<T> =
  | {
      ok: true;
      value: T;
    }
  | {
      ok: false;
      response: Response;
    };

export type NotmidProtectedWriteAction =
  | "profile_settings_update"
  | "capture_publish"
  | "clip_save"
  | "thread_invite_accept"
  | "thread_invite_reject"
  | "thread_start"
  | "thread_message_send";

export type ApiAuthContextDeps = {
  authMode: NotmidAuthMode;
  firebaseTokenVerifier?: FirebaseIdTokenVerifier;
};

export type ThreadInviteDeps = {
  repository: NotmidRepository;
  resolveAuthContext: (context: Context<NotmidApiEnv>) => Promise<NotmidApiAuthContext>;
};

export function readRuntimeConfig(): NotmidApiRuntimeConfig {
  const config = createRuntimeConfig(process.env);

  if (config.errors.length === 0) {
    return config.value;
  }

  for (const error of config.errors) {
    console.error(`notmid API config error: ${error}`);
  }

  process.exit(1);
}

export async function readJsonObject<T>(
  context: Context<NotmidApiEnv>,
): Promise<JsonObjectReadResult<T>> {
  try {
    const body = await context.req.json();

    if (isJsonObject(body)) {
      return {
        ok: true,
        value: body as T,
      };
    }
  } catch {
  }

  return {
    ok: false,
    response: context.json(
      errorResponse(
        "invalid_json",
        "Request body must be a valid JSON object.",
        currentRequestId(context),
      ),
      400,
    ),
  };
}

export function respondRepositoryResult<T>(
  context: Context<NotmidApiEnv>,
  result: NotmidRepositoryResult<T>,
): Response {
  if (!result.ok) {
    return context.json({ error: result.error }, result.status);
  }

  return context.json(result.value);
}

export function respondProtectedWriteResult<T>(
  context: Context<NotmidApiEnv>,
  result: NotmidRepositoryResult<T>,
  authContext: NotmidApiAuthContext,
  action: NotmidProtectedWriteAction,
): Response {
  const status = result.ok ? 200 : result.status;

  auditProtectedWrite(context, authContext, action, status);
  return respondRepositoryResult(context, result);
}

export function authRequiredResponse(
  context: Context<NotmidApiEnv>,
  authContext: NotmidApiAuthContext,
  action: NotmidProtectedWriteAction,
  message: string,
): Response {
  auditProtectedWrite(context, authContext, action, 401);

  return context.json(
    {
      error: {
        code: "auth_required",
        message,
      },
    },
    401,
  );
}

export async function respondThreadInvite(
  context: Context<NotmidApiEnv>,
  decision: NotmidChatInviteDecision,
  action: NotmidProtectedWriteAction,
  deps: ThreadInviteDeps,
): Promise<Response> {
  const authContext = await deps.resolveAuthContext(context);

  if (!isAuthorizedWrite(authContext)) {
    return authRequiredResponse(
      context,
      authContext,
      action,
      "Responding to a chat request requires an authenticated notmid session.",
    );
  }

  const threadId = context.req.param("threadId");
  if (!threadId) {
    auditProtectedWrite(context, authContext, action, 404);
    return context.json(
      errorResponse("thread_not_found", "Thread not found.", currentRequestId(context)),
      404,
    );
  }

  const result = await deps.repository.respondThreadInvite(
    threadId,
    decision,
    repositoryActorForAuthContext(authContext),
  );

  return respondProtectedWriteResult(context, result, authContext, action);
}

export function currentRequestId(context: Context<NotmidApiEnv>): string {
  return context.get("requestId") ?? resolveRequestId(context.req.header("x-request-id"));
}

export function applyApiSecurityHeaders(context: Context<NotmidApiEnv>): void {
  context.header("referrer-policy", "no-referrer");
  context.header("x-content-type-options", "nosniff");
  context.header("x-frame-options", "DENY");
}

export function isMutableApiRequest(context: Context<NotmidApiEnv>): boolean {
  const method = context.req.method.toUpperCase();
  return (method === "POST" || method === "PATCH") && pathFromUrl(context.req.url).startsWith("/v1/");
}

export function rateLimitKeyForContext(context: Context<NotmidApiEnv>): string {
  const forwardedFor = context.req.header("x-forwarded-for")?.split(",", 1)[0]?.trim();
  const directIp =
    forwardedFor ||
    context.req.header("cf-connecting-ip")?.trim() ||
    context.req.header("x-real-ip")?.trim();
  const authorization = context.req.header("authorization")?.trim();

  return directIp || authorization || "anonymous";
}

export function applyRateLimitHeaders(
  context: Context<NotmidApiEnv>,
  decision: NotmidRateLimitDecision,
): void {
  context.header("retry-after", String(decision.retryAfterSeconds));
  context.header("x-ratelimit-limit", String(decision.limit));
  context.header("x-ratelimit-remaining", String(decision.remaining));
  context.header("x-ratelimit-reset", String(Math.ceil(decision.resetAt / 1000)));
}

export function createRuntimeFirebaseTokenVerifier(
  config: NotmidApiRuntimeConfig,
): FirebaseIdTokenVerifier | undefined {
  if (config.authMode !== "firebase" || !config.firebaseProjectId) {
    return undefined;
  }

  return createFirebaseTokenVerifier({
    projectId: config.firebaseProjectId,
  });
}

export function createRuntimeRepository(config: NotmidApiRuntimeConfig) {
  if (config.dataBackend === "fixture") {
    return createFixtureNotmidRepository(apiGeneratedAt);
  }

  if (!config.databaseUrl) {
    throw new Error("DATABASE_URL is required when NOTMID_DATA_BACKEND=postgres.");
  }

  return createPostgresNotmidRepository(
    createRuntimePostgresQueryClient({
      databaseUrl: config.databaseUrl,
    }),
    {
      generatedAt: apiGeneratedAt,
    },
  );
}

export async function resolveApiAuthContextForRequest(
  context: Context<NotmidApiEnv>,
  deps: ApiAuthContextDeps,
) {
  return resolveAuthContext(deps.authMode, context.req.header("authorization"), {
    firebaseTokenVerifier: deps.firebaseTokenVerifier,
  });
}

export function repositoryActorForAuthContext(
  context: NotmidApiAuthContext,
): NotmidRepositoryActor {
  if (!context.user) {
    return localFixtureActor();
  }

  return {
    avatarImageUrl: context.user.avatarImageUrl,
    displayName: context.user.displayName,
    handle: context.user.handle,
    homeNeighborhood: context.user.homeNeighborhood,
    roles: context.user.roles,
    userId: context.user.id,
  };
}

export function optionalRepositoryActorForAuthContext(
  context: NotmidApiAuthContext,
): NotmidRepositoryActor | undefined {
  return context.user ? repositoryActorForAuthContext(context) : undefined;
}

function isJsonObject(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function auditProtectedWrite(
  context: Context<NotmidApiEnv>,
  authContext: NotmidApiAuthContext,
  action: NotmidProtectedWriteAction,
  status: number,
): void {
  logAuditEvent({
    action,
    actorId: authContext.user?.id,
    authMode: authContext.mode,
    method: context.req.method,
    outcome: auditOutcomeForStatus(status),
    path: pathFromUrl(context.req.url),
    requestId: currentRequestId(context),
    status,
  });
}

function auditOutcomeForStatus(status: number): "success" | "denied" | "failed" {
  if (status >= 200 && status < 300) {
    return "success";
  }

  if (status === 401 || status === 403) {
    return "denied";
  }

  return "failed";
}

function apiGeneratedAt(): string {
  return new Date().toISOString();
}
