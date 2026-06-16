import type { NotmidAuthMode } from "@notmid/contracts";

export type NotmidApiRuntimeConfig = {
  authMode: NotmidAuthMode;
  databaseUrl?: string;
  dataBackend: NotmidDataBackend;
  diagnosticFailureEnabled: boolean;
  firebaseProjectId?: string;
  mutationRateLimit: {
    maxRequests: number;
    windowMs: number;
  };
  nodeEnv: string;
  port: number;
  webOrigin: string;
};

export type RuntimeConfigResult = {
  errors: string[];
  value: NotmidApiRuntimeConfig;
};

export type NotmidDataBackend = "fixture" | "postgres";

export function createRuntimeConfig(env: NodeJS.ProcessEnv): RuntimeConfigResult {
  const nodeEnv = env.NODE_ENV ?? "development";
  const production = nodeEnv === "production";
  const authMode = resolveAuthMode(env.NOTMID_AUTH_MODE);
  const dataBackend = resolveDataBackend(env.NOTMID_DATA_BACKEND);
  const databaseUrl = env.DATABASE_URL?.trim() || undefined;
  const webOrigin = env.NOTMID_WEB_ORIGIN ?? "http://localhost:3000";
  const port = Number.parseInt(env.NOTMID_API_PORT ?? "8787", 10);
  const errors: string[] = [];
  const mutationRateLimitMax = positiveIntegerConfig(
    env.NOTMID_MUTATION_RATE_LIMIT_MAX,
    120,
    "NOTMID_MUTATION_RATE_LIMIT_MAX",
    errors,
  );
  const mutationRateLimitWindowMs = positiveIntegerConfig(
    env.NOTMID_MUTATION_RATE_LIMIT_WINDOW_MS,
    60_000,
    "NOTMID_MUTATION_RATE_LIMIT_WINDOW_MS",
    errors,
  );

  if (!Number.isInteger(port) || port <= 0 || port > 65535) {
    errors.push("NOTMID_API_PORT must be a valid TCP port.");
  }

  if (!dataBackend) {
    errors.push("NOTMID_DATA_BACKEND must be fixture or postgres.");
  }

  if (dataBackend === "postgres" && !databaseUrl) {
    errors.push("DATABASE_URL is required when NOTMID_DATA_BACKEND=postgres.");
  }

  if (production) {
    if (!env.NOTMID_WEB_ORIGIN?.trim()) {
      errors.push("NOTMID_WEB_ORIGIN is required when NODE_ENV=production.");
    }

    if (!isHttpsOrigin(webOrigin)) {
      errors.push("NOTMID_WEB_ORIGIN must use https when NODE_ENV=production.");
    }

    if (isLocalOrigin(webOrigin)) {
      errors.push("NOTMID_WEB_ORIGIN must not point at localhost in production.");
    }

    if (!env.NOTMID_AUTH_MODE?.trim()) {
      errors.push("NOTMID_AUTH_MODE is required when NODE_ENV=production.");
    } else if (authMode === "fake") {
      errors.push("NOTMID_AUTH_MODE=fake is local-only and cannot run in production.");
    }

    if (!env.NOTMID_DATA_BACKEND?.trim()) {
      errors.push("NOTMID_DATA_BACKEND is required when NODE_ENV=production.");
    } else if (dataBackend === "fixture") {
      errors.push("NOTMID_DATA_BACKEND=fixture is local-only and cannot run in production.");
    }

    if (authMode === "firebase" && !env.FIREBASE_PROJECT_ID?.trim()) {
      errors.push("FIREBASE_PROJECT_ID is required when production auth mode is firebase.");
    }

    if (env.NOTMID_ENABLE_DIAGNOSTIC_FAILURE === "true") {
      errors.push("NOTMID_ENABLE_DIAGNOSTIC_FAILURE is local-only and cannot run in production.");
    }
  }

  return {
    errors,
    value: {
      authMode,
      databaseUrl,
      dataBackend: dataBackend ?? "fixture",
      diagnosticFailureEnabled: env.NOTMID_ENABLE_DIAGNOSTIC_FAILURE === "true",
      firebaseProjectId: env.FIREBASE_PROJECT_ID?.trim() || undefined,
      mutationRateLimit: {
        maxRequests: mutationRateLimitMax,
        windowMs: mutationRateLimitWindowMs,
      },
      nodeEnv,
      port,
      webOrigin,
    },
  };
}

function positiveIntegerConfig(
  value: string | undefined,
  fallback: number,
  name: string,
  errors: string[],
): number {
  const trimmed = value?.trim();

  if (!trimmed) {
    return fallback;
  }

  if (!/^[0-9]+$/.test(trimmed)) {
    errors.push(`${name} must be a positive integer.`);
    return fallback;
  }

  const parsed = Number.parseInt(trimmed, 10);

  if (!Number.isSafeInteger(parsed) || parsed < 1) {
    errors.push(`${name} must be a positive integer.`);
    return fallback;
  }

  return parsed;
}

function resolveAuthMode(mode: string | undefined): NotmidAuthMode {
  if (mode === "firebase" || mode === "disabled") {
    return mode;
  }

  return "fake";
}

function resolveDataBackend(backend: string | undefined): NotmidDataBackend | null {
  if (backend === undefined || backend === "" || backend === "fixture") {
    return "fixture";
  }

  if (backend === "postgres") {
    return backend;
  }

  return null;
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
