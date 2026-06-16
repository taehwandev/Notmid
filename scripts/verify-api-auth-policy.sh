#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

API_PORT="${NOTMID_AUTH_POLICY_API_PORT:-8792}"
API_BASE_URL="http://localhost:${API_PORT}"
API_LOG="${TMPDIR:-/tmp}/notmid-api-auth-policy.log"
RESPONSE_BODY="${TMPDIR:-/tmp}/notmid-api-auth-policy-response.$$"
FAKE_TOKEN="${NOTMID_FAKE_ACCESS_TOKEN:-notmid-fake-local-dev-token}"
PUBLISH_BODY='{"draftId":"local-draft-001","caption":"auth policy smoke","placeId":"neon-yard","moodTags":["smoke"],"visibility":"public"}'

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
  local mode="$1"
  shift

  echo "== Starting API auth mode ${mode} on ${API_BASE_URL} =="
  (
    cd apps/api
    exec env NODE_ENV=development NOTMID_API_PORT="$API_PORT" NOTMID_AUTH_MODE="$mode" "$@" \
      node --import tsx src/server.ts
  ) >"$API_LOG" 2>&1 &
  API_PID="$!"
  wait_for_url "${API_BASE_URL}/health"
}

stop_api() {
  if [[ -n "${API_PID:-}" ]]; then
    kill "$API_PID" >/dev/null 2>&1 || true
    wait "$API_PID" 2>/dev/null || true
    unset API_PID
  fi
}

cleanup() {
  stop_api
}
trap cleanup EXIT

assert_contains() {
  local expected="$1"
  shift
  local body

  body="$(curl -fsS "$@")"

  if [[ "$body" != *"$expected"* ]]; then
    echo "Expected response to contain: ${expected}" >&2
    printf '%s\n' "$body" >&2
    return 1
  fi
}

assert_status_contains() {
  local expected_status="$1"
  local expected_text="$2"
  shift 2
  local status

  status="$(curl -sS -o "$RESPONSE_BODY" -w "%{http_code}" "$@")"

  if [[ "$status" != "$expected_status" ]]; then
    echo "Expected HTTP ${expected_status}, got HTTP ${status}" >&2
    cat "$RESPONSE_BODY" >&2
    return 1
  fi

  if ! grep -Fq "$expected_text" "$RESPONSE_BODY"; then
    echo "Expected response to contain: ${expected_text}" >&2
    cat "$RESPONSE_BODY" >&2
    return 1
  fi
}

echo "== API dependencies =="
ensure_js_deps

echo "== Fake auth mode allows only the local fake token =="
start_api fake
assert_contains '"authenticated":false' "${API_BASE_URL}/v1/auth/status"
assert_contains '"authenticated":true' \
  -H "authorization: Bearer ${FAKE_TOKEN}" \
  "${API_BASE_URL}/v1/auth/status"
assert_contains '"moderationStatus":"queued"' \
  -X POST "${API_BASE_URL}/v1/capture/publish" \
  -H "authorization: Bearer ${FAKE_TOKEN}" \
  -H 'content-type: application/json' \
  --data "$PUBLISH_BODY"
stop_api

echo "== Disabled auth mode rejects fake token writes =="
start_api disabled
assert_status_contains 409 '"fake_auth_disabled"' \
  -X POST "${API_BASE_URL}/v1/auth/fake-sign-in" \
  -H 'content-type: application/json' \
  --data '{"provider":"fake","returnTo":"/notmid/capture"}'
assert_contains '"mode":"disabled"' \
  -H "authorization: Bearer ${FAKE_TOKEN}" \
  "${API_BASE_URL}/v1/auth/status"
assert_contains '"authenticated":false' \
  -H "authorization: Bearer ${FAKE_TOKEN}" \
  "${API_BASE_URL}/v1/auth/status"
assert_status_contains 401 '"auth_required"' \
  -X POST "${API_BASE_URL}/v1/capture/publish" \
  -H "authorization: Bearer ${FAKE_TOKEN}" \
  -H 'content-type: application/json' \
  --data "$PUBLISH_BODY"
stop_api

echo "== Firebase auth mode does not trust unverified bearer tokens =="
start_api firebase FIREBASE_PROJECT_ID=notmid-prod-placeholder
assert_contains '"mode":"firebase"' \
  -H "authorization: Bearer ${FAKE_TOKEN}" \
  "${API_BASE_URL}/v1/auth/status"
assert_contains '"authenticated":false' \
  -H "authorization: Bearer ${FAKE_TOKEN}" \
  "${API_BASE_URL}/v1/auth/status"
assert_status_contains 401 '"auth_required"' \
  -X POST "${API_BASE_URL}/v1/capture/publish" \
  -H "authorization: Bearer ${FAKE_TOKEN}" \
  -H 'content-type: application/json' \
  --data "$PUBLISH_BODY"
stop_api

echo "verify-api-auth-policy passed"
