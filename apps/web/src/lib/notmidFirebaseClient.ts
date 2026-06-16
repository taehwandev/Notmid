"use client";

export type NotmidFirebaseClientProvider = "anonymous" | "google";

export type NotmidFirebaseClientSession = {
  idToken: string;
  refreshToken: string;
};

export async function signInAnonymouslyWithNotmidFirebase(): Promise<NotmidFirebaseClientSession> {
  const apiKey = readFirebaseWebApiKey();
  const response = await fetch(
    `https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=${encodeURIComponent(apiKey)}`,
    {
      body: JSON.stringify({ returnSecureToken: true }),
      headers: {
        accept: "application/json",
        "content-type": "application/json",
      },
      method: "POST",
    },
  );

  if (!response.ok) {
    throw new Error("Firebase anonymous sign-in failed.");
  }

  const body = (await response.json()) as { idToken?: unknown; refreshToken?: unknown };
  if (typeof body.idToken !== "string" || !body.idToken.trim()) {
    throw new Error("Firebase anonymous sign-in did not return an ID token.");
  }
  if (typeof body.refreshToken !== "string" || !body.refreshToken.trim()) {
    throw new Error("Firebase anonymous sign-in did not return a refresh token.");
  }

  return {
    idToken: body.idToken,
    refreshToken: body.refreshToken,
  };
}

export function readGoogleIdentityClientId(): string | undefined {
  return process.env.NEXT_PUBLIC_NOTMID_GOOGLE_CLIENT_ID?.trim() || undefined;
}

function readFirebaseWebApiKey(): string {
  const apiKey = process.env.NEXT_PUBLIC_NOTMID_FIREBASE_API_KEY?.trim();

  if (!apiKey) {
    throw new Error("notmid Firebase web config is missing: NEXT_PUBLIC_NOTMID_FIREBASE_API_KEY");
  }

  return apiKey;
}
