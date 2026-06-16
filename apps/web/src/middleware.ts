import { NextRequest, NextResponse } from "next/server";
import { notmidAuthCookieName, notmidAuthRefreshCookieName } from "./lib/notmidAuthCookies";
import {
  clearNotmidFirebaseSessionCookies,
  looksLikeJwt,
  looksLikeRefreshToken,
  refreshNotmidFirebaseIdToken,
  setNotmidFirebaseSessionCookies,
} from "./lib/notmidFirebaseSession";

const firebaseIdTokenRefreshLeewaySeconds = 5 * 60;
const firebaseSessionRoutePrefix = "/notmid/login/firebase-session";

export async function middleware(request: NextRequest) {
  if (request.nextUrl.pathname.startsWith(firebaseSessionRoutePrefix)) {
    return NextResponse.next();
  }

  const config = readMiddlewareFirebaseRefreshConfig();

  if (!config) {
    return NextResponse.next();
  }

  const refreshToken = request.cookies.get(notmidAuthRefreshCookieName)?.value.trim() ?? "";

  if (!looksLikeRefreshToken(refreshToken)) {
    return NextResponse.next();
  }

  const idToken = request.cookies.get(notmidAuthCookieName)?.value.trim();

  if (!shouldRefreshFirebaseIdToken(idToken)) {
    return NextResponse.next();
  }

  const refreshedSession = await refreshNotmidFirebaseIdToken(config.apiKey, refreshToken).catch(
    () => undefined,
  );

  if (!refreshedSession) {
    return clearForwardedFirebaseSession(request);
  }

  const verified = await verifyRefreshedNotmidSession(config.apiBaseUrl, refreshedSession.idToken);

  if (!verified) {
    return clearForwardedFirebaseSession(request);
  }

  const requestHeaders = new Headers(request.headers);
  requestHeaders.set("cookie", requestCookieHeaderWithAccessToken(request, refreshedSession.idToken));

  const response = NextResponse.next({
    request: {
      headers: requestHeaders,
    },
  });
  setNotmidFirebaseSessionCookies(response, refreshedSession);

  return response;
}

export const config = {
  matcher: ["/notmid/:path*"],
};

function readMiddlewareFirebaseRefreshConfig():
  | {
      apiBaseUrl: string;
      apiKey: string;
    }
  | undefined {
  if (process.env.NEXT_PUBLIC_NOTMID_AUTH_PROVIDER?.trim() !== "firebase") {
    return undefined;
  }

  const apiKey = process.env.NEXT_PUBLIC_NOTMID_FIREBASE_API_KEY?.trim();

  if (!apiKey) {
    return undefined;
  }

  return {
    apiBaseUrl: process.env.NOTMID_API_BASE_URL?.trim() || "http://localhost:8787",
    apiKey,
  };
}

function shouldRefreshFirebaseIdToken(idToken: string | undefined): boolean {
  if (!idToken || !looksLikeJwt(idToken)) {
    return true;
  }

  const expiresAtSeconds = readJwtExpiresAtSeconds(idToken);

  return (
    !expiresAtSeconds ||
    expiresAtSeconds <= Math.floor(Date.now() / 1000) + firebaseIdTokenRefreshLeewaySeconds
  );
}

function readJwtExpiresAtSeconds(idToken: string): number | null {
  const encodedPayload = idToken.split(".")[1];

  if (!encodedPayload) {
    return null;
  }

  try {
    const payload = JSON.parse(base64UrlDecode(encodedPayload)) as { exp?: unknown };
    return typeof payload.exp === "number" && Number.isFinite(payload.exp) ? payload.exp : null;
  } catch {
    return null;
  }
}

function base64UrlDecode(value: string): string {
  const normalized = value.replace(/-/g, "+").replace(/_/g, "/");
  const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, "=");

  return globalThis.atob(padded);
}

async function verifyRefreshedNotmidSession(
  apiBaseUrl: string,
  idToken: string,
): Promise<boolean> {
  const response = await fetch(`${normalizeBaseUrl(apiBaseUrl)}/v1/auth/status`, {
    cache: "no-store",
    headers: {
      accept: "application/json",
      authorization: `Bearer ${idToken}`,
    },
  }).catch(() => undefined);

  if (!response?.ok) {
    return false;
  }

  const body = (await response.json().catch(() => undefined)) as
    | { authenticated?: unknown }
    | undefined;

  return body?.authenticated === true;
}

function clearForwardedFirebaseSession(request: NextRequest) {
  const requestHeaders = new Headers(request.headers);
  const cookieHeader = requestCookieHeaderWithoutAuthCookies(request);

  if (cookieHeader) {
    requestHeaders.set("cookie", cookieHeader);
  } else {
    requestHeaders.delete("cookie");
  }

  const response = NextResponse.next({
    request: {
      headers: requestHeaders,
    },
  });
  clearNotmidFirebaseSessionCookies(response);

  return response;
}

function requestCookieHeaderWithAccessToken(request: NextRequest, idToken: string): string {
  return [
    `${notmidAuthCookieName}=${idToken}`,
    ...requestCookiePairsWithout(request, notmidAuthCookieName),
  ].join("; ");
}

function requestCookieHeaderWithoutAuthCookies(request: NextRequest): string {
  return requestCookiePairsWithout(
    request,
    notmidAuthCookieName,
    notmidAuthRefreshCookieName,
  ).join("; ");
}

function requestCookiePairsWithout(request: NextRequest, ...names: string[]): string[] {
  const excludedNames = new Set(names);

  return (request.headers.get("cookie") ?? "")
    .split(";")
    .map((pair) => pair.trim())
    .filter((pair) => {
      const separatorIndex = pair.indexOf("=");

      if (separatorIndex <= 0) {
        return false;
      }

      return !excludedNames.has(pair.slice(0, separatorIndex));
    });
}

function normalizeBaseUrl(baseUrl: string): string {
  return baseUrl.endsWith("/") ? baseUrl.slice(0, -1) : baseUrl;
}
