#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

WORKFLOW_FILE=".github/workflows/api-postgres-migrations.yml"

require_fragment() {
  local fragment="$1"

  if ! grep -Fq -- "$fragment" "$WORKFLOW_FILE"; then
    echo "Missing required migration workflow fragment: $fragment" >&2
    exit 1
  fi
}

reject_fragment() {
  local fragment="$1"

  if grep -Fq -- "$fragment" "$WORKFLOW_FILE"; then
    echo "Unsafe migration workflow fragment is present: $fragment" >&2
    exit 1
  fi
}

if [[ ! -f "$WORKFLOW_FILE" ]]; then
  echo "Missing $WORKFLOW_FILE" >&2
  exit 1
fi

echo "== API Postgres migration workflow trigger =="
require_fragment "workflow_dispatch:"
require_fragment "target_environment:"
require_fragment "mode:"
require_fragment "confirmation:"
require_fragment "- staging"
require_fragment "- production"
require_fragment "- plan"
require_fragment "- apply"
reject_fragment "pull_request:"
reject_fragment "branches:"

echo "== API Postgres migration workflow permissions and concurrency =="
require_fragment "permissions:"
require_fragment "contents: read"
require_fragment "concurrency:"
require_fragment "cancel-in-progress: false"
require_fragment 'environment: ${{ inputs.target_environment }}'

echo "== API Postgres migration workflow guardrails =="
require_fragment "Verify migration workflow contract"
require_fragment "bash scripts/verify-api-postgres-migration-workflow.sh"
require_fragment "bash scripts/migrate-api-postgres.sh --plan"
require_fragment "Require apply confirmation"
require_fragment 'CONFIRMATION: ${{ inputs.confirmation }}'
require_fragment "apply-notmid-postgres-migrations"
require_fragment "if: inputs.mode == 'apply'"
require_fragment '[[ "$CONFIRMATION" != "apply-notmid-postgres-migrations" ]]'
require_fragment 'DATABASE_URL: ${{ secrets.NOTMID_DATABASE_URL }}'
require_fragment "NOTMID_MIGRATION_CONFIRM: apply"
require_fragment "bash scripts/migrate-api-postgres.sh --apply"

echo "== API Postgres migration workflow local plan =="
bash scripts/migrate-api-postgres.sh --plan >/dev/null

echo "verify-api-postgres-migration-workflow passed"
