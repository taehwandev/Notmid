import {
  notmidFakeAuthSession,
  notmidFakeAuthStatus,
  notmidSignedOutAuthStatus,
  type NotmidAuthStatusResponse,
} from "@notmid/contracts";
import { cookies } from "next/headers";
import {
  notmidAuthCookieName,
  notmidAuthCookiePath,
  notmidLegacyFakeAuthCookieName,
} from "./notmidAuthCookies";
import { createNotmidWebApiClient, isNotmidLocalFallbackEnabled } from "./notmidRuntime";

export {
  notmidAuthCookieName,
  notmidAuthCookiePath,
  notmidAuthRefreshCookieName,
} from "./notmidAuthCookies";

export async function getNotmidAuthStatus(): Promise<NotmidAuthStatusResponse> {
  const accessToken = await getNotmidAccessToken();
  const api = createNotmidWebApiClient();

  return api
    .getAuthStatus(accessToken)
    .catch(() =>
      isNotmidLocalFallbackEnabled() && accessToken === notmidFakeAuthSession.accessToken
        ? notmidFakeAuthStatus
        : notmidSignedOutAuthStatus,
    );
}

export async function getNotmidAccessToken(): Promise<string | undefined> {
  const cookieStore = await cookies();
  return (
    cookieStore.get(notmidAuthCookieName)?.value ??
    cookieStore.get(notmidLegacyFakeAuthCookieName)?.value
  );
}

export function normalizeNotmidReturnTo(value: unknown): string | undefined {
  if (typeof value !== "string" || !value.startsWith("/notmid") || value.startsWith("//")) {
    return undefined;
  }

  return value;
}
