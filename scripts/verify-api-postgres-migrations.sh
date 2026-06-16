#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

LOG_FILE="${TMPDIR:-/tmp}/notmid-api-postgres-migrations.log"

build_test_database_url() {
  printf '%s://%s:%s@%s:%s/%s' \
    "postgresql" \
    "notmid_user" \
    "notmid_password" \
    "db.example.invalid" \
    "5432" \
    "notmid"
}

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

echo "== API Postgres migration dependencies =="
ensure_js_deps

echo "== API Postgres migration plan =="
plan_output="$(bash scripts/migrate-api-postgres.sh --plan)"
printf '%s\n' "$plan_output"
grep -Fq "0001_initial_schema" <<<"$plan_output"
grep -Fq "0002_chat_thread_access" <<<"$plan_output"
grep -Fq "0003_user_relationships" <<<"$plan_output"

echo "== API Postgres migration apply guard =="
if (cd apps/api && node --import tsx src/runPostgresMigrations.ts --apply) >"$LOG_FILE" 2>&1; then
  echo "Expected Postgres migration apply to require explicit confirmation." >&2
  exit 1
fi
grep -Fq "DATABASE_URL is required" "$LOG_FILE"

if (
  cd apps/api
  env DATABASE_URL="$(build_test_database_url)" \
    node --import tsx src/runPostgresMigrations.ts --apply
) >"$LOG_FILE" 2>&1; then
  echo "Expected Postgres migration apply to require NOTMID_MIGRATION_CONFIRM=apply." >&2
  exit 1
fi
grep -Fq "NOTMID_MIGRATION_CONFIRM=apply" "$LOG_FILE"

echo "== API Postgres migration runner contract =="
(
  cd apps/api
  node --import tsx --input-type=module <<'NODE'
import assert from "node:assert/strict";
import {
  applyPostgresMigrations,
  describePostgresMigrationPlan,
  loadPostgresMigrations,
  splitPostgresStatements,
  validatePostgresMigrations,
} from "./src/postgresMigrations.ts";

const migrations = loadPostgresMigrations();
assert.equal(migrations.length, 3);
assert.equal(migrations[0].id, "0001_initial_schema");
assert.equal(migrations[1].id, "0002_chat_thread_access");
assert.equal(migrations[2].id, "0003_user_relationships");
assert.match(migrations[0].checksum, /^[a-f0-9]{64}$/);
assert.match(migrations[1].checksum, /^[a-f0-9]{64}$/);
assert.match(migrations[2].checksum, /^[a-f0-9]{64}$/);

const plan = describePostgresMigrationPlan(migrations);
assert.equal(plan.length, 3);
assert.equal(plan[0].id, "0001_initial_schema");
assert.equal(plan[1].id, "0002_chat_thread_access");
assert.equal(plan[2].id, "0003_user_relationships");

assert.throws(
  () =>
    validatePostgresMigrations([
      {
        checksum: "bad",
        id: "0002_bad_drop",
        label: "Bad destructive migration",
        path: "inline",
        sql: "DROP TABLE notmid_users;",
      },
    ]),
  /destructive SQL/,
);

assert.deepEqual(splitPostgresStatements("SELECT ';' AS value; SELECT $$;$$ AS value;"), [
  "SELECT ';' AS value",
  "SELECT $$;$$ AS value",
]);

const calls = [];
const appliedRows = [];
const fakeClient = {
  async unsafe(sql, values = []) {
    calls.push({ sql, values });

    if (/SELECT id, checksum/.test(sql)) {
      return appliedRows;
    }

    if (/INSERT INTO notmid_schema_migrations/.test(sql)) {
      appliedRows.push({
        applied_at: "2026-05-30T00:00:00.000Z",
        checksum: values[1],
        id: values[0],
      });
    }

    return [];
  },
  async end() {},
};

const firstRun = await applyPostgresMigrations(fakeClient, migrations);
assert.equal(firstRun.applied.length, 3);
assert.equal(firstRun.skipped.length, 0);
assert(calls.some((call) => call.sql === "BEGIN"));
assert(calls.some((call) => call.sql === "COMMIT"));
assert(calls.some((call) => /CREATE TABLE IF NOT EXISTS notmid_users/.test(call.sql)));
assert(calls.some((call) => /CREATE TABLE IF NOT EXISTS notmid_chat_thread_access/.test(call.sql)));
assert(calls.some((call) => /CREATE TABLE IF NOT EXISTS notmid_user_relationships/.test(call.sql)));
assert(calls.some((call) => /INSERT INTO notmid_schema_migrations/.test(call.sql)));

const callCountAfterFirstRun = calls.length;
const secondRun = await applyPostgresMigrations(fakeClient, migrations);
assert.equal(secondRun.applied.length, 0);
assert.equal(secondRun.skipped.length, 3);
assert.equal(calls.slice(callCountAfterFirstRun).some((call) => call.sql === "BEGIN"), false);

const driftClient = {
  async unsafe(sql) {
    if (/SELECT id, checksum/.test(sql)) {
      return [
        {
          applied_at: "2026-05-30T00:00:00.000Z",
          checksum: "0".repeat(64),
          id: "0001_initial_schema",
        },
      ];
    }

    return [];
  },
  async end() {},
};

await assert.rejects(
  () => applyPostgresMigrations(driftClient, migrations),
  /checksum changed/,
);

console.log("Postgres migration runner checks passed");
NODE
)

echo "verify-api-postgres-migrations passed"
