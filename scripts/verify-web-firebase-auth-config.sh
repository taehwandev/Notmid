#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

LOG_FILE="${TMPDIR:-/tmp}/notmid-web-firebase-auth-config.log"

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

validate_web_config() {
  (
    cd apps/web
    env NOTMID_VALIDATE_WEB_CONFIG_ONLY=true "$@" ../api/node_modules/.bin/tsx src/lib/notmidRuntime.ts
  )
}

expect_web_config_failure() {
  local expected="$1"
  shift

  if validate_web_config "$@" >"$LOG_FILE" 2>&1; then
    echo "Expected Web config to fail: ${expected}" >&2
    return 1
  fi

  if ! grep -q "$expected" "$LOG_FILE"; then
    echo "Expected Web config failure to mention: ${expected}" >&2
    cat "$LOG_FILE" >&2
    return 1
  fi
}

expect_web_config_success() {
  if ! validate_web_config "$@" >"$LOG_FILE" 2>&1; then
    echo "Expected Web config to pass." >&2
    cat "$LOG_FILE" >&2
    return 1
  fi
}

echo "== Web Firebase auth dependencies =="
ensure_js_deps

echo "== Web Firebase auth config rejects unsafe production auth =="
expect_web_config_failure \
  "NEXT_PUBLIC_NOTMID_AUTH_PROVIDER is required" \
  NODE_ENV=production \
  NOTMID_API_BASE_URL=https://thdev.app

expect_web_config_failure \
  "fake is local-only" \
  NODE_ENV=production \
  NOTMID_API_BASE_URL=https://thdev.app \
  NEXT_PUBLIC_NOTMID_AUTH_PROVIDER=fake

expect_web_config_failure \
  "NEXT_PUBLIC_NOTMID_FIREBASE_API_KEY is required" \
  NODE_ENV=production \
  NOTMID_API_BASE_URL=https://thdev.app \
  NEXT_PUBLIC_NOTMID_AUTH_PROVIDER=firebase

expect_web_config_failure \
  "NEXT_PUBLIC_NOTMID_GOOGLE_CLIENT_ID is required" \
  NODE_ENV=production \
  NOTMID_API_BASE_URL=https://thdev.app \
  NEXT_PUBLIC_NOTMID_AUTH_PROVIDER=firebase \
  NEXT_PUBLIC_NOTMID_FIREBASE_API_KEY=public-api-key-placeholder \
  NEXT_PUBLIC_NOTMID_FIREBASE_AUTH_DOMAIN=notmid-prod.firebaseapp.com \
  NEXT_PUBLIC_NOTMID_FIREBASE_PROJECT_ID=notmid-prod-placeholder \
  NEXT_PUBLIC_NOTMID_FIREBASE_APP_ID=1:123456789:web:abcdef

expect_web_config_failure \
  "Client-visible Firebase config must not include private values" \
  NODE_ENV=production \
  NOTMID_API_BASE_URL=https://thdev.app \
  NEXT_PUBLIC_NOTMID_AUTH_PROVIDER=firebase \
  NEXT_PUBLIC_NOTMID_FIREBASE_API_KEY=public-api-key-placeholder \
  NEXT_PUBLIC_NOTMID_FIREBASE_AUTH_DOMAIN=notmid-prod.firebaseapp.com \
  NEXT_PUBLIC_NOTMID_FIREBASE_PROJECT_ID=notmid-prod-placeholder \
  NEXT_PUBLIC_NOTMID_FIREBASE_APP_ID=1:123456789:web:abcdef \
  NEXT_PUBLIC_NOTMID_GOOGLE_CLIENT_ID=public-google-client-id.apps.googleusercontent.com \
  NEXT_PUBLIC_NOTMID_FIREBASE_PRIVATE_KEY=do-not-commit

expect_web_config_failure \
  "NEXT_PUBLIC_NOTMID_GOOGLE_CLIENT_SECRET" \
  NODE_ENV=production \
  NOTMID_API_BASE_URL=https://thdev.app \
  NEXT_PUBLIC_NOTMID_AUTH_PROVIDER=firebase \
  NEXT_PUBLIC_NOTMID_FIREBASE_API_KEY=public-api-key-placeholder \
  NEXT_PUBLIC_NOTMID_FIREBASE_AUTH_DOMAIN=notmid-prod.firebaseapp.com \
  NEXT_PUBLIC_NOTMID_FIREBASE_PROJECT_ID=notmid-prod-placeholder \
  NEXT_PUBLIC_NOTMID_FIREBASE_APP_ID=1:123456789:web:abcdef \
  NEXT_PUBLIC_NOTMID_GOOGLE_CLIENT_ID=public-google-client-id.apps.googleusercontent.com \
  NEXT_PUBLIC_NOTMID_GOOGLE_CLIENT_SECRET=do-not-commit

