"use server";

import { isNotmidApiRequestError } from "@notmid/api-client";
import { notmidRoutes } from "@notmid/contracts";
import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { normalizeNotmidReturnTo } from "./notmidAuth";
import { createNotmidWebApiClient } from "./notmidRuntime";
import { runNotmidAuthenticatedApiAction } from "./notmidServerActions";

export async function saveClipFromWeb(formData: FormData) {
  const clipId = readFormString(formData, "clipId");

  if (!clipId) {
    redirect(notmidRoutes.feed);
  }

  const returnTo = normalizeNotmidReturnTo(formData.get("returnTo")) ?? notmidRoutes.clip(clipId);

  try {
    await runNotmidAuthenticatedApiAction(returnTo, (accessToken) =>
      createNotmidWebApiClient().saveClip(clipId, accessToken),
    );
  } catch (error) {
    if (isNotmidApiRequestError(error, 404)) {
      redirect(appendQuery(returnTo, "save", "missing"));
    }

    throw error;
  }

  revalidatePath(notmidRoutes.home);
  revalidatePath(notmidRoutes.feed);
  revalidatePath(notmidRoutes.clip(clipId));
  revalidatePath(returnTo);
  redirect(appendQuery(returnTo, "saved", "1"));
}

function readFormString(formData: FormData, key: string): string {
  const value = formData.get(key);
  return typeof value === "string" ? value.trim() : "";
}

function appendQuery(path: string, key: string, value: string): string {
  const separator = path.includes("?") ? "&" : "?";
  return `${path}${separator}${key}=${encodeURIComponent(value)}`;
}
