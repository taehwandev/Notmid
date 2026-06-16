"use client";

import Script from "next/script";
import { useCallback, useEffect, useRef, useState } from "react";
import {
  readGoogleIdentityClientId,
  signInAnonymouslyWithNotmidFirebase,
  type NotmidFirebaseClientProvider,
} from "../../../lib/notmidFirebaseClient";

type FirebaseLoginActionsProps = {
  returnTo: string;
};

type PendingProvider = NotmidFirebaseClientProvider | undefined;
type GoogleCredentialResponse = {
  credential?: string;
};

declare global {
  interface Window {
    google?: {
      accounts?: {
        id?: {
          initialize: (config: {
            auto_select?: boolean;
            callback: (response: GoogleCredentialResponse) => void;
            client_id: string;
            ux_mode?: "popup" | "redirect";
          }) => void;
          renderButton: (
            parent: HTMLElement,
            options: {
              logo_alignment?: "left" | "center";
              shape?: "rectangular" | "pill" | "circle" | "square";
              size?: "large" | "medium" | "small";
              text?: "signin_with" | "signup_with" | "continue_with" | "signin";
              theme?: "outline" | "filled_blue" | "filled_black";
              width?: number;
            },
          ) => void;
        };
      };
    };
  }
}

export function FirebaseLoginActions({ returnTo }: FirebaseLoginActionsProps) {
  const [pendingProvider, setPendingProvider] = useState<PendingProvider>();
  const [errorMessage, setErrorMessage] = useState<string>();
  const [googleScriptReady, setGoogleScriptReady] = useState(false);
  const googleButtonRef = useRef<HTMLDivElement>(null);
  const googleClientId = readGoogleIdentityClientId();

  const openVerifiedFirebaseSession = useCallback(
    async (
      endpoint: "/notmid/login/firebase-session" | "/notmid/login/firebase-session/google",
      body: Record<string, string>,
    ) => {
      const response = await fetch(endpoint, {
        body: JSON.stringify({ ...body, returnTo }),
        headers: {
          accept: "application/json",
          "content-type": "application/json",
        },
        method: "POST",
      });

      if (!response.ok) {
        throw new Error("notmid could not open a verified session.");
      }

      const webSession = (await response.json()) as { nextPath?: string };
      window.location.assign(webSession.nextPath ?? returnTo);
    },
    [returnTo],
  );

  const handleGoogleCredential = useCallback(
    (response: GoogleCredentialResponse) => {
      const googleIdToken = response.credential?.trim() ?? "";
      if (!googleIdToken) {
        setErrorMessage("Google did not return a sign-in token. Try again.");
        setPendingProvider(undefined);
        return;
      }

      setPendingProvider("google");
      setErrorMessage(undefined);
      void openVerifiedFirebaseSession("/notmid/login/firebase-session/google", {
        googleIdToken,
      }).catch(() => {
        setErrorMessage("Google sign-in failed. Try again from this browser window.");
        setPendingProvider(undefined);
      });
    },
    [openVerifiedFirebaseSession],
  );

  useEffect(() => {
    const googleIdentity = window.google?.accounts?.id;
    const buttonElement = googleButtonRef.current;

    if (!googleClientId || !googleScriptReady || !googleIdentity || !buttonElement) {
      return;
    }

    googleIdentity.initialize({
      auto_select: false,
      callback: handleGoogleCredential,
      client_id: googleClientId,
      ux_mode: "popup",
    });

    buttonElement.replaceChildren();
    googleIdentity.renderButton(buttonElement, {
      logo_alignment: "left",
      shape: "rectangular",
      size: "large",
      text: "continue_with",
      theme: "outline",
      width: buttonElement.clientWidth || 320,
    });
  }, [googleClientId, googleScriptReady, handleGoogleCredential]);

  async function continueAnonymouslyWithFirebase() {
    setPendingProvider("anonymous");
    setErrorMessage(undefined);

    try {
      const firebaseSession = await signInAnonymouslyWithNotmidFirebase();
      await openVerifiedFirebaseSession("/notmid/login/firebase-session", firebaseSession);
    } catch {
      setErrorMessage("Sign-in failed. Try again from this browser window.");
      setPendingProvider(undefined);
    }
  }

  return (
    <div className="login-web-social-stack">
      {googleClientId ? (
        <>
          <Script
            src="https://accounts.google.com/gsi/client"
            strategy="afterInteractive"
            onLoad={() => setGoogleScriptReady(true)}
            onError={() => setErrorMessage("Google sign-in could not load. Try again later.")}
          />
          <div
            className="login-web-google-button"
            data-pending={pendingProvider === "google" ? "true" : undefined}
            ref={googleButtonRef}
          />
        </>
      ) : null}
      <button
        className="login-web-social-button"
        disabled={pendingProvider !== undefined}
        onClick={() => void continueAnonymouslyWithFirebase()}
        type="button"
      >
        <span>A</span>
        {pendingProvider === "anonymous" ? "Creating session..." : "Continue as guest"}
      </button>
      {errorMessage ? (
        <p className="login-web-auth-error" role="alert">
          {errorMessage}
        </p>
      ) : null}
    </div>
  );
}
