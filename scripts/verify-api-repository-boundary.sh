#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

API_PORT="${NOTMID_REPOSITORY_API_PORT:-8793}"
API_BASE_URL="http://localhost:${API_PORT}"
API_LOG="${TMPDIR:-/tmp}/notmid-api-repository-boundary.log"
RESPONSE_BODY="${TMPDIR:-/tmp}/notmid-api-repository-boundary-response.$$"
FAKE_TOKEN="${NOTMID_FAKE_ACCESS_TOKEN:-notmid-fake-local-dev-token}"
VALID_PUBLISH_BODY='{"draftId":"repository-smoke","caption":"repository boundary smoke","placeId":"neon-yard","moodTags":["smoke"],"visibility":"public"}'
INVALID_PUBLISH_BODY='{"draftId":"repository-smoke","caption":"","placeId":"neon-yard","moodTags":["smoke"],"visibility":"public"}'
UNKNOWN_PLACE_BODY='{"draftId":"repository-smoke","caption":"repository boundary smoke","placeId":"missing-place","moodTags":["smoke"],"visibility":"public"}'

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
  echo "== Starting API repository boundary smoke on ${API_BASE_URL} =="
  (
    cd apps/api
    exec env NODE_ENV=development NOTMID_API_PORT="$API_PORT" NOTMID_AUTH_MODE=fake \
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
start_api

echo "== Repository read boundary =="
assert_contains '"source":"api"' "${API_BASE_URL}/v1/capture/draft"
assert_contains '"id":"latte-line-was-worth-it"' "${API_BASE_URL}/v1/feed"
assert_contains '"highlightedClipIds"' "${API_BASE_URL}/v1/map"
assert_contains '"id":"neon-yard"' "${API_BASE_URL}/v1/places/neon-yard"
assert_contains '"source":"api"' "${API_BASE_URL}/v1/inbox/threads/tonight-seongsu/detail"
assert_status_contains 404 '"clip_not_found"' "${API_BASE_URL}/v1/clips/missing-clip"

echo "== Repository write boundary =="
assert_status_contains 400 '"invalid_json"' \
  -X POST "${API_BASE_URL}/v1/auth/fake-sign-in" \
  -H 'content-type: application/json' \
  --data '{"provider":'
assert_contains '"id":"receipt-repository-smoke"' \
  -X POST "${API_BASE_URL}/v1/capture/publish" \
  -H "authorization: Bearer ${FAKE_TOKEN}" \
  -H 'content-type: application/json' \
  --data "$VALID_PUBLISH_BODY"
assert_contains '"saved":true' \
  -X POST "${API_BASE_URL}/v1/clips/latte-line-was-worth-it/save" \
  -H "authorization: Bearer ${FAKE_TOKEN}"
assert_status_contains 404 '"clip_not_found"' \
  -X POST "${API_BASE_URL}/v1/clips/missing-clip/save" \
  -H "authorization: Bearer ${FAKE_TOKEN}"
assert_contains '"displayName":"Local You"' \
  "${API_BASE_URL}/v1/profile/settings" \
  -H "authorization: Bearer ${FAKE_TOKEN}"
assert_contains '"updated":true' \
  -X PATCH "${API_BASE_URL}/v1/profile/settings" \
  -H "authorization: Bearer ${FAKE_TOKEN}" \
  -H 'content-type: application/json' \
  --data '{"displayName":"Repository User","homeNeighborhood":"Seongsu Lab"}'
assert_status_contains 400 '"missing_display_name"' \
  -X PATCH "${API_BASE_URL}/v1/profile/settings" \
  -H "authorization: Bearer ${FAKE_TOKEN}" \
  -H 'content-type: application/json' \
  --data '{"displayName":"   ","homeNeighborhood":"Seongsu Lab"}'
assert_status_contains 400 '"missing_caption"' \
  -X POST "${API_BASE_URL}/v1/capture/publish" \
  -H "authorization: Bearer ${FAKE_TOKEN}" \
  -H 'content-type: application/json' \
  --data "$INVALID_PUBLISH_BODY"
assert_status_contains 400 '"invalid_json"' \
  -X POST "${API_BASE_URL}/v1/capture/publish" \
  -H "authorization: Bearer ${FAKE_TOKEN}" \
  -H 'content-type: application/json' \
  --data '['
assert_status_contains 404 '"place_not_found"' \
  -X POST "${API_BASE_URL}/v1/capture/publish" \
  -H "authorization: Bearer ${FAKE_TOKEN}" \
  -H 'content-type: application/json' \
  --data "$UNKNOWN_PLACE_BODY"
assert_contains '"inviteStatus":"accepted"' \
  -X POST "${API_BASE_URL}/v1/inbox/threads" \
  -H "authorization: Bearer ${FAKE_TOKEN}" \
  -H 'content-type: application/json' \
  --data '{"participantHandle":"min.zip","body":"friend start smoke","attachedClipId":"latte-line-was-worth-it"}'
assert_contains '"inviteStatus":"pending-outbound"' \
  -X POST "${API_BASE_URL}/v1/inbox/threads" \
  -H "authorization: Bearer ${FAKE_TOKEN}" \
  -H 'content-type: application/json' \
  --data '{"participantHandle":"receipt.han","body":"non-friend start smoke","attachedPlaceId":"underpass-gallery"}'
assert_status_contains 403 '"chat_invite_required"' \
  -X POST "${API_BASE_URL}/v1/inbox/threads/thread-receipt-han-you-local-underpass-gallery/messages" \
  -H "authorization: Bearer ${FAKE_TOKEN}" \
  -H 'content-type: application/json' \
  --data '{"body":"blocked until the request is accepted"}'
assert_status_contains 404 '"chat_participant_not_found"' \
  -X POST "${API_BASE_URL}/v1/inbox/threads" \
  -H "authorization: Bearer ${FAKE_TOKEN}" \
  -H 'content-type: application/json' \
  --data '{"participantHandle":"missing.handle","body":"unknown target"}'
assert_contains '"id":"msg-tonight-seongsu-local-reply"' \
  -X POST "${API_BASE_URL}/v1/inbox/threads/tonight-seongsu/messages" \
  -H "authorization: Bearer ${FAKE_TOKEN}" \
  -H 'content-type: application/json' \
  --data '{"body":"repository boundary reply"}'
assert_status_contains 403 '"chat_invite_required"' \
  -X POST "${API_BASE_URL}/v1/inbox/threads/rain-route/messages" \
  -H "authorization: Bearer ${FAKE_TOKEN}" \
  -H 'content-type: application/json' \
  --data '{"body":"blocked before invite acceptance"}'
assert_contains '"inviteStatus":"accepted"' \
  -X POST "${API_BASE_URL}/v1/inbox/threads/rain-route/invite/accept" \
  -H "authorization: Bearer ${FAKE_TOKEN}"
assert_contains '"id":"msg-rain-route-local-reply"' \
  -X POST "${API_BASE_URL}/v1/inbox/threads/rain-route/messages" \
  -H "authorization: Bearer ${FAKE_TOKEN}" \
  -H 'content-type: application/json' \
  --data '{"body":"repository boundary reply after acceptance"}'
assert_status_contains 400 '"empty_message"' \
  -X POST "${API_BASE_URL}/v1/inbox/threads/tonight-seongsu/messages" \
  -H "authorization: Bearer ${FAKE_TOKEN}" \
  -H 'content-type: application/json' \
  --data '{"body":"   "}'
assert_status_contains 400 '"invalid_json"' \
  -X PATCH "${API_BASE_URL}/v1/profile/settings" \
  -H "authorization: Bearer ${FAKE_TOKEN}" \
  -H 'content-type: application/json' \
  --data '[]'

echo "verify-api-repository-boundary passed"
