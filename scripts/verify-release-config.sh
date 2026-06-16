#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

RELEASE_APK="app/build/outputs/apk/release/app-release.apk"
UNSIGNED_RELEASE_APK="app/build/outputs/apk/release/app-release-unsigned.apk"
RELEASE_BUILDCONFIG="app/build/generated/source/buildConfig/release/app/thdev/glassnavlab/BuildConfig.java"
RELEASE_METADATA="app/build/outputs/apk/release/output-metadata.json"

echo "== Release config APK =="
./gradlew :app:assembleRelease

if [[ -f "$RELEASE_APK" ]]; then
  echo "APK: ${RELEASE_APK}"
elif [[ -f "$UNSIGNED_RELEASE_APK" ]]; then
  echo "APK: ${UNSIGNED_RELEASE_APK}"
else
  echo "Release APK was not produced." >&2
  exit 2
fi

if [[ ! -f "$RELEASE_BUILDCONFIG" ]]; then
  echo "Release BuildConfig was not generated at ${RELEASE_BUILDCONFIG}." >&2
  exit 1
fi

if grep -q 'http://10.0.2.2' "$RELEASE_BUILDCONFIG"; then
  echo "Release BuildConfig still points at the Android emulator host." >&2
  exit 1
fi

if ! grep -q 'NOTMID_API_BASE_URL = "https://' "$RELEASE_BUILDCONFIG"; then
  echo "Release API base URL must be HTTPS." >&2
  exit 1
fi

if ! grep -q 'NOTMID_BUILD_CHANNEL = "release"' "$RELEASE_BUILDCONFIG"; then
  echo "Release BuildConfig does not identify the release channel." >&2
  exit 1
fi

if grep -q 'NOTMID_AUTH_MODE = "fake"' "$RELEASE_BUILDCONFIG"; then
  echo "Release BuildConfig must not package local fake auth mode." >&2
  exit 1
fi

if ! grep -Eq 'NOTMID_AUTH_MODE = "(firebase|disabled)"' "$RELEASE_BUILDCONFIG"; then
  echo "Release auth mode must be firebase or disabled." >&2
  exit 1
fi

if grep -q 'NOTMID_AUTH_MODE = "firebase"' "$RELEASE_BUILDCONFIG" &&
  grep -q 'NOTMID_FIREBASE_API_KEY = ""' "$RELEASE_BUILDCONFIG"; then
  echo "Release Firebase auth mode requires a public Firebase API key." >&2
  exit 1
fi

if grep -q 'NOTMID_AUTH_MODE = "firebase"' "$RELEASE_BUILDCONFIG" &&
  grep -q 'NOTMID_GOOGLE_SERVER_CLIENT_ID = ""' "$RELEASE_BUILDCONFIG"; then
  echo "Release Firebase auth mode requires a Google OAuth web client ID for Android Google sign-in." >&2
  exit 1
fi

if grep -q 'NOTMID_CONTENT_SOURCE = "static"' "$RELEASE_BUILDCONFIG"; then
  echo "Release BuildConfig must not package static fixture content." >&2
  exit 1
fi

if ! grep -q 'NOTMID_CONTENT_SOURCE = "api"' "$RELEASE_BUILDCONFIG"; then
  echo "Release content source must be api." >&2
  exit 1
fi

if [[ ! -f "$RELEASE_METADATA" ]]; then
  echo "Release metadata was not generated at ${RELEASE_METADATA}." >&2
  exit 1
fi

echo "Release config gate passed."
