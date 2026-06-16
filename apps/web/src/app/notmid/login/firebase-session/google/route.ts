import { notmidRoutes } from "@notmid/contracts";
import { NextRequest, NextResponse } from "next/server";
import { normalizeNotmidReturnTo, notmidAuthCookieName } from "../../../../../lib/notmidAuth";
import {
  clearNotmidFirebaseSessionCookies,
  exchangeGoogleIdTokenForNotmidFirebaseSession,
  looksLikeJwt,
  setNotmidFirebaseSessionCookies,
} from "../../../../../lib/notmidFirebaseSession";
import {
  createNotmidWebApiClient,
  readNotmidWebRuntimeConfig,
} from "../../../../../lib/notmidRuntime";

export async function POST(request: NextRequest) {
  const runtimeConfig = readNotmidWebRuntimeConfig();

  if (runtimeConfig.authProvider !== "firebase") {
    return NextResponse.json(
      { error: { code: "firebase_auth_disabled", message: "Firebase sign-in is not enabled." } },
      { status: 409 },
    );
  }

  const apiKey = runtimeConfig.firebaseWebConfig.apiKey;
  const body = await readGoogleFirebaseSessionRequest(request);
  const googleIdToken = body?.googleIdToken.trim() ?? "";

  if (!apiKey) {
    return NextResponse.json(
      { error: { code: "firebase_config_missing", message: "Firebase web config is missing." } },
      { status: 500 },
    );
  }
  if (!looksLikeJwt(googleIdToken)) {
    return NextResponse.json(
      { error: { code: "invalid_google_id_token", message: "A Google ID token is required." } },
      { status: 400 },
    );
  }

  const returnTo = normalizeNotmidReturnTo(body?.returnTo) ?? notmidRoutes.capture;
  const existingFirebaseIdToken = request.cookies.get(notmidAuthCookieName)?.value.trim();
  const requestUri = new URL(request.url).origin;
  const firebaseSession =
    existingFirebaseIdToken && looksLikeJwt(existingFirebaseIdToken)
      ? await exchangeGoogleIdTokenForNotmidFirebaseSession(
          apiKey,
          googleIdToken,
          requestUri,
          existingFirebaseIdToken,
        ).catch(() =>
          exchangeGoogleIdTokenForNotmidFirebaseSession(apiKey, googleIdToken, requestUri).catch(
            () => undefined,
          ),
        )
      : await exchangeGoogleIdTokenForNotmidFirebaseSession(
          apiKey,
          googleIdToken,
          requestUri,
        ).catch(() => undefined);

  if (!firebaseSession) {
    return NextResponse.json(
      { error: { code: "google_firebase_sign_in_failed", message: "Google sign-in failed." } },
      { status: 401 },
    );
  }

  const authStatus = await createNotmidWebApiClient()
    .getAuthStatus(firebaseSession.idToken)
    .catch(() => undefined);

  if (!authStatus?.authenticated) {
    const response = NextResponse.json(
      {
        error: {
          code: "unverified_id_token",
          message: "The API did not accept this Firebase session.",
        },
      },
      { status: 401 },
    );
    clearNotmidFirebaseSessionCookies(response);

    return response;
  }

  const response = NextResponse.json({ nextPath: returnTo });
  setNotmidFirebaseSessionCookies(response, firebaseSession);

  return response;
}

async function readGoogleFirebaseSessionRequest(
  request: Request,
): Promise<{ googleIdToken: string; returnTo?: string } | undefined> {
  const body = await request.json().catch(() => undefined);

  if (!body || typeof body !== "object") {
    return undefined;
  }

  const googleIdToken =
    "googleIdToken" in body && typeof body.googleIdToken === "string" ? body.googleIdToken : "";
  const returnTo = "returnTo" in body && typeof body.returnTo === "string" ? body.returnTo : undefined;

  return { googleIdToken, returnTo };
}