echo "== Web Firebase auth config accepts explicit safe modes =="
expect_web_config_success \
  NODE_ENV=development \
  NOTMID_API_BASE_URL=http://localhost:8787

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

echo "== Web Firebase auth session bridge static checks =="
rg -q "identitytoolkit.googleapis.com/v1/accounts:signUp" apps/web/src/lib/notmidFirebaseClient.ts
rg -q "returnSecureToken: true" apps/web/src/lib/notmidFirebaseClient.ts
rg -q "refreshToken" apps/web/src/lib/notmidFirebaseClient.ts
rg -q "accounts.google.com/gsi/client" apps/web/src/app/notmid/login/FirebaseLoginActions.tsx
rg -q "accounts\\?\\.id" apps/web/src/app/notmid/login/FirebaseLoginActions.tsx
rg -q "renderButton" apps/web/src/app/notmid/login/FirebaseLoginActions.tsx
rg -q "credential" apps/web/src/app/notmid/login/FirebaseLoginActions.tsx
rg -q "firebase-session/google" apps/web/src/app/notmid/login/FirebaseLoginActions.tsx
rg -q "identitytoolkit.googleapis.com/v1/accounts:signInWithIdp" \
  apps/web/src/lib/notmidFirebaseSession.ts
rg -q "providerId: \"google.com\"" apps/web/src/lib/notmidFirebaseSession.ts
rg -q "existingFirebaseIdToken" apps/web/src/lib/notmidFirebaseSession.ts
rg -q "exchangeGoogleIdTokenForNotmidFirebaseSession" \
  apps/web/src/app/notmid/login/firebase-session/google/route.ts
rg -q "request.cookies.get\\(notmidAuthCookieName\\)" \
  apps/web/src/app/notmid/login/firebase-session/google/route.ts
rg -q "getAuthStatus\\(firebaseSession.idToken\\)" \
  apps/web/src/app/notmid/login/firebase-session/google/route.ts
rg -q "getAuthStatus\\(idToken\\)" apps/web/src/app/notmid/login/firebase-session/route.ts
rg -q "httpOnly: true" apps/web/src/lib/notmidFirebaseSession.ts
rg -q "securetoken.googleapis.com/v1/token" apps/web/src/lib/notmidFirebaseSession.ts
rg -q "grant_type: \"refresh_token\"" apps/web/src/lib/notmidFirebaseSession.ts
rg -q "notmid_refresh_token" apps/web/src/lib/notmidAuthCookies.ts
rg -q "getAuthStatus\\(refreshedSession.idToken\\)" \
  apps/web/src/app/notmid/login/firebase-session/refresh/route.ts
rg -q "clearNotmidFirebaseSessionCookies" \
  apps/web/src/app/notmid/login/firebase-session/refresh/route.ts
rg -q "refreshNotmidFirebaseIdToken" apps/web/src/middleware.ts
rg -q "verifyRefreshedNotmidSession" apps/web/src/middleware.ts
rg -q "setNotmidFirebaseSessionCookies" apps/web/src/middleware.ts
rg -q "requestCookieHeaderWithAccessToken" apps/web/src/middleware.ts
rg -q "notmid_access_token" apps/web/src/lib/notmidAuthCookies.ts
rg -q "signOutFromNotmid" apps/web/src/app/notmid/profile/settings/page.tsx
rg -q "notmidAuthRefreshCookieName" apps/web/src/app/notmid/profile/settings/page.tsx
rg -q "notmidLegacyFakeAuthCookieName" apps/web/src/app/notmid/profile/settings/page.tsx
rg -q "maxAge: 0" apps/web/src/app/notmid/profile/settings/page.tsx

if rg -q "localStorage|sessionStorage|browserLocalPersistence" \
  apps/web/src/lib/notmidFirebaseClient.ts \
  apps/web/src/lib/notmidFirebaseSession.ts \
  apps/web/src/middleware.ts \
  apps/web/src/app/notmid/login/FirebaseLoginActions.tsx \
  apps/web/src/app/notmid/login/firebase-session; then
  echo "Firebase web auth must not store tokens in browser storage." >&2
  exit 1
fi

echo "verify-web-firebase-auth-config passed"
