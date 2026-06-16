import {
  notmidAuthRequiredActions,
  notmidFakeAccessToken,
  notmidFakeAuthSession,
  notmidFakeAuthStatus,
  notmidFakeAuthUser,
  notmidRoutes,
  notmidSignedOutAuthStatus,
  type NotmidAuthMode,
  type NotmidAuthProvider,
  type NotmidAuthStatusResponse,
  type NotmidSignInRequest,
  type NotmidSignInResponse,
  type NotmidAuthUser,
} from "@notmid/contracts";
import type { FirebaseIdTokenVerifier, FirebaseVerifiedIdToken } from "./firebaseTokenVerifier";

export type NotmidApiAuthContext = {
  authenticated: boolean;
  mode: NotmidAuthMode;
  provider?: NotmidAuthProvider;
  sessionExpiresAt?: string;
  tokenKind?: "fake-local" | "firebase-id-token" | "firebase-unverified";
  user?: NotmidAuthUser;
};

export type ResolveAuthContextOptions = {
  firebaseTokenVerifier?: FirebaseIdTokenVerifier;
};

export async function resolveAuthContext(
  mode: NotmidAuthMode,
  authorization: string | undefined,
  options: ResolveAuthContextOptions = {},
): Promise<NotmidApiAuthContext> {
  const bearerToken = parseBearerToken(authorization);

  if (mode === "fake" && bearerToken === notmidFakeAccessToken) {
    return {
      authenticated: true,
      mode,
      provider: "fake",
      sessionExpiresAt: notmidFakeAuthSession.expiresAt,
      tokenKind: "fake-local",
      user: notmidFakeAuthUser,
    };
  }

  if (mode === "firebase" && bearerToken) {
    const verifiedToken = await options.firebaseTokenVerifier?.verifyIdToken(bearerToken);

    if (verifiedToken) {
      return {
        authenticated: true,
        mode,
        provider: authProviderForFirebaseProvider(verifiedToken.provider),
        sessionExpiresAt: verifiedToken.expiresAt,
        tokenKind: "firebase-id-token",
        user: authUserForFirebaseToken(verifiedToken),
      };
    }

    return {
      authenticated: false,
      mode,
      tokenKind: "firebase-unverified",
    };
  }

  return {
    authenticated: false,
    mode,
  };
}

export function authStatusForContext(context: NotmidApiAuthContext): NotmidAuthStatusResponse {
  if (context.authenticated && context.mode === "fake") {
    return {
      ...notmidFakeAuthStatus,
      mode: context.mode,
      source: "api",
    };
  }

  if (context.authenticated && context.mode === "firebase" && context.user) {
    return {
      source: "api",
      generatedAt: new Date().toISOString(),
      mode: context.mode,
      authenticated: true,
      user: context.user,
      sessionExpiresAt: context.sessionExpiresAt,
      requiredFor: notmidAuthRequiredActions,
    };
  }

  return {
    ...notmidSignedOutAuthStatus,
    mode: context.mode,
    source: "api",
    requiredFor: notmidAuthRequiredActions,
  };
}

export function canUseFakeSignIn(mode: NotmidAuthMode): boolean {
  return mode === "fake";
}

export function fakeSignInResponse(
  request: Partial<NotmidSignInRequest> & { provider: NotmidAuthProvider },
): NotmidSignInResponse {
  return {
    mode: "fake",
    session: {
      ...notmidFakeAuthSession,
      provider: request.provider,
    },
    nextPath: normalizeNextPath(request.returnTo),
  };
}

export function isAuthorizedWrite(context: NotmidApiAuthContext): boolean {
  return context.authenticated;
}

export function parseAuthProvider(provider: unknown): NotmidAuthProvider | null {
  if (provider === "fake" || provider === "anonymous" || provider === "google") {
    return provider;
  }

  return null;
}

export function normalizeNextPath(returnTo: unknown): string {
  if (
    typeof returnTo !== "string" ||
    !returnTo.startsWith("/notmid") ||
    returnTo.startsWith("//")
  ) {
    return notmidRoutes.capture;
  }

  return returnTo;
}

function parseBearerToken(authorization: string | undefined): string | null {
  const match = authorization?.match(/^Bearer\s+(.+)$/i);
  const token = match?.[1]?.trim();

  return token && token.length > 0 ? token : null;
}

function authProviderForFirebaseProvider(provider: string | undefined): NotmidAuthProvider | undefined {
  if (provider === "anonymous") {
    return "anonymous";
  }

  if (provider === "google.com") {
    return "google";
  }

  return undefined;
}

function authUserForFirebaseToken(token: FirebaseVerifiedIdToken): NotmidAuthUser {
  const handle = handleForFirebaseToken(token);

  return {
    id: `firebase:${token.uid}`,
    handle,
    displayName: token.displayName ?? displayNameForEmail(token.email) ?? handle,
    homeNeighborhood: "Notmid",
    avatarImageUrl: token.pictureUrl ?? "",
    roles: ["creator"],
  };
}

function handleForFirebaseToken(token: FirebaseVerifiedIdToken): string {
  const emailPrefix = token.email?.split("@")[0];
  const baseHandle = emailPrefix ?? token.uid;
  const sanitizedHandle = baseHandle
    .toLowerCase()
    .replace(/[^a-z0-9._-]/g, ".")
    .replace(/\.+/g, ".")
    .replace(/^\.+|\.+$/g, "");

  return sanitizedHandle.length > 0 ? sanitizedHandle : `firebase.${token.uid.slice(0, 12)}`;
}

function displayNameForEmail(email: string | undefined): string | undefined {
  return email?.split("@")[0];
}
