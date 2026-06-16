import { notmidRoutes } from "@notmid/contracts";
import { NextResponse } from "next/server";
import { normalizeNotmidReturnTo } from "../../../../lib/notmidAuth";
import {
  clearNotmidFirebaseSessionCookies,
  looksLikeJwt,
  looksLikeRefreshToken,
  setNotmidFirebaseSessionCookies,
} from "../../../../lib/notmidFirebaseSession";
import { createNotmidWebApiClient, readNotmidWebRuntimeConfig } from "../../../../lib/notmidRuntime";

export async function POST(request: Request) {
  const runtimeConfig = readNotmidWebRuntimeConfig();

  if (runtimeConfig.authProvider !== "firebase") {
    return NextResponse.json(
      { error: { code: "firebase_auth_disabled", message: "Firebase sign-in is not enabled." } },
      { status: 409 },
    );
  }

  const body = await readFirebaseSessionRequest(request);
  const idToken = body?.idToken.trim() ?? "";
  const refreshToken = body?.refreshToken.trim() ?? "";

  if (!looksLikeJwt(idToken)) {
    return NextResponse.json(
      { error: { code: "invalid_id_token", message: "A Firebase ID token is required." } },
      { status: 400 },
    );
  }
  if (!looksLikeRefreshToken(refreshToken)) {
    return NextResponse.json(
      { error: { code: "invalid_refresh_token", message: "A Firebase refresh token is required." } },
      { status: 400 },
    );
  }

  const returnTo = normalizeNotmidReturnTo(body?.returnTo) ?? notmidRoutes.capture;
  const authStatus = await createNotmidWebApiClient()
    .getAuthStatus(idToken)
    .catch(() => undefined);

  if (!authStatus?.authenticated) {
    return NextResponse.json(
      {
        error: {
          code: "unverified_id_token",
          message: "The API did not accept this Firebase session.",
        },
      },
      { status: 401 },
    );
  }

  const response = NextResponse.json({ nextPath: returnTo });
  setNotmidFirebaseSessionCookies(response, { idToken, refreshToken });

  return response;
}

export async function DELETE() {
  const response = NextResponse.json({ signedOut: true });
  clearNotmidFirebaseSessionCookies(response);

  return response;
}

async function readFirebaseSessionRequest(
  request: Request,
): Promise<{ idToken: string; refreshToken: string; returnTo?: string } | undefined> {
  const body = await request.json().catch(() => undefined);

  if (!body || typeof body !== "object") {
    return undefined;
  }

  const idToken = "idToken" in body && typeof body.idToken === "string" ? body.idToken : "";
  const refreshToken =
    "refreshToken" in body && typeof body.refreshToken === "string" ? body.refreshToken : "";
  const returnTo = "returnTo" in body && typeof body.returnTo === "string" ? body.returnTo : undefined;

  return { idToken, refreshToken, returnTo };
}
