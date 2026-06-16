import { isNotmidApiRequestError } from "@notmid/api-client";
import { notmidRoutes } from "@notmid/contracts";
import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import {
  getNotmidAccessToken,
  notmidAuthCookieName,
  notmidAuthCookiePath,
  notmidAuthRefreshCookieName,
} from "./notmidAuth";
import {
  firebaseIdTokenCookieMaxAgeSeconds,
  firebaseRefreshTokenCookieMaxAgeSeconds,
  looksLikeRefreshToken,
  refreshNotmidFirebaseIdToken,
  type NotmidFirebaseServerSession,
} from "./notmidFirebaseSession";
import { createNotmidWebApiClient, readNotmidWebRuntimeConfig } from "./notmidRuntime";

export async function runNotmidAuthenticatedApiAction<T>(
  returnTo: string,
  action: (accessToken: string) => Promise<T>,
): Promise<T> {
  const accessToken = await getNotmidAccessToken();

  if (!accessToken) {
    redirect(notmidRoutes.login(returnTo));
  }

  try {
    return await action(accessToken);
  } catch (error) {
    if (!isNotmidApiRequestError(error, 401)) {
      throw error;
    }
  }

  const refreshedAccessToken = await refreshNotmidActionSession();

  if (!refreshedAccessToken) {
    redirect(notmidRoutes.login(returnTo));
  }

  return action(refreshedAccessToken);
}

async function refreshNotmidActionSession(): Promise<string | undefined> {
  const runtimeConfig = readNotmidWebRuntimeConfig();

  if (runtimeConfig.authProvider !== "firebase") {
    return undefined;
  }

  const apiKey = runtimeConfig.firebaseWebConfig.apiKey;
  const cookieStore = await cookies();
  const refreshToken = cookieStore.get(notmidAuthRefreshCookieName)?.value.trim() ?? "";

  if (!apiKey || !looksLikeRefreshToken(refreshToken)) {
    return undefined;
  }

  const refreshedSession = await refreshNotmidFirebaseIdToken(apiKey, refreshToken).catch(
    () => undefined,
  );

  if (!refreshedSession) {
    clearNotmidActionSessionCookies(cookieStore);
    return undefined;
  }

  const authStatus = await createNotmidWebApiClient()
    .getAuthStatus(refreshedSession.idToken)
    .catch(() => undefined);

  if (!authStatus?.authenticated) {
    clearNotmidActionSessionCookies(cookieStore);
    return undefined;
  }

  setNotmidActionSessionCookies(cookieStore, refreshedSession);
  return refreshedSession.idToken;
}

function setNotmidActionSessionCookies(
  cookieStore: Awaited<ReturnType<typeof cookies>>,
  session: NotmidFirebaseServerSession,
) {
  cookieStore.set(notmidAuthCookieName, session.idToken, {
    httpOnly: true,
    maxAge: session.idTokenMaxAgeSeconds ?? firebaseIdTokenCookieMaxAgeSeconds,
    path: notmidAuthCookiePath,
    sameSite: "lax",
    secure: process.env.NODE_ENV === "production",
  });
  cookieStore.set(notmidAuthRefreshCookieName, session.refreshToken, {
    httpOnly: true,
    maxAge: firebaseRefreshTokenCookieMaxAgeSeconds,
    path: notmidAuthCookiePath,
    sameSite: "lax",
    secure: process.env.NODE_ENV === "production",
  });
}

function clearNotmidActionSessionCookies(cookieStore: Awaited<ReturnType<typeof cookies>>) {
  for (const cookieName of [notmidAuthCookieName, notmidAuthRefreshCookieName]) {
    cookieStore.set(cookieName, "", {
      httpOnly: true,
      maxAge: 0,
      path: notmidAuthCookiePath,
      sameSite: "lax",
      secure: process.env.NODE_ENV === "production",
    });
  }
}
