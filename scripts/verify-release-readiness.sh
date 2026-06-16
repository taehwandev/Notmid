#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

APK_SIGNER="${APK_SIGNER:-}"
RELEASE_APK="app/build/outputs/apk/release/app-release.apk"
UNSIGNED_RELEASE_APK="app/build/outputs/apk/release/app-release-unsigned.apk"
RELEASE_BUILDCONFIG="app/build/generated/source/buildConfig/release/app/thdev/glassnavlab/BuildConfig.java"
RELEASE_METADATA="app/build/outputs/apk/release/output-metadata.json"
status=0

metadata_value() {
  local key="$1"
  sed -nE "s/.*\"${key}\": ?\"?([^\",]+)\"?,?/\1/p" "$RELEASE_METADATA" | head -n 1
}

find_apksigner() {
  if [[ -n "$APK_SIGNER" ]]; then
    printf '%s\n' "$APK_SIGNER"
    return
  fi

  build_tools_dir="${ANDROID_HOME:-$HOME/Library/Android/sdk}/build-tools"
  if [[ ! -d "$build_tools_dir" ]]; then
    return
  fi

  find "$build_tools_dir" \
    -name apksigner \
    -type f \
    2>/dev/null |
    sort |
    tail -n 1
}

echo "== Release APK =="
./gradlew :app:assembleRelease

if [[ -f "$RELEASE_APK" ]]; then
  apk_path="$RELEASE_APK"
elif [[ -f "$UNSIGNED_RELEASE_APK" ]]; then
  apk_path="$UNSIGNED_RELEASE_APK"
else
  echo "Release APK was not produced." >&2
  exit 2
fi

echo "APK: ${apk_path}"

if [[ ! -f "$RELEASE_BUILDCONFIG" ]]; then
  echo "Release BuildConfig was not generated at ${RELEASE_BUILDCONFIG}." >&2
  status=1
elif grep -q 'http://10.0.2.2' "$RELEASE_BUILDCONFIG"; then
  echo "Release BuildConfig still points at the Android emulator host." >&2
  status=1
elif grep -q 'NOTMID_AUTH_MODE = "fake"' "$RELEASE_BUILDCONFIG"; then
  echo "Release BuildConfig still packages local fake auth mode." >&2
  status=1
elif ! grep -Eq 'NOTMID_AUTH_MODE = "(firebase|disabled)"' "$RELEASE_BUILDCONFIG"; then
  echo "Release auth mode must be firebase or disabled." >&2
  status=1
elif grep -q 'NOTMID_AUTH_MODE = "firebase"' "$RELEASE_BUILDCONFIG" &&
  grep -q 'NOTMID_FIREBASE_API_KEY = ""' "$RELEASE_BUILDCONFIG"; then
  echo "Release Firebase auth mode requires a public Firebase API key." >&2
  status=1
elif grep -q 'NOTMID_AUTH_MODE = "firebase"' "$RELEASE_BUILDCONFIG" &&
  grep -q 'NOTMID_GOOGLE_SERVER_CLIENT_ID = ""' "$RELEASE_BUILDCONFIG"; then
  echo "Release Firebase auth mode requires a Google OAuth web client ID for Android Google sign-in." >&2
  status=1
elif grep -q 'NOTMID_CONTENT_SOURCE = "static"' "$RELEASE_BUILDCONFIG"; then
  echo "Release BuildConfig still packages static fixture content." >&2
  status=1
elif ! grep -q 'NOTMID_CONTENT_SOURCE = "api"' "$RELEASE_BUILDCONFIG"; then
  echo "Release content source must be api." >&2
  status=1
else
  echo "Release API base URL does not use the emulator host."
  echo "Release auth mode does not use local fake auth."
  echo "Release content source uses the API boundary."
  echo "Release Firebase auth config has required public client identifiers when enabled."
fi

apksigner_path="$(find_apksigner)"
if [[ -z "$apksigner_path" || ! -x "$apksigner_path" ]]; then
  echo "apksigner was not found; cannot verify release signing." >&2
  status=1
elif "$apksigner_path" verify --verbose "$apk_path" >/dev/null 2>&1; then
  echo "Release APK signature verified."
else
  echo "Release APK is not signed for distribution." >&2
  status=1
fi

if [[ ! -f "$RELEASE_METADATA" ]]; then
  echo "Release metadata was not generated at ${RELEASE_METADATA}." >&2
  status=1
else
  version_code="$(metadata_value versionCode)"
  version_name="$(metadata_value versionName)"

  if [[ -z "$version_code" || -z "$version_name" ]]; then
    echo "Release metadata is missing versionCode or versionName." >&2
    status=1
  elif [[ "$version_code" == "1" && "$version_name" == "1.0" ]]; then
    echo "Release version still uses the local baseline 1 / 1.0." >&2
    status=1
  elif [[ ! "$version_name" =~ ^([0-9]{2})\.([0-9]{2})\.([0-9]+)(-rc\.[0-9]+)?$ ]]; then
    echo "Release versionName must follow monthly CalVer YY.MM.N or YY.MM.N-rc.N: ${version_name}" >&2
    status=1
  else
    version_year="${BASH_REMATCH[1]}"
    version_month="${BASH_REMATCH[2]}"
    version_number="${BASH_REMATCH[3]}"

    if [[ ! "$version_code" =~ ^[0-9]{8}$ ]]; then
      echo "Release versionCode must follow YYMMNNBB as an 8-digit integer: ${version_code}" >&2
      status=1
      version_code="00000000"
    fi

    expected_code_prefix="$(
      printf '%02d%02d%02d' \
        "$((10#$version_year))" \
        "$((10#$version_month))" \
        "$((10#$version_number))"
    )"

    if (( 10#$version_month < 1 || 10#$version_month > 12 )); then
      echo "Release versionName month must be 01-12: ${version_name}" >&2
      status=1
    elif (( 10#$version_number < 1 || 10#$version_number > 99 )); then
      echo "Release versionName sequence must fit YYMMNNBB versionCode: ${version_name}" >&2
      status=1
    elif [[ "${version_code:0:6}" != "$expected_code_prefix" ]]; then
      echo "Release versionCode ${version_code} does not match versionName ${version_name}." >&2
      status=1
    elif (( 10#${version_code:6:2} < 1 )); then
      echo "Release versionCode build suffix must be at least 01: ${version_code}" >&2
      status=1
    else
      echo "Release version matches the documented Android release contract."
    fi
  fi
fi

if [[ "$status" -ne 0 ]]; then
  echo "Release readiness failed. Provide signing, version, and environment values before distribution." >&2
  exit "$status"
fi

echo "Release readiness passed."
