import { NextRequest, NextResponse } from "next/server";
import { notmidAuthRefreshCookieName } from "../../../../../lib/notmidAuth";
import {
  clearNotmidFirebaseSessionCookies,
  looksLikeRefreshToken,
  refreshNotmidFirebaseIdToken,
  setNotmidFirebaseSessionCookies,
} from "../../../../../lib/notmidFirebaseSession";
import { createNotmidWebApiClient, readNotmidWebRuntimeConfig } from "../../../../../lib/notmidRuntime";

export async function POST(request: NextRequest) {
  const runtimeConfig = readNotmidWebRuntimeConfig();

  if (runtimeConfig.authProvider !== "firebase") {
    return NextResponse.json(
      { error: { code: "firebase_auth_disabled", message: "Firebase sign-in is not enabled." } },
      { status: 409 },
    );
  }

  const apiKey = runtimeConfig.firebaseWebConfig.apiKey;
  const refreshToken = request.cookies.get(notmidAuthRefreshCookieName)?.value.trim() ?? "";

  if (!apiKey || !looksLikeRefreshToken(refreshToken)) {
    return clearSessionResponse("refresh_required", "A Firebase refresh session is required.");
  }

  const refreshedSession = await refreshNotmidFirebaseIdToken(apiKey, refreshToken).catch(
    () => undefined,
  );

  if (!refreshedSession) {
    return clearSessionResponse("refresh_failed", "The Firebase refresh session is no longer valid.");
  }

  const authStatus = await createNotmidWebApiClient()
    .getAuthStatus(refreshedSession.idToken)
    .catch(() => undefined);

  if (!authStatus?.authenticated) {
    return clearSessionResponse("unverified_id_token", "The API did not accept this Firebase session.");
  }

  const response = NextResponse.json({
    authenticated: true,
    sessionExpiresAt: authStatus.sessionExpiresAt,
  });
  setNotmidFirebaseSessionCookies(response, refreshedSession);

  return response;
}

function clearSessionResponse(code: string, message: string) {
  const response = NextResponse.json({ error: { code, message } }, { status: 401 });
  clearNotmidFirebaseSessionCookies(response);
  return response;
}
