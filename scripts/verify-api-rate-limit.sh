#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

API_PORT="${NOTMID_RATE_LIMIT_API_PORT:-8796}"
API_BASE_URL="http://localhost:${API_PORT}"
API_LOG="${TMPDIR:-/tmp}/notmid-api-rate-limit.log"
HEADERS_FILE="${TMPDIR:-/tmp}/notmid-api-rate-limit-headers.$$"
BODY_FILE="${TMPDIR:-/tmp}/notmid-api-rate-limit-body.$$"

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
  echo "== Starting API rate-limit smoke on ${API_BASE_URL} =="
  (
    cd apps/api
    exec env NODE_ENV=development NOTMID_API_PORT="$API_PORT" NOTMID_AUTH_MODE=fake \
      NOTMID_MUTATION_RATE_LIMIT_MAX=1 \
      NOTMID_MUTATION_RATE_LIMIT_WINDOW_MS=60000 \
      node --import tsx src/server.ts
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

fake_sign_in_status() {
  local ip="$1"
  request_status \
    -X POST \
    -H "x-forwarded-for: ${ip}" \
    -H "content-type: application/json" \
    --data '{"provider":"fake","returnTo":"/notmid"}' \
    "${API_BASE_URL}/v1/auth/fake-sign-in"
}

echo "== API dependencies =="
ensure_js_deps
start_api

echo "== Mutable API requests are limited per client key =="
if [[ "$(fake_sign_in_status 203.0.113.10)" != "200" ]]; then
  echo "Expected first mutable request to pass." >&2
  cat "$BODY_FILE" >&2
  exit 1
fi

if [[ "$(fake_sign_in_status 203.0.113.10)" != "429" ]]; then
  echo "Expected second mutable request from same client to be rate limited." >&2
  cat "$BODY_FILE" >&2
  exit 1
fi

if ! grep -Fq '"code":"rate_limited"' "$BODY_FILE"; then
  echo "Rate limited response did not use the stable rate_limited error code." >&2
  cat "$BODY_FILE" >&2
  exit 1
fi

if ! grep -iq "retry-after:" "$HEADERS_FILE"; then
  echo "Rate limited response did not include retry-after." >&2
  cat "$HEADERS_FILE" >&2
  exit 1
fi

if [[ "$(fake_sign_in_status 203.0.113.11)" != "200" ]]; then
  echo "Expected a different client key to have its own rate-limit bucket." >&2
  cat "$BODY_FILE" >&2
  exit 1
fi

echo "== Read-only API requests are not counted as mutations =="
curl -fsS "${API_BASE_URL}/health" | grep -q '"service":"notmid-api"'
curl -fsS "${API_BASE_URL}/v1/feed" | grep -q '"clips"'

echo "verify-api-rate-limit passed"
