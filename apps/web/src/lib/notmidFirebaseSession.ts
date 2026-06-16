import type { NextResponse } from "next/server";
import {
  notmidAuthCookieName,
  notmidAuthCookiePath,
  notmidAuthRefreshCookieName,
} from "./notmidAuthCookies";

export const firebaseIdTokenCookieMaxAgeSeconds = 50 * 60;
export const firebaseRefreshTokenCookieMaxAgeSeconds = 60 * 60 * 24 * 30;

export type NotmidFirebaseServerSession = {
  idToken: string;
  refreshToken: string;
  idTokenMaxAgeSeconds?: number;
};

export async function refreshNotmidFirebaseIdToken(
  apiKey: string,
  refreshToken: string,
  fetcher: typeof fetch = fetch,
): Promise<NotmidFirebaseServerSession> {
  const response = await fetcher(
    `https://securetoken.googleapis.com/v1/token?key=${encodeURIComponent(apiKey)}`,
    {
      body: new URLSearchParams({
        grant_type: "refresh_token",
        refresh_token: refreshToken,
      }),
      headers: {
        accept: "application/json",
        "content-type": "application/x-www-form-urlencoded",
      },
      method: "POST",
    },
  );

  if (!response.ok) {
    throw new Error("Firebase token refresh failed.");
  }

  const body = (await response.json()) as {
    expires_in?: unknown;
    id_token?: unknown;
    refresh_token?: unknown;
  };
  const idToken = typeof body.id_token === "string" ? body.id_token.trim() : "";
  const nextRefreshToken =
    typeof body.refresh_token === "string" && body.refresh_token.trim()
      ? body.refresh_token
      : refreshToken;

  if (!idToken || !looksLikeJwt(idToken)) {
    throw new Error("Firebase token refresh did not return an ID token.");
  }

  return {
    idToken,
    refreshToken: nextRefreshToken,
    idTokenMaxAgeSeconds: resolveIdTokenCookieMaxAgeSeconds(body.expires_in),
  };
}

export async function exchangeGoogleIdTokenForNotmidFirebaseSession(
  apiKey: string,
  googleIdToken: string,
  requestUri: string,
  existingFirebaseIdToken?: string,
  fetcher: typeof fetch = fetch,
): Promise<NotmidFirebaseServerSession> {
  if (!looksLikeJwt(googleIdToken)) {
    throw new Error("A Google ID token is required.");
  }

  const response = await fetcher(
    `https://identitytoolkit.googleapis.com/v1/accounts:signInWithIdp?key=${encodeURIComponent(
      apiKey,
    )}`,
    {
      body: JSON.stringify({
        ...(existingFirebaseIdToken && looksLikeJwt(existingFirebaseIdToken)
          ? { idToken: existingFirebaseIdToken }
          : {}),
        postBody: new URLSearchParams({
          id_token: googleIdToken,
          providerId: "google.com",
        }).toString(),
        requestUri,
        returnIdpCredential: false,
        returnSecureToken: true,
      }),
      headers: {
        accept: "application/json",
        "content-type": "application/json",
      },
      method: "POST",
    },
  );

  if (!response.ok) {
    throw new Error("Firebase Google sign-in failed.");
  }

  const body = (await response.json()) as {
    expiresIn?: unknown;
    idToken?: unknown;
    refreshToken?: unknown;
  };
  const idToken = typeof body.idToken === "string" ? body.idToken.trim() : "";
  const refreshToken = typeof body.refreshToken === "string" ? body.refreshToken.trim() : "";

  if (!idToken || !looksLikeJwt(idToken)) {
    throw new Error("Firebase Google sign-in did not return an ID token.");
  }
  if (!looksLikeRefreshToken(refreshToken)) {
    throw new Error("Firebase Google sign-in did not return a refresh token.");
  }

  return {
    idToken,
    refreshToken,
    idTokenMaxAgeSeconds: resolveIdTokenCookieMaxAgeSeconds(body.expiresIn),
  };
}

export function setNotmidFirebaseSessionCookies(
  response: NextResponse,
  session: NotmidFirebaseServerSession,
) {
  response.cookies.set(notmidAuthCookieName, session.idToken, {
    httpOnly: true,
    maxAge: session.idTokenMaxAgeSeconds ?? firebaseIdTokenCookieMaxAgeSeconds,
    path: notmidAuthCookiePath,
    sameSite: "lax",
    secure: process.env.NODE_ENV === "production",
  });
  response.cookies.set(notmidAuthRefreshCookieName, session.refreshToken, {
    httpOnly: true,
    maxAge: firebaseRefreshTokenCookieMaxAgeSeconds,
    path: notmidAuthCookiePath,
    sameSite: "lax",
    secure: process.env.NODE_ENV === "production",
  });
}

export function clearNotmidFirebaseSessionCookies(response: NextResponse) {
  response.cookies.set(notmidAuthCookieName, "", {
    httpOnly: true,
    maxAge: 0,
    path: notmidAuthCookiePath,
    sameSite: "lax",
    secure: process.env.NODE_ENV === "production",
  });
  response.cookies.set(notmidAuthRefreshCookieName, "", {
    httpOnly: true,
    maxAge: 0,
    path: notmidAuthCookiePath,
    sameSite: "lax",
    secure: process.env.NODE_ENV === "production",
  });
}

export function looksLikeJwt(value: string): boolean {
  return value.split(".").length === 3;
}

export function looksLikeRefreshToken(value: string): boolean {
  return value.length >= 20 && !/\s/.test(value);
}

function resolveIdTokenCookieMaxAgeSeconds(value: unknown): number {
  const expiresInSeconds =
    typeof value === "string" ? Number.parseInt(value, 10) : Number.NaN;

  if (!Number.isFinite(expiresInSeconds) || expiresInSeconds <= 120) {
    return firebaseIdTokenCookieMaxAgeSeconds;
  }

  return Math.min(firebaseIdTokenCookieMaxAgeSeconds, expiresInSeconds - 60);
}
