#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

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

echo "== Patch hygiene =="
git diff --check

echo "== Secret hygiene =="
bash scripts/verify-secret-hygiene.sh

echo "== Android tests =="
./gradlew test

echo "== Android debug APK =="
./gradlew :app:assembleDebug

echo "== Web/API dependencies =="
ensure_js_deps

echo "== Web/API typecheck =="
packages/contracts/node_modules/.bin/tsc -p packages/contracts/tsconfig.json --noEmit
packages/api-client/node_modules/.bin/tsc -p packages/api-client/tsconfig.json --noEmit
apps/api/node_modules/.bin/tsc -p apps/api/tsconfig.json --noEmit
apps/web/node_modules/.bin/tsc -p apps/web/tsconfig.json --noEmit

echo "== API build =="
apps/api/node_modules/.bin/tsc -p apps/api/tsconfig.json --noEmit

echo "== OpenAPI contract =="
bash scripts/verify-openapi-contract.sh

echo "== API auth policy =="
bash scripts/verify-api-auth-policy.sh

echo "== API Firebase auth =="
bash scripts/verify-api-firebase-auth.sh

echo "== API repository boundary =="
bash scripts/verify-api-repository-boundary.sh

echo "== API Postgres repository adapter =="
bash scripts/verify-api-postgres-repository.sh

echo "== API Postgres runtime wiring =="
bash scripts/verify-api-postgres-runtime.sh

echo "== API Postgres migration workflow =="
bash scripts/verify-api-postgres-migrations.sh

echo "== API Postgres migration deployment workflow =="
bash scripts/verify-api-postgres-migration-workflow.sh

echo "== API persistence config =="
bash scripts/verify-api-persistence-config.sh

echo "== API diagnostics =="
bash scripts/verify-api-diagnostics.sh

echo "== API audit logs =="
bash scripts/verify-api-audit-logs.sh

echo "== API rate limit =="
bash scripts/verify-api-rate-limit.sh

echo "== Web/API production config =="
bash scripts/verify-web-api-production-config.sh

echo "== Web Firebase auth config =="
bash scripts/verify-web-firebase-auth-config.sh

echo "== Web write actions =="
bash scripts/verify-web-write-actions.sh

echo "== Web build =="
(
  cd apps/web
  node_modules/.bin/next build
)

echo "verify-local passed"
