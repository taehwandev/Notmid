#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

ADB="${ADB:-${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb}"
PACKAGE_NAME="${NOTMID_ANDROID_PACKAGE:-app.thdev.glassnavlab}"
LAUNCH_ACTIVITY="${NOTMID_ANDROID_ACTIVITY:-${PACKAGE_NAME}/.MainActivity}"
SCREENSHOT_DIR="${NOTMID_ANDROID_SMOKE_DIR:-/private/tmp}"
SCREENSHOT_PATH="${SCREENSHOT_DIR}/notmid-android-smoke.png"
DEVICE_SCREENSHOT="/sdcard/notmid-android-smoke.png"

if [[ ! -x "$ADB" ]]; then
  echo "ADB not found at ${ADB}. Set ANDROID_HOME or ADB." >&2
  exit 2
fi

device_count="$("$ADB" devices | awk 'NR > 1 && $2 == "device" { count++ } END { print count + 0 }')"
if [[ "$device_count" -eq 0 ]]; then
  echo "No Android device is connected. Start an emulator or connect an unlocked device." >&2
  exit 2
fi

if [[ "$device_count" -gt 1 && -z "${ANDROID_SERIAL:-}" ]]; then
  echo "Multiple Android devices are connected. Set ANDROID_SERIAL before running this smoke check." >&2
  exit 2
fi

mkdir -p "$SCREENSHOT_DIR"

echo "== Android debug APK =="
./gradlew :app:assembleDebug

echo "== Install debug APK =="
"$ADB" install -r app/build/outputs/apk/debug/app-debug.apk >/dev/null

echo "== Launch Notmid =="
"$ADB" shell am start -n "$LAUNCH_ACTIVITY" >/dev/null
sleep 3

window_state="$("$ADB" shell dumpsys window 2>/dev/null || true)"
current_focus="$(printf '%s\n' "$window_state" | grep 'mCurrentFocus=' || true)"
focused_app="$(printf '%s\n' "$window_state" | grep 'mFocusedApp=' || true)"

printf '%s\n' "$current_focus"
printf '%s\n' "$focused_app"

if [[ "$focused_app" != *"$PACKAGE_NAME"* ]]; then
  echo "Notmid is not the focused app after launch." >&2
  exit 3
fi

if [[ "$current_focus" != *"$PACKAGE_NAME"* ]]; then
  echo "Notmid launched, but the visible window is not the app." >&2
  echo "Unlock the device, close system overlays, and rerun this script." >&2
  exit 4
fi

echo "== Capture screen =="
"$ADB" shell screencap -p "$DEVICE_SCREENSHOT" >/dev/null
"$ADB" pull "$DEVICE_SCREENSHOT" "$SCREENSHOT_PATH" >/dev/null

echo "Android smoke passed"
echo "Screenshot: ${SCREENSHOT_PATH}"
