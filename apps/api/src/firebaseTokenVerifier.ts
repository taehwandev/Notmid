import { createVerify } from "node:crypto";

const googleFirebasePublicKeyEndpoint =
  "https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com";
const maxTokenLength = 8192;
const clockSkewSeconds = 300;

export type FirebaseVerifiedIdToken = {
  uid: string;
  email?: string;
  displayName?: string;
  pictureUrl?: string;
  provider?: string;
  expiresAt: string;
};

export type FirebaseIdTokenVerifier = {
  verifyIdToken: (token: string) => Promise<FirebaseVerifiedIdToken | null>;
};

export type FirebaseCertificateProvider = {
  getCertificate: (kid: string) => Promise<string | null>;
};

export type FirebaseIdTokenVerifierOptions = {
  certificateProvider?: FirebaseCertificateProvider;
  now?: () => number;
  projectId: string;
};

type JwtHeader = {
  alg?: unknown;
  kid?: unknown;
};

type FirebaseTokenPayload = {
  aud?: unknown;
  email?: unknown;
  exp?: unknown;
  firebase?: unknown;
  iat?: unknown;
  iss?: unknown;
  name?: unknown;
  picture?: unknown;
  sub?: unknown;
};

type FirebaseClaim = {
  sign_in_provider?: unknown;
};

export function createFirebaseTokenVerifier(
  options: FirebaseIdTokenVerifierOptions,
): FirebaseIdTokenVerifier {
  const certificateProvider =
    options.certificateProvider ?? createGoogleFirebaseCertificateProvider();
  const now = options.now ?? Date.now;

  return {
    verifyIdToken: async (token) => {
      try {
        return await verifyFirebaseIdToken({
          certificateProvider,
          now,
          projectId: options.projectId,
          token,
        });
      } catch {
        return null;
      }
    },
  };
}

export function createGoogleFirebaseCertificateProvider(
  fetcher: typeof fetch = fetch,
): FirebaseCertificateProvider {
  let cachedCertificates: Record<string, string> = {};
  let cachedUntilMs = 0;

  return {
    getCertificate: async (kid) => {
      const nowMs = Date.now();

      if (cachedUntilMs <= nowMs || !cachedCertificates[kid]) {
        const response = await fetcher(googleFirebasePublicKeyEndpoint, {
          headers: {
            accept: "application/json",
          },
        });

        if (!response.ok) {
          return null;
        }

        const body = await response.json();
        cachedCertificates = normalizeCertificateMap(body);
        cachedUntilMs = nowMs + maxAgeMs(response.headers.get("cache-control"));
      }

      return cachedCertificates[kid] ?? null;
    },
  };
}

async function verifyFirebaseIdToken(options: {
  certificateProvider: FirebaseCertificateProvider;
  now: () => number;
  projectId: string;
  token: string;
}): Promise<FirebaseVerifiedIdToken | null> {
  if (options.token.length > maxTokenLength) {
    return null;
  }

  const tokenParts = options.token.split(".");

  if (tokenParts.length !== 3) {
    return null;
  }

  const [encodedHeader, encodedPayload, encodedSignature] = tokenParts;
  const header = decodeJwtJson<JwtHeader>(encodedHeader);
  const payload = decodeJwtJson<FirebaseTokenPayload>(encodedPayload);

  if (!header || !payload || header.alg !== "RS256" || typeof header.kid !== "string") {
    return null;
  }

  if (!isValidPayload(payload, options.projectId, Math.floor(options.now() / 1000))) {
    return null;
  }

  const certificate = await options.certificateProvider.getCertificate(header.kid);

  if (!certificate) {
    return null;
  }

  const verifier = createVerify("RSA-SHA256");
  verifier.update(`${encodedHeader}.${encodedPayload}`);
  verifier.end();

  if (!verifier.verify(certificate, Buffer.from(encodedSignature, "base64url"))) {
    return null;
  }

  return {
    uid: payload.sub,
    email: stringClaim(payload.email),
    displayName: stringClaim(payload.name),
    pictureUrl: stringClaim(payload.picture),
    provider: firebaseProvider(payload.firebase),
    expiresAt: new Date(payload.exp * 1000).toISOString(),
  };
}

function decodeJwtJson<T>(encodedValue: string): T | null {
  try {
    return JSON.parse(Buffer.from(encodedValue, "base64url").toString("utf8")) as T;
  } catch {
    return null;
  }
}

function isValidPayload(
  payload: FirebaseTokenPayload,
  projectId: string,
  nowSeconds: number,
): payload is FirebaseTokenPayload & {
  aud: string;
  exp: number;
  iat: number;
  iss: string;
  sub: string;
} {
  const issuer = `https://securetoken.google.com/${projectId}`;

  return (
    payload.aud === projectId &&
    payload.iss === issuer &&
    typeof payload.sub === "string" &&
    payload.sub.length > 0 &&
    payload.sub.length <= 128 &&
    typeof payload.exp === "number" &&
    Number.isFinite(payload.exp) &&
    payload.exp > nowSeconds - clockSkewSeconds &&
    typeof payload.iat === "number" &&
    Number.isFinite(payload.iat) &&
    payload.iat <= nowSeconds + clockSkewSeconds
  );
}

function normalizeCertificateMap(value: unknown): Record<string, string> {
  if (!value || typeof value !== "object") {
    return {};
  }

  return Object.entries(value).reduce<Record<string, string>>((certificates, [kid, certificate]) => {
    if (typeof certificate === "string") {
      certificates[kid] = certificate;
    }

    return certificates;
  }, {});
}

function maxAgeMs(cacheControl: string | null): number {
  const maxAge = cacheControl?.match(/max-age=(\d+)/i)?.[1];
  const maxAgeSeconds = maxAge ? Number.parseInt(maxAge, 10) : 300;

  return Number.isFinite(maxAgeSeconds) ? maxAgeSeconds * 1000 : 300_000;
}

function firebaseProvider(firebase: unknown): string | undefined {
  if (!firebase || typeof firebase !== "object") {
    return undefined;
  }

  return stringClaim((firebase as FirebaseClaim).sign_in_provider);
}

function stringClaim(value: unknown): string | undefined {
  return typeof value === "string" && value.trim().length > 0 ? value : undefined;
}
