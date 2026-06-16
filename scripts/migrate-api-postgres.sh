#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

MODE="${1:---plan}"

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

case "$MODE" in
  --plan)
    ;;
  --apply)
    if [[ -z "${DATABASE_URL:-}" ]]; then
      echo "DATABASE_URL is required to apply Postgres migrations." >&2
      exit 1
    fi

    if [[ "${NOTMID_MIGRATION_CONFIRM:-}" != "apply" ]]; then
      echo "Refusing to apply migrations without NOTMID_MIGRATION_CONFIRM=apply." >&2
      exit 1
    fi
    ;;
  *)
    echo "Usage: bash scripts/migrate-api-postgres.sh [--plan|--apply]" >&2
    exit 1
    ;;
esac

ensure_js_deps

(
  cd apps/api
  node --import tsx src/runPostgresMigrations.ts "$MODE"
)
