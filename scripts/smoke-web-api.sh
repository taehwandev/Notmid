#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

API_PORT="${NOTMID_API_PORT:-8787}"
WEB_PORT="${NOTMID_WEB_PORT:-3001}"
API_BASE_URL="http://localhost:${API_PORT}"
WEB_BASE_URL="http://localhost:${WEB_PORT}"
API_LOG="${TMPDIR:-/tmp}/notmid-api-smoke.log"
WEB_LOG="${TMPDIR:-/tmp}/notmid-web-smoke.log"

pnpm_cmd() {
  if command -v pnpm >/dev/null 2>&1; then
    pnpm "$@"
  else
    npm exec --yes pnpm@10.12.1 -- "$@"
  fi
}

ensure_js_deps() {
  if [[ -x apps/api/node_modules/.bin/tsx && -x apps/web/node_modules/.bin/next ]]; then
    echo "Web/API dependencies already installed"
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
  if [[ -n "${WEB_PID:-}" ]]; then
    kill "$WEB_PID" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

echo "== Web/API dependencies =="
ensure_js_deps

echo "== Starting API on ${API_BASE_URL} =="
(
  cd apps/api
  NOTMID_API_PORT="$API_PORT" node --import tsx src/server.ts
) >"$API_LOG" 2>&1 &
API_PID="$!"
wait_for_url "${API_BASE_URL}/health"

echo "== Starting web on ${WEB_BASE_URL} =="
(
  cd apps/web
  NOTMID_API_BASE_URL="$API_BASE_URL" node_modules/.bin/next dev -p "$WEB_PORT"
) >"$WEB_LOG" 2>&1 &
WEB_PID="$!"
wait_for_url "${WEB_BASE_URL}/notmid"

echo "== API smoke =="
curl -fsS "${API_BASE_URL}/health" | grep -q '"service":"notmid-api"'
curl -fsS "${API_BASE_URL}/openapi.json" | grep -q '"operationId":"getFeed"'
curl -fsS "${API_BASE_URL}/v1/auth/status" | grep -q '"authenticated":false'
curl -fsS -X POST "${API_BASE_URL}/v1/auth/fake-sign-in" \
  -H 'content-type: application/json' \
  --data '{"provider":"fake","returnTo":"/notmid/capture"}' |
  grep -q '"accessToken":"notmid-fake-local-dev-token"'
curl -fsS "${API_BASE_URL}/v1/clips/latte-line-was-worth-it" | grep -q '"id":"latte-line-was-worth-it"'
curl -fsS "${API_BASE_URL}/v1/deeplinks/resolve?url=https%3A%2F%2Fthdev.app%2Fnotmid%2Fprofile%2Fsettings" |
  grep -q '"profile-settings"'
curl -fsS "${API_BASE_URL}/v1/deeplinks/resolve?url=https%3A%2F%2Fthdev.app%2Fnotmid%2Flogin%3Fnext%3D%252Fnotmid%252Fcapture" |
  grep -q '"login"'
if [[ "$(curl -sS -o /dev/null -w '%{http_code}' -X POST "${API_BASE_URL}/v1/capture/publish" \
  -H 'content-type: application/json' \
  --data '{"draftId":"draft-local-receipt","caption":"Smoke publish","placeId":"neon-yard","moodTags":["smoke"],"visibility":"public"}')" != "401" ]]; then
  echo "Expected unauthenticated capture publish to return 401" >&2
  exit 1
fi
if [[ "$(curl -sS -o /dev/null -w '%{http_code}' -X POST "${API_BASE_URL}/v1/clips/latte-line-was-worth-it/save")" != "401" ]]; then
  echo "Expected unauthenticated clip save to return 401" >&2
  exit 1
fi
if [[ "$(curl -sS -o /dev/null -w '%{http_code}' -X PATCH "${API_BASE_URL}/v1/profile/settings" \
  -H 'content-type: application/json' \
  --data '{"displayName":"Smoke User","homeNeighborhood":"Seongsu"}')" != "401" ]]; then
  echo "Expected unauthenticated profile settings update to return 401" >&2
  exit 1
fi
curl -fsS -X POST "${API_BASE_URL}/v1/capture/publish" \
  -H 'authorization: Bearer notmid-fake-local-dev-token' \
  -H 'content-type: application/json' \
  --data '{"draftId":"draft-local-receipt","caption":"Smoke publish","placeId":"neon-yard","moodTags":["smoke"],"visibility":"public"}' |
  grep -q '"moderationStatus":"queued"'
curl -fsS -X POST "${API_BASE_URL}/v1/clips/latte-line-was-worth-it/save" \
  -H 'authorization: Bearer notmid-fake-local-dev-token' |
  grep -q '"saved":true'
curl -fsS "${API_BASE_URL}/v1/profile/settings" \
  -H 'authorization: Bearer notmid-fake-local-dev-token' |
  grep -q '"displayName":"Local You"'
curl -fsS -X PATCH "${API_BASE_URL}/v1/profile/settings" \
  -H 'authorization: Bearer notmid-fake-local-dev-token' \
  -H 'content-type: application/json' \
  --data '{"displayName":"Smoke User","homeNeighborhood":"Seongsu Smoke"}' |
  grep -q '"updated":true'
if [[ "$(curl -sS -o /dev/null -w '%{http_code}' -X POST "${API_BASE_URL}/v1/inbox/threads/tonight-seongsu/messages" \
  -H 'content-type: application/json' \
  --data '{"body":"Smoke message"}')" != "401" ]]; then
  echo "Expected unauthenticated chat send to return 401" >&2
  exit 1
fi
curl -fsS -X POST "${API_BASE_URL}/v1/inbox/threads/tonight-seongsu/messages" \
  -H 'authorization: Bearer notmid-fake-local-dev-token' \
  -H 'content-type: application/json' \
  --data '{"body":"Smoke message"}' |
  grep -q '"mine":true'

echo "== Web smoke =="
curl -fsS "${WEB_BASE_URL}/notmid" | grep -q 'latte line was worth it'
curl -fsS "${WEB_BASE_URL}/notmid/login?next=%2Fnotmid%2Fcapture" | grep -q 'show receipts'
curl -fsS "${WEB_BASE_URL}/notmid/capture" | grep -q 'sign in to publish receipts'
curl -fsS "${WEB_BASE_URL}/notmid/inbox" | grep -q 'pull up plans'
curl -fsS "${WEB_BASE_URL}/notmid/chats/tonight-seongsu" | grep -q 'Attached context'
curl -fsSI "${WEB_BASE_URL}/notmid/clips/latte-line-was-worth-it" | grep -q '200 OK'
curl -fsSI "${WEB_BASE_URL}/notmid/places/neon-yard" | grep -q '200 OK'
curl -fsS "${WEB_BASE_URL}/notmid/profile" | grep -q 'profile starts after sign in'
curl -fsS "${WEB_BASE_URL}/notmid/profile/settings" | grep -q 'open-source mode'

echo "smoke-web-api passed"
