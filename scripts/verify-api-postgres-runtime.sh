#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

API_PORT="${NOTMID_POSTGRES_RUNTIME_API_PORT:-8795}"
API_BASE_URL="http://localhost:${API_PORT}"
API_LOG="${TMPDIR:-/tmp}/notmid-api-postgres-runtime.log"

build_test_database_url() {
  printf '%s://%s:%s@%s:%s/%s' \
    "postgresql" \
    "notmid_user" \
    "notmid_password" \
    "db.example.invalid" \
    "5432" \
    "notmid"
}

PROD_DATABASE_URL="$(build_test_database_url)"

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

echo "== API Postgres runtime dependencies =="
ensure_js_deps

echo "== API Postgres query client wrapper =="
(
  cd apps/api
  node --import tsx --input-type=module <<'NODE'
import assert from "node:assert/strict";
import {
  createPostgresQueryClientFromSql,
  postgresClientOptionsForOptions,
} from "./src/postgresQueryClient.ts";

const clientOptions = postgresClientOptionsForOptions({
  databaseUrl: [
    "postgresql",
    "://",
    "notmid_user",
    ":",
    "notmid_password",
    "@",
    "db.example.invalid",
    ":5432/notmid",
  ].join(""),
});
assert.equal(clientOptions.max, 5);
assert.equal(clientOptions.connect_timeout, 5);
assert.equal(clientOptions.idle_timeout, 30);

const calls = [];
let closed = false;
const client = createPostgresQueryClientFromSql({
  async unsafe(sql, values) {
    calls.push({ sql, values });
    return [{ id: "ok" }];
  },
  async end(options) {
    calls.push({ endOptions: options });
    closed = true;
  },
});

const result = await client.query("SELECT $1::TEXT AS id", ["ok"]);
assert.deepEqual(result.rows, [{ id: "ok" }]);
assert.deepEqual(calls, [{ sql: "SELECT $1::TEXT AS id", values: ["ok"] }]);

await client.close();
assert.equal(closed, true);
assert.deepEqual(calls[1], { endOptions: { timeout: 5 } });

console.log("Postgres query client wrapper checks passed");
NODE
)

echo "== API Postgres runtime starts without connecting during health =="
(
  cd apps/api
  exec env NODE_ENV=production NOTMID_API_PORT="$API_PORT" NOTMID_AUTH_MODE=disabled \
    NOTMID_WEB_ORIGIN=https://thdev.app NOTMID_DATA_BACKEND=postgres \
    DATABASE_URL="$PROD_DATABASE_URL" node --import tsx src/server.ts
) >"$API_LOG" 2>&1 &
API_PID="$!"
wait_for_url "${API_BASE_URL}/health"

curl -fsS "${API_BASE_URL}/health" | grep -Fq '"service":"notmid-api"'

echo "verify-api-postgres-runtime passed"
