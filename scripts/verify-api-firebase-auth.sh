#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

pnpm_cmd() {
  if command -v pnpm >/dev/null 2>&1; then
    pnpm "$@"
  else
    npm exec --yes pnpm@10.12.1 -- "$@"
  fi
}

ensure_js_deps() {
  if [[ -x apps/api/node_modules/.bin/tsx ]]; then
    echo "API dependencies already installed"
  else
    pnpm_cmd install --frozen-lockfile
  fi
}

echo "== API Firebase auth dependencies =="
ensure_js_deps

echo "== Firebase ID token verifier =="
(
  cd apps/api
  node --import tsx --input-type=module <<'NODE'
import assert from "node:assert/strict";
import { createSign, generateKeyPairSync } from "node:crypto";
import { createFirebaseTokenVerifier } from "./src/firebaseTokenVerifier.ts";
import {
  authStatusForContext,
  isAuthorizedWrite,
  resolveAuthContext,
} from "./src/authPolicy.ts";

const projectId = "notmid-test-project";
const kid = "notmid-test-key";
const nowSeconds = 1_800_000_000;
const issuer = `https://securetoken.google.com/${projectId}`;
const { privateKey, publicKey } = generateKeyPairSync("rsa", { modulusLength: 2048 });
const publicKeyPem = publicKey.export({ type: "spki", format: "pem" }).toString();
const verifier = createFirebaseTokenVerifier({
  projectId,
  now: () => nowSeconds * 1000,
  certificateProvider: {
    getCertificate: async (requestedKid) => (requestedKid === kid ? publicKeyPem : null),
  },
});

function encodeJson(value) {
  return Buffer.from(JSON.stringify(value)).toString("base64url");
}

function signToken(payload, header = { alg: "RS256", kid, typ: "JWT" }) {
  const unsignedToken = `${encodeJson(header)}.${encodeJson(payload)}`;
  const signature = createSign("RSA-SHA256").update(unsignedToken).sign(privateKey);

  return `${unsignedToken}.${signature.toString("base64url")}`;
}

const validToken = signToken({
  aud: projectId,
  iss: issuer,
  sub: "firebase-user-001",
  iat: nowSeconds - 60,
  exp: nowSeconds + 3600,
  email: "verified@example.com",
  name: "Verified Example",
  picture: "https://example.test/avatar.png",
  firebase: {
    sign_in_provider: "google.com",
  },
});

const verifiedToken = await verifier.verifyIdToken(validToken);
assert.equal(verifiedToken?.uid, "firebase-user-001");
assert.equal(verifiedToken?.email, "verified@example.com");
assert.equal(verifiedToken?.displayName, "Verified Example");
assert.equal(verifiedToken?.provider, "google.com");

const authContext = await resolveAuthContext("firebase", `Bearer ${validToken}`, {
  firebaseTokenVerifier: verifier,
});
assert.equal(authContext.authenticated, true);
assert.equal(authContext.tokenKind, "firebase-id-token");
assert.equal(authContext.provider, "google");
assert.equal(authContext.user?.id, "firebase:firebase-user-001");
assert.equal(isAuthorizedWrite(authContext), true);

const status = authStatusForContext(authContext);
assert.equal(status.source, "api");
assert.equal(status.mode, "firebase");
assert.equal(status.authenticated, true);
assert.equal(status.user?.handle, "verified");

const anonymousToken = signToken({
  aud: projectId,
  iss: issuer,
  sub: "anonymous-user-001",
  iat: nowSeconds - 60,
  exp: nowSeconds + 3600,
  firebase: {
    sign_in_provider: "anonymous",
  },
});
const anonymousContext = await resolveAuthContext("firebase", `Bearer ${anonymousToken}`, {
  firebaseTokenVerifier: verifier,
});
assert.equal(anonymousContext.authenticated, true);
assert.equal(anonymousContext.provider, "anonymous");

const wrongAudienceToken = signToken({
  aud: "other-project",
  iss: issuer,
  sub: "firebase-user-001",
  iat: nowSeconds - 60,
  exp: nowSeconds + 3600,
});
assert.equal(await verifier.verifyIdToken(wrongAudienceToken), null);

const expiredToken = signToken({
  aud: projectId,
  iss: issuer,
  sub: "firebase-user-001",
  iat: nowSeconds - 7200,
  exp: nowSeconds - 600,
});
assert.equal(await verifier.verifyIdToken(expiredToken), null);

const wrongKidToken = signToken(
  {
    aud: projectId,
    iss: issuer,
    sub: "firebase-user-001",
    iat: nowSeconds - 60,
    exp: nowSeconds + 3600,
  },
  { alg: "RS256", kid: "unknown-key", typ: "JWT" },
);
assert.equal(await verifier.verifyIdToken(wrongKidToken), null);

const wrongAlgorithmToken = signToken(
  {
    aud: projectId,
    iss: issuer,
    sub: "firebase-user-001",
    iat: nowSeconds - 60,
    exp: nowSeconds + 3600,
  },
  { alg: "none", kid, typ: "JWT" },
);
assert.equal(await verifier.verifyIdToken(wrongAlgorithmToken), null);

const fakeTokenInFirebaseMode = await resolveAuthContext(
  "firebase",
  "Bearer notmid-fake-local-dev-token",
  { firebaseTokenVerifier: verifier },
);
assert.equal(fakeTokenInFirebaseMode.authenticated, false);
assert.equal(fakeTokenInFirebaseMode.tokenKind, "firebase-unverified");

const fakeTokenInFakeMode = await resolveAuthContext("fake", "Bearer notmid-fake-local-dev-token", {
  firebaseTokenVerifier: verifier,
});
assert.equal(fakeTokenInFakeMode.authenticated, true);
assert.equal(fakeTokenInFakeMode.tokenKind, "fake-local");

console.log("Firebase ID token verifier checks passed");
NODE
)

echo "verify-api-firebase-auth passed"
