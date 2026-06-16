#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

API_PORT="${NOTMID_AUDIT_API_PORT:-8797}"
API_BASE_URL="http://localhost:${API_PORT}"
API_LOG="${TMPDIR:-/tmp}/notmid-api-audit.log"
FAKE_TOKEN="${NOTMID_FAKE_ACCESS_TOKEN:-notmid-fake-local-dev-token}"
SENSITIVE_BODY_MARKER="secret-body-should-not-log"

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

cleanup() {
  if [[ -n "${API_PID:-}" ]]; then
    kill "$API_PID" >/dev/null 2>&1 || true
    wait "$API_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT

require_log() {
  local expected="$1"

  if ! grep -Fq "$expected" "$API_LOG"; then
    echo "Expected audit log to contain: ${expected}" >&2
    cat "$API_LOG" >&2
    return 1
  fi
}

assert_status() {
  local expected_status="$1"
  shift
  local status

  status="$(curl -sS -o /dev/null -w "%{http_code}" "$@")"

  if [[ "$status" != "$expected_status" ]]; then
    echo "Expected HTTP ${expected_status}, got HTTP ${status}" >&2
    return 1
  fi
}

echo "== API audit log dependencies =="
ensure_js_deps

echo "== Starting API audit-log smoke on ${API_BASE_URL} =="
(
  cd apps/api
  exec env NODE_ENV=development NOTMID_API_PORT="$API_PORT" NOTMID_AUTH_MODE=fake \
    node --import tsx src/server.ts
) >"$API_LOG" 2>&1 &
API_PID="$!"
wait_for_url "${API_BASE_URL}/health"

echo "== Protected write audit trail =="
assert_status 401 \
  -X POST "${API_BASE_URL}/v1/capture/publish" \
  -H 'x-request-id: audit-denied-0001' \
  -H 'content-type: application/json' \
  --data "{\"draftId\":\"audit-denied\",\"caption\":\"${SENSITIVE_BODY_MARKER}\",\"placeId\":\"neon-yard\",\"moodTags\":[\"audit\"],\"visibility\":\"public\"}"

assert_status 200 \
  -X POST "${API_BASE_URL}/v1/clips/latte-line-was-worth-it/save" \
  -H 'x-request-id: audit-success-0001' \
  -H "authorization: Bearer ${FAKE_TOKEN}"

assert_status 400 \
  -X POST "${API_BASE_URL}/v1/capture/publish" \
  -H 'x-request-id: audit-failed-0001' \
  -H "authorization: Bearer ${FAKE_TOKEN}" \
  -H 'content-type: application/json' \
  --data '{"draftId":"audit-failed","caption":"","placeId":"neon-yard","moodTags":["audit"],"visibility":"public"}'

require_log '"event":"notmid_api_audit"'
require_log '"requestId":"audit-denied-0001"'
require_log '"action":"capture_publish"'
require_log '"actorId":"anonymous"'
require_log '"outcome":"denied"'
require_log '"status":401'
require_log '"requestId":"audit-success-0001"'
require_log '"action":"clip_save"'
require_log '"actorId":"local-you"'
require_log '"outcome":"success"'
require_log '"status":200'
require_log '"requestId":"audit-failed-0001"'
require_log '"outcome":"failed"'
require_log '"status":400'

if grep -Fq "$FAKE_TOKEN" "$API_LOG"; then
  echo "Audit logs must not contain bearer tokens." >&2
  cat "$API_LOG" >&2
  exit 1
fi

if grep -Fq "$SENSITIVE_BODY_MARKER" "$API_LOG"; then
  echo "Audit logs must not contain request body values." >&2
  cat "$API_LOG" >&2
  exit 1
fi

echo "verify-api-audit-logs passed"
