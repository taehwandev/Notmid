#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

LOG_FILE="${TMPDIR:-/tmp}/notmid-api-production-config.log"

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

validate_web_config() {
  (
    cd apps/web
    env NOTMID_VALIDATE_WEB_CONFIG_ONLY=true "$@" ../api/node_modules/.bin/tsx src/lib/notmidRuntime.ts
  )
}

expect_config_failure() {
  local expected="$1"
  shift

  if validate_api_config "$@" >"$LOG_FILE" 2>&1; then
    echo "Expected API production config to fail: ${expected}" >&2
    return 1
  fi

  if ! grep -q "$expected" "$LOG_FILE"; then
    echo "Expected API production config failure to mention: ${expected}" >&2
    cat "$LOG_FILE" >&2
    return 1
  fi
}

expect_web_config_failure() {
  local expected="$1"
  shift

  if validate_web_config "$@" >"$LOG_FILE" 2>&1; then
    echo "Expected Web production config to fail: ${expected}" >&2
    return 1
  fi

  if ! grep -q "$expected" "$LOG_FILE"; then
    echo "Expected Web production config failure to mention: ${expected}" >&2
    cat "$LOG_FILE" >&2
    return 1
  fi
}

expect_config_success() {
  if ! validate_api_config "$@" >"$LOG_FILE" 2>&1; then
    echo "Expected API production config to pass." >&2
    cat "$LOG_FILE" >&2
    return 1
  fi
}

expect_web_config_success() {
  if ! validate_web_config "$@" >"$LOG_FILE" 2>&1; then
    echo "Expected Web production config to pass." >&2
    cat "$LOG_FILE" >&2
    return 1
  fi
}

echo "== API production config dependencies =="
ensure_js_deps

echo "== API production config rejects local defaults =="
expect_config_failure \
  "NOTMID_WEB_ORIGIN is required" \
  NODE_ENV=production \
  NOTMID_AUTH_MODE=disabled \
  NOTMID_DATA_BACKEND=postgres \
  DATABASE_URL="$PROD_DATABASE_URL"

expect_config_failure \
  "must use https" \
  NODE_ENV=production \
  NOTMID_WEB_ORIGIN=http://localhost:3000 \
  NOTMID_AUTH_MODE=disabled \
  NOTMID_DATA_BACKEND=postgres \
  DATABASE_URL="$PROD_DATABASE_URL"

expect_config_failure \
  "fake is local-only" \
  NODE_ENV=production \
  NOTMID_WEB_ORIGIN=https://thdev.app \
  NOTMID_AUTH_MODE=fake \
  NOTMID_DATA_BACKEND=postgres \
  DATABASE_URL="$PROD_DATABASE_URL"

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
  "FIREBASE_PROJECT_ID is required" \
  NODE_ENV=production \
  NOTMID_WEB_ORIGIN=https://thdev.app \
  NOTMID_AUTH_MODE=firebase \
  NOTMID_DATA_BACKEND=postgres \
  DATABASE_URL="$PROD_DATABASE_URL"

expect_config_failure \
  "NOTMID_ENABLE_DIAGNOSTIC_FAILURE is local-only" \
  NODE_ENV=production \
  NOTMID_WEB_ORIGIN=https://thdev.app \
  NOTMID_AUTH_MODE=disabled \
  NOTMID_DATA_BACKEND=postgres \
  DATABASE_URL="$PROD_DATABASE_URL" \
  NOTMID_ENABLE_DIAGNOSTIC_FAILURE=true

expect_config_failure \
  "NOTMID_MUTATION_RATE_LIMIT_MAX must be a positive integer" \
  NODE_ENV=production \
  NOTMID_WEB_ORIGIN=https://thdev.app \
  NOTMID_AUTH_MODE=disabled \
  NOTMID_DATA_BACKEND=postgres \
  DATABASE_URL="$PROD_DATABASE_URL" \
  NOTMID_MUTATION_RATE_LIMIT_MAX=0

expect_config_failure \
  "NOTMID_MUTATION_RATE_LIMIT_WINDOW_MS must be a positive integer" \
  NODE_ENV=production \
  NOTMID_WEB_ORIGIN=https://thdev.app \
  NOTMID_AUTH_MODE=disabled \
  NOTMID_DATA_BACKEND=postgres \
  DATABASE_URL="$PROD_DATABASE_URL" \
  NOTMID_MUTATION_RATE_LIMIT_WINDOW_MS=not-a-number

echo "== API production config accepts safe production baselines =="
expect_config_success \
  NODE_ENV=production \
  NOTMID_WEB_ORIGIN=https://thdev.app \
  NOTMID_AUTH_MODE=disabled \
  NOTMID_DATA_BACKEND=postgres \
  DATABASE_URL="$PROD_DATABASE_URL"

expect_config_success \
  NODE_ENV=production \
  NOTMID_WEB_ORIGIN=https://thdev.app \
  NOTMID_AUTH_MODE=firebase \
  NOTMID_DATA_BACKEND=postgres \
  DATABASE_URL="$PROD_DATABASE_URL" \
  FIREBASE_PROJECT_ID=notmid-prod-placeholder \
  NOTMID_MUTATION_RATE_LIMIT_MAX=240 \
  NOTMID_MUTATION_RATE_LIMIT_WINDOW_MS=60000

echo "== Web production config rejects local defaults =="
expect_web_config_failure \
  "NOTMID_API_BASE_URL is required" \
  NODE_ENV=production

expect_web_config_failure \
  "must use https" \
  NODE_ENV=production \
  NOTMID_API_BASE_URL=http://localhost:8787

expect_web_config_failure \
  "NEXT_PUBLIC_NOTMID_GOOGLE_CLIENT_ID is required" \
  NODE_ENV=production \
  NOTMID_API_BASE_URL=https://thdev.app \
  NEXT_PUBLIC_NOTMID_AUTH_PROVIDER=firebase \
  NEXT_PUBLIC_NOTMID_FIREBASE_API_KEY=public-api-key-placeholder \
  NEXT_PUBLIC_NOTMID_FIREBASE_AUTH_DOMAIN=notmid-prod.firebaseapp.com \
  NEXT_PUBLIC_NOTMID_FIREBASE_PROJECT_ID=notmid-prod-placeholder \
  NEXT_PUBLIC_NOTMID_FIREBASE_APP_ID=1:123456789:web:abcdef

echo "== Web production config accepts safe production baselines =="
expect_web_config_success \
  NODE_ENV=production \
  NOTMID_API_BASE_URL=https://thdev.app \
  NEXT_PUBLIC_NOTMID_AUTH_PROVIDER=disabled

expect_web_config_success \
  NODE_ENV=production \
  NOTMID_API_BASE_URL=https://thdev.app \
  NEXT_PUBLIC_NOTMID_AUTH_PROVIDER=firebase \
  NEXT_PUBLIC_NOTMID_FIREBASE_API_KEY=public-api-key-placeholder \
  NEXT_PUBLIC_NOTMID_FIREBASE_AUTH_DOMAIN=notmid-prod.firebaseapp.com \
  NEXT_PUBLIC_NOTMID_FIREBASE_PROJECT_ID=notmid-prod-placeholder \
  NEXT_PUBLIC_NOTMID_FIREBASE_APP_ID=1:123456789:web:abcdef \
  NEXT_PUBLIC_NOTMID_GOOGLE_CLIENT_ID=public-google-client-id.apps.googleusercontent.com

echo "verify-web-api-production-config passed"
