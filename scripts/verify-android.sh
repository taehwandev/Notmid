#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "== Patch hygiene =="
git diff --check

echo "== Secret hygiene =="
bash scripts/verify-secret-hygiene.sh

echo "== Android tests and debug APK =="
./gradlew test :app:assembleDebug "$@"

echo "verify-android passed"
