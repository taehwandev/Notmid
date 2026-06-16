# API Postgres Migration Workflow

This workflow is the production-facing operator path for applying the checked-in
Postgres schema migrations. It is intentionally separate from the normal CI gate.

## Workflow

```text
.github/workflows/api-postgres-migrations.yml
```

The workflow is `workflow_dispatch` only. It must not run on pull requests,
pushes, schedules, or tags.

Inputs:

- `target_environment`: `staging` or `production`.
- `mode`: `plan` or `apply`.
- `confirmation`: must be `apply-notmid-postgres-migrations` when `mode=apply`.

GitHub Environment requirements:

- Create `staging` and `production` environments before using `apply`.
- Store the environment-scoped secret as `NOTMID_DATABASE_URL`.
- Protect `production` with required reviewers before allowing deployments.
- Keep database URLs out of repository files, action logs, issues, and PR text.

## Plan

`plan` is safe for normal operator review:

```bash
bash scripts/migrate-api-postgres.sh --plan
```

It prints migration ids, labels, and checksums only. It does not read
`DATABASE_URL` and does not open a database connection.

## Apply

`apply` is allowed only through the manual workflow after environment approval,
or through an equivalent deployment system with the same controls:

```bash
NOTMID_MIGRATION_CONFIRM=apply DATABASE_URL='<server-side-postgres-url>' bash scripts/migrate-api-postgres.sh --apply
```

Required controls:

- plan output reviewed before apply
- environment-scoped `NOTMID_DATABASE_URL`
- `NOTMID_MIGRATION_CONFIRM=apply`
- one run per environment at a time
- backup or restore point confirmed outside this repository
- forward-fix path recorded for the release

## Verification

CI verifies the workflow contract without applying migrations:

```bash
bash scripts/verify-api-postgres-migration-workflow.sh
```

The verifier checks that the workflow is manual-only, read-only, environment
scoped, confirmation-gated, and wired to the guarded migration script.

## Rollback And Forward Fix

The first schema migration is additive and idempotent. It uses `CREATE TABLE IF
NOT EXISTS` and `CREATE INDEX IF NOT EXISTS`.

If apply fails before completion, rerun `plan`, inspect the migration ledger, and
prefer a forward fix. Do not add destructive rollback SQL unless a restore point
and production owner approval exist.
