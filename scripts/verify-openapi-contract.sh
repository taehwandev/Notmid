#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

API_PORT="${NOTMID_OPENAPI_API_PORT:-8791}"
API_BASE_URL="http://localhost:${API_PORT}"
API_LOG="${TMPDIR:-/tmp}/notmid-openapi-api.log"
OPENAPI_JSON="${TMPDIR:-/tmp}/notmid-openapi.json"

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

wait_for_url() {
  local url="$1"
  local attempts="${2:-40}"

  for _ in $(seq 1 "$attempts"); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      return 0
    fi
    sleep 0.5
  done

  echo "Timed out waiting for ${url}" >&2
  return 1
}

cleanup() {
  if [[ -n "${API_PID:-}" ]]; then
    kill "$API_PID" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

echo "== OpenAPI dependencies =="
ensure_js_deps

echo "== Starting API on ${API_BASE_URL} =="
(
  cd apps/api
  NOTMID_API_PORT="$API_PORT" node --import tsx src/server.ts
) >"$API_LOG" 2>&1 &
API_PID="$!"
wait_for_url "${API_BASE_URL}/health"

echo "== OpenAPI endpoint =="
curl -fsS "${API_BASE_URL}/openapi.json" -o "$OPENAPI_JSON"

node --input-type=module - "$OPENAPI_JSON" <<'NODE'
import { readFileSync } from "node:fs";

const documentPath = process.argv.at(-1);
const document = JSON.parse(readFileSync(documentPath, "utf8"));

const expectedPaths = [
  "/health",
  "/openapi.json",
  "/v1/auth/status",
  "/v1/auth/fake-sign-in",
  "/v1/profile/settings",
  "/v1/capture/draft",
  "/v1/capture/publish",
  "/v1/feed",
  "/v1/map",
  "/v1/clips/{clipId}",
  "/v1/clips/{clipId}/save",
  "/v1/places/{placeId}",
  "/v1/inbox/threads",
  "/v1/inbox/threads/{threadId}",
  "/v1/inbox/threads/{threadId}/detail",
  "/v1/inbox/threads/{threadId}/invite/accept",
  "/v1/inbox/threads/{threadId}/invite/reject",
  "/v1/inbox/threads/{threadId}/messages",
  "/v1/deeplinks/resolve",
];

const expectedSchemas = [
  "NotmidAuthStatusResponse",
  "NotmidClipSaveResponse",
  "NotmidProfileSettingsResponse",
  "NotmidProfileSettingsUpdateRequest",
  "NotmidProfileSettingsUpdateResponse",
  "NotmidCaptureDraftResponse",
  "NotmidCapturePublishRequest",
  "NotmidFeedResponse",
  "NotmidMapResponse",
  "NotmidInboxResponse",
  "NotmidChatAccess",
  "NotmidChatInviteResponse",
  "NotmidStartThreadRequest",
  "NotmidStartThreadResponse",
  "NotmidThreadDetailResponse",
  "NotmidResolvedRouteStack",
  "NotmidErrorResponse",
];

if (typeof document.openapi !== "string" || !document.openapi.startsWith("3.")) {
  throw new Error("OpenAPI document must use OpenAPI 3.x.");
}

for (const path of expectedPaths) {
  if (!document.paths?.[path]) {
    throw new Error(`Missing OpenAPI path: ${path}`);
  }
}

for (const schema of expectedSchemas) {
  if (!document.components?.schemas?.[schema]) {
    throw new Error(`Missing OpenAPI schema: ${schema}`);
  }
}

const operations = Object.values(document.paths).flatMap((pathItem) =>
  Object.values(pathItem).filter((operation) => typeof operation === "object"),
);
const mutationResponseContracts = {
  "/v1/auth/fake-sign-in": ["400", "429"],
  "/v1/profile/settings": ["429"],
  "/v1/capture/publish": ["429"],
  "/v1/clips/{clipId}/save": ["429"],
  "/v1/inbox/threads": ["429"],
  "/v1/inbox/threads/{threadId}/invite/accept": ["429"],
  "/v1/inbox/threads/{threadId}/invite/reject": ["429"],
  "/v1/inbox/threads/{threadId}/messages": ["429"],
};

for (const operation of operations) {
  if (!operation.operationId) {
    throw new Error("Every OpenAPI operation must define operationId.");
  }
}

for (const [path, requiredStatuses] of Object.entries(mutationResponseContracts)) {
  const pathItem = document.paths?.[path];
  const operation = pathItem?.post ?? pathItem?.patch;

  if (!operation) {
    throw new Error(`Missing mutating OpenAPI operation for ${path}`);
  }

  for (const status of requiredStatuses) {
    if (!operation.responses?.[status]) {
      throw new Error(`Missing OpenAPI ${status} response for ${path}`);
    }
  }
}
NODE

echo "verify-openapi-contract passed"
