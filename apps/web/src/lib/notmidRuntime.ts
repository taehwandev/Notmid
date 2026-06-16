import { createNotmidApiClient } from "@notmid/api-client";

export function createNotmidWebApiClient() {
  return createNotmidApiClient({
    baseUrl: readNotmidWebRuntimeConfig().apiBaseUrl,
    fetcher: noStoreFetch,
  });
}

export function readNotmidWebRuntimeConfig(): NotmidWebRuntimeConfig {
  const config = createNotmidWebRuntimeConfig(process.env);

  if (config.errors.length > 0) {
    throw new Error(config.errors.map((error) => `notmid web config error: ${error}`).join("\n"));
  }

  return config.value;
}

export function isNotmidLocalFallbackEnabled(): boolean {
  const config = createNotmidWebRuntimeConfig(process.env);
  return (
    config.errors.length === 0 &&
    config.value.nodeEnv !== "production" &&
    config.value.authProvider === "fake"
  );
}

export function readNotmidWebAuthProviderForRender(): NotmidWebAuthProvider {
  const config = createNotmidWebRuntimeConfig(process.env);
  return config.errors.length === 0 ? config.value.authProvider : "disabled";
}

export const noStoreFetch: typeof fetch = (input, init) =>
  fetch(input, {
    ...init,
    cache: "no-store",
  });

function createNotmidWebRuntimeConfig(env: NodeJS.ProcessEnv): RuntimeConfigResult {
  const nodeEnv = env.NODE_ENV ?? "development";
  const production = nodeEnv === "production";
  const apiBaseUrl = env.NOTMID_API_BASE_URL ?? "http://localhost:8787";
  const authProvider = resolveWebAuthProvider(env.NEXT_PUBLIC_NOTMID_AUTH_PROVIDER, production);
  const firebaseWebConfig = readFirebaseWebConfig(env);
  const errors: string[] = [];

  if (production) {
    if (!env.NOTMID_API_BASE_URL?.trim()) {
      errors.push("NOTMID_API_BASE_URL is required when NODE_ENV=production.");
    }

    if (!isHttpsOrigin(apiBaseUrl)) {
      errors.push("NOTMID_API_BASE_URL must use https when NODE_ENV=production.");
    }

    if (isLocalOrigin(apiBaseUrl)) {
      errors.push("NOTMID_API_BASE_URL must not point at localhost in production.");
    }

    if (!env.NEXT_PUBLIC_NOTMID_AUTH_PROVIDER?.trim()) {
      errors.push("NEXT_PUBLIC_NOTMID_AUTH_PROVIDER is required when NODE_ENV=production.");
    }
  }

  if (!authProvider) {
    errors.push("NEXT_PUBLIC_NOTMID_AUTH_PROVIDER must be fake, firebase, or disabled.");
  } else if (production && authProvider === "fake") {
    errors.push("NEXT_PUBLIC_NOTMID_AUTH_PROVIDER=fake is local-only and cannot run in production.");
  }

  if (authProvider === "firebase") {
    errors.push(...firebaseWebConfig.errors);
  }

  const leakedClientSecretKeys = findLeakedClientSecretKeys(env);
  if (leakedClientSecretKeys.length > 0) {
    errors.push(
      `Client-visible Firebase config must not include private values: ${leakedClientSecretKeys.join(
        ", ",
      )}.`,
    );
  }

  return {
    errors,
    value: {
      apiBaseUrl,
      authProvider: authProvider ?? "disabled",
      firebaseWebConfig: firebaseWebConfig.value,
      nodeEnv,
    },
  };
}

function isHttpsOrigin(origin: string): boolean {
  try {
    return new URL(origin).protocol === "https:";
  } catch {
    return false;
  }
}

function isLocalOrigin(origin: string): boolean {
  try {
    const hostname = new URL(origin).hostname;
    return hostname === "localhost" || hostname === "127.0.0.1" || hostname === "0.0.0.0";
  } catch {
    return false;
  }
}

function resolveWebAuthProvider(
  value: string | undefined,
  production: boolean,
): NotmidWebAuthProvider | null {
  const provider = value?.trim();

  if (!provider) {
    return production ? null : "fake";
  }

  if (provider === "fake" || provider === "firebase" || provider === "disabled") {
    return provider;
  }

  return null;
}

