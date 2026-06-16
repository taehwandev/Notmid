#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

API_PORT="${NOTMID_DIAGNOSTICS_API_PORT:-8794}"
API_BASE_URL="http://localhost:${API_PORT}"
API_LOG="${TMPDIR:-/tmp}/notmid-api-diagnostics.log"
HEADERS_FILE="${TMPDIR:-/tmp}/notmid-api-diagnostics-headers.$$"
BODY_FILE="${TMPDIR:-/tmp}/notmid-api-diagnostics-body.$$"
REQUEST_ID="notmid-test-request-001"

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
  tail -80 "$API_LOG" >&2 || true
  return 1
}

start_api() {
  echo "== Starting API diagnostics smoke on ${API_BASE_URL} =="
  (
    cd apps/api
    exec env NODE_ENV=development NOTMID_API_PORT="$API_PORT" NOTMID_AUTH_MODE=fake \
      NOTMID_ENABLE_DIAGNOSTIC_FAILURE=true node --import tsx src/server.ts
  ) >"$API_LOG" 2>&1 &
  API_PID="$!"
  wait_for_url "${API_BASE_URL}/health"
}

cleanup() {
  if [[ -n "${API_PID:-}" ]]; then
    kill "$API_PID" >/dev/null 2>&1 || true
    wait "$API_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT

request_status() {
  curl -sS -D "$HEADERS_FILE" -o "$BODY_FILE" -w "%{http_code}" "$@"
}

expect_status_body_header() {
  local expected_status="$1"
  local expected_body="$2"
  local expected_header="$3"
  shift 3
  local status

  status="$(request_status "$@")"

  if [[ "$status" != "$expected_status" ]]; then
    echo "Expected HTTP ${expected_status}, got HTTP ${status}" >&2
    cat "$BODY_FILE" >&2
    return 1
  fi

  if ! grep -Fq "$expected_body" "$BODY_FILE"; then
    echo "Expected response body to contain: ${expected_body}" >&2
    cat "$BODY_FILE" >&2
    return 1
  fi

  if ! grep -iq "$expected_header" "$HEADERS_FILE"; then
    echo "Expected response headers to contain: ${expected_header}" >&2
    cat "$HEADERS_FILE" >&2
    return 1
  fi
}

echo "== API dependencies =="
ensure_js_deps
start_api

echo "== Request id propagation =="
expect_status_body_header \
  200 \
  "\"requestId\":\"${REQUEST_ID}\"" \
  "x-request-id: ${REQUEST_ID}" \
  -H "x-request-id: ${REQUEST_ID}" \
  "${API_BASE_URL}/health"

if ! grep -iq "x-content-type-options: nosniff" "$HEADERS_FILE"; then
  echo "Expected API response to include x-content-type-options: nosniff" >&2
  cat "$HEADERS_FILE" >&2
  exit 1
fi

if ! grep -iq "referrer-policy: no-referrer" "$HEADERS_FILE"; then
  echo "Expected API response to include referrer-policy: no-referrer" >&2
  cat "$HEADERS_FILE" >&2
  exit 1
fi

if ! grep -iq "x-frame-options: DENY" "$HEADERS_FILE"; then
  echo "Expected API response to include x-frame-options: DENY" >&2
  cat "$HEADERS_FILE" >&2
  exit 1
fi

echo "== Stable not-found diagnostics =="
expect_status_body_header \
  404 \
  '"code":"route_not_found"' \
  "x-request-id: ${REQUEST_ID}" \
  -H "x-request-id: ${REQUEST_ID}" \
  "${API_BASE_URL}/v1/unknown"
grep -Fq "\"requestId\":\"${REQUEST_ID}\"" "$BODY_FILE"

echo "== Stable internal-error diagnostics =="
expect_status_body_header \
  500 \
  '"code":"internal_error"' \
  "x-request-id: ${REQUEST_ID}" \
  -H "x-request-id: ${REQUEST_ID}" \
  "${API_BASE_URL}/__notmid/diagnostic-error"
grep -Fq "\"requestId\":\"${REQUEST_ID}\"" "$BODY_FILE"

if grep -Fq "Notmid diagnostic failure" "$BODY_FILE"; then
  echo "Internal error response leaked the exception message." >&2
  cat "$BODY_FILE" >&2
  exit 1
fi

if ! grep -Fq '"event":"notmid_api_unhandled_error"' "$API_LOG"; then
  echo "Expected API log to contain the privacy-safe unhandled error event." >&2
  cat "$API_LOG" >&2
  exit 1
fi

echo "verify-api-diagnostics passed"
