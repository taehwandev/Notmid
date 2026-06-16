#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

LOG_FILE="${TMPDIR:-/tmp}/notmid-api-persistence-config.log"
SCHEMA_FILES=(
  "apps/api/db/postgres/schema.sql"
  "apps/api/db/postgres/0002_chat_thread_access.sql"
  "apps/api/db/postgres/0003_user_relationships.sql"
)

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

validate_api_config() {
  (
    cd apps/api
    env NOTMID_VALIDATE_CONFIG_ONLY=true "$@" node --import tsx src/server.ts
  )
}

expect_config_failure() {
  local expected="$1"
  shift

  if validate_api_config "$@" >"$LOG_FILE" 2>&1; then
    echo "Expected API persistence config to fail: ${expected}" >&2
    return 1
  fi

  if ! grep -q "$expected" "$LOG_FILE"; then
    echo "Expected API persistence config failure to mention: ${expected}" >&2
    cat "$LOG_FILE" >&2
    return 1
  fi
}

expect_config_success() {
  if ! validate_api_config "$@" >"$LOG_FILE" 2>&1; then
    echo "Expected API persistence config to pass." >&2
    cat "$LOG_FILE" >&2
    return 1
  fi
}

echo "== API persistence config dependencies =="
ensure_js_deps

echo "== API persistence config rejects unsafe production storage =="
expect_config_failure \
  "NOTMID_DATA_BACKEND is required" \
  NODE_ENV=production \
  NOTMID_WEB_ORIGIN=https://thdev.app \
  NOTMID_AUTH_MODE=disabled

expect_config_failure \
  "NOTMID_DATA_BACKEND=fixture is local-only" \
  NODE_ENV=production \
  NOTMID_WEB_ORIGIN=https://thdev.app \
  NOTMID_AUTH_MODE=disabled \
  NOTMID_DATA_BACKEND=fixture

expect_config_failure \
  "DATABASE_URL is required" \
  NODE_ENV=production \
  NOTMID_WEB_ORIGIN=https://thdev.app \
  NOTMID_AUTH_MODE=disabled \
  NOTMID_DATA_BACKEND=postgres

expect_config_failure \
  "NOTMID_DATA_BACKEND must be fixture or postgres" \
  NODE_ENV=production \
  NOTMID_WEB_ORIGIN=https://thdev.app \
  NOTMID_AUTH_MODE=disabled \
  NOTMID_DATA_BACKEND=memory \
  DATABASE_URL="$PROD_DATABASE_URL"

echo "== API persistence config accepts explicit production Postgres =="
expect_config_success \
  NODE_ENV=production \
  NOTMID_WEB_ORIGIN=https://thdev.app \
  NOTMID_AUTH_MODE=disabled \
  NOTMID_DATA_BACKEND=postgres \
  DATABASE_URL="$PROD_DATABASE_URL"

echo "== API persistence schema contract =="
node --input-type=module - "${SCHEMA_FILES[@]}" <<'NODE'
import { readFileSync } from "node:fs";

const schemaPaths = process.argv.slice(2);
const schema = schemaPaths.map((path) => readFileSync(path, "utf8")).join("\n");

const forbiddenPatterns = [
  /\bDROP\b/i,
  /\bTRUNCATE\b/i,
  /\bDELETE\s+FROM\b/i,
  /\bALTER\s+TABLE\b[\s\S]*\bDROP\b/i,
];

for (const pattern of forbiddenPatterns) {
  if (pattern.test(schema)) {
    throw new Error(`Schema contains destructive SQL matching ${pattern}.`);
  }
}

const requiredFragments = [
  "CREATE TABLE IF NOT EXISTS notmid_users",
  "CREATE TABLE IF NOT EXISTS notmid_places",
  "CREATE TABLE IF NOT EXISTS notmid_clips",
  "CREATE TABLE IF NOT EXISTS notmid_saved_clips",
  "CREATE TABLE IF NOT EXISTS notmid_saved_places",
  "CREATE TABLE IF NOT EXISTS notmid_chat_threads",
  "CREATE TABLE IF NOT EXISTS notmid_chat_thread_participants",
  "CREATE TABLE IF NOT EXISTS notmid_chat_messages",
  "CREATE TABLE IF NOT EXISTS notmid_chat_thread_access",
  "CREATE TABLE IF NOT EXISTS notmid_user_relationships",
  "address TEXT NOT NULL DEFAULT ''",
  "open_now BOOLEAN NOT NULL DEFAULT FALSE",
  "score INTEGER NOT NULL DEFAULT 0 CHECK (score >= 0)",
  "REFERENCES notmid_users(id)",
  "REFERENCES notmid_places(id)",
  "REFERENCES notmid_clips(id)",
  "CHECK (visibility IN ('public', 'friends', 'private'))",
  "CHECK (relationship IN ('friend', 'non-friend'))",
  "CHECK (status IN ('friend', 'blocked'))",
  "CHECK (user_id <> related_user_id)",
  "invite_status IN ('accepted', 'pending-inbound', 'pending-outbound', 'rejected')",
  "CREATE INDEX IF NOT EXISTS notmid_clips_place_created_idx",
  "CREATE INDEX IF NOT EXISTS notmid_chat_messages_thread_created_idx",
  "CREATE INDEX IF NOT EXISTS notmid_chat_thread_access_user_status_idx",
  "CREATE INDEX IF NOT EXISTS notmid_user_relationships_related_status_idx",
];

for (const fragment of requiredFragments) {
  if (!schema.includes(fragment)) {
    throw new Error(`Postgres schema is missing required fragment: ${fragment}`);
  }
}
NODE

echo "verify-api-persistence-config passed"