function readFirebaseWebConfig(env: NodeJS.ProcessEnv): FirebaseWebConfigResult {
  const value: NotmidFirebaseWebConfig = {
    apiKey: env.NEXT_PUBLIC_NOTMID_FIREBASE_API_KEY?.trim() || undefined,
    appId: env.NEXT_PUBLIC_NOTMID_FIREBASE_APP_ID?.trim() || undefined,
    authDomain: env.NEXT_PUBLIC_NOTMID_FIREBASE_AUTH_DOMAIN?.trim() || undefined,
    googleClientId: env.NEXT_PUBLIC_NOTMID_GOOGLE_CLIENT_ID?.trim() || undefined,
    messagingSenderId: env.NEXT_PUBLIC_NOTMID_FIREBASE_MESSAGING_SENDER_ID?.trim() || undefined,
    projectId: env.NEXT_PUBLIC_NOTMID_FIREBASE_PROJECT_ID?.trim() || undefined,
    storageBucket: env.NEXT_PUBLIC_NOTMID_FIREBASE_STORAGE_BUCKET?.trim() || undefined,
  };
  const errors = [
    ["NEXT_PUBLIC_NOTMID_FIREBASE_API_KEY", value.apiKey],
    ["NEXT_PUBLIC_NOTMID_FIREBASE_AUTH_DOMAIN", value.authDomain],
    ["NEXT_PUBLIC_NOTMID_FIREBASE_PROJECT_ID", value.projectId],
    ["NEXT_PUBLIC_NOTMID_FIREBASE_APP_ID", value.appId],
    ["NEXT_PUBLIC_NOTMID_GOOGLE_CLIENT_ID", value.googleClientId],
  ]
    .filter(([, configValue]) => !configValue)
    .map(([key]) => `${key} is required when NEXT_PUBLIC_NOTMID_AUTH_PROVIDER=firebase.`);

  return {
    errors,
    value,
  };
}

function findLeakedClientSecretKeys(env: NodeJS.ProcessEnv): string[] {
  const forbiddenKeys = [
    "NEXT_PUBLIC_NOTMID_FIREBASE_PRIVATE_KEY",
    "NEXT_PUBLIC_NOTMID_FIREBASE_CLIENT_EMAIL",
    "NEXT_PUBLIC_NOTMID_FIREBASE_SERVICE_ACCOUNT",
    "NEXT_PUBLIC_NOTMID_GOOGLE_CLIENT_SECRET",
    "NEXT_PUBLIC_FIREBASE_PRIVATE_KEY",
    "NEXT_PUBLIC_FIREBASE_CLIENT_EMAIL",
    "NEXT_PUBLIC_FIREBASE_SERVICE_ACCOUNT",
    "NEXT_PUBLIC_GOOGLE_CLIENT_SECRET",
  ];

  return forbiddenKeys.filter((key) => Boolean(env[key]?.trim()));
}

export type NotmidWebAuthProvider = "fake" | "firebase" | "disabled";

type NotmidWebRuntimeConfig = {
  apiBaseUrl: string;
  authProvider: NotmidWebAuthProvider;
  firebaseWebConfig: NotmidFirebaseWebConfig;
  nodeEnv: string;
};

type RuntimeConfigResult = {
  errors: string[];
  value: NotmidWebRuntimeConfig;
};

type NotmidFirebaseWebConfig = {
  apiKey?: string;
  appId?: string;
  authDomain?: string;
  googleClientId?: string;
  messagingSenderId?: string;
  projectId?: string;
  storageBucket?: string;
};

type FirebaseWebConfigResult = {
  errors: string[];
  value: NotmidFirebaseWebConfig;
};

if (process.env.NOTMID_VALIDATE_WEB_CONFIG_ONLY === "true") {
  const config = createNotmidWebRuntimeConfig(process.env);

  if (config.errors.length > 0) {
    for (const error of config.errors) {
      console.error(`notmid web config error: ${error}`);
    }
    process.exit(1);
  }

  console.log(
    `notmid web config valid for ${config.value.nodeEnv} with ${config.value.authProvider} auth`,
  );
  process.exit(0);
}
