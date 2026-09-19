# Service repository extraction

Status: local extraction completed on 2026-09-19. The user selected
`Notmid-web` and `Notmid-server` without importing old Git history.

The result is an Android repository plus separate Web and API repositories.
The boundary is HTTP, as specified in
[the target ARD](notmid-target-boundary-ard.md). Android SDK integration and
client adapters stay in Android; server handlers, authorization enforcement,
persistence, SQL migrations and web rendering move with the services.

## Independent boundaries

- [Pinned HTTP contract](../contracts/README.md): Android tests consume JSON,
  with no TypeScript import or running service.
- `scripts/verify-android.sh`: patch/secret checks, all Gradle tests and Debug
  assembly. Accepts Gradle arguments, for example `--offline`.
- `scripts/verify-local.sh` now invokes only Android verification.
- Android CI installs no Node/pnpm. Each service repository has its own CI and
  `scripts/verify-local.sh`; neither requires Java, the Android SDK or a sibling
  checkout. Release APK checks stay in Android CI.
- Notmid-server owns `packages/contracts` and the OpenAPI export. Notmid-web
  consumes a pinned source snapshot with provenance and exact inventory/hash
  checks in `contract-snapshot.json`. No filesystem imports cross repositories.

## Transfer inventory

| Moved to service repositories | Kept in Android repository |
| --- | --- |
| `apps/api` → Notmid-server; `apps/web` → Notmid-web | `app`, `core`, `feature`, `build-logic`, Gradle wrapper/config |
| canonical `packages/contracts` → server; snapshot and `packages/api-client` → web | pinned OpenAPI artifact and Kotlin adapters/tests |
| package manifest, pnpm workspace/lockfile, TypeScript base config | Android release and device smoke scripts |
| API/Postgres migration workflow and service CI job | Android CI job |
| API/Web verification, smoke and migration scripts | Android verification entrypoint |
| API-only database migration runbook | Android release contract |

Secret hygiene checks and ignore rules were copied to both repositories.
Mixed configuration checks were split by ownership; historical backend/Firebase
documents are qualified as reference material. Only tracked source and empty
templates were transferred. Runtime credentials, local environment, dependencies
and build outputs were excluded. Existing ignored runtime directories are
preserved locally under ignored `.tao` storage.

## Preservation and verification

The source baseline is `bf9ebdec614223bcacc6929da22dacce05fbd45f`.
Each destination has `extraction-source.json` with the original file hashes.
Eighty runtime, SQL and contract file instances were checked byte-for-byte
unchanged. Workspace metadata, lockfiles, scripts, CI and documentation were
adapted. Existing unfinished behavior and fixtures are preserved.

- Android verification passed after service removal: Gradle tests and Debug
  assembly, using only the pinned JSON contract for Kotlin contract tests.
- Web verification passed at its standalone location: snapshot integrity,
  typechecks, configuration/auth/write checks and Next.js production build.
- Server verification passed at its standalone location: typechecks, API
  checks, mock Postgres checks, OpenAPI comparison and local fixture HTTP smoke.
- Separate local web/server processes served the feed successfully over HTTP
  with the web fixture fallback disabled.

These checks do not prove live Firebase sign-in or production database behavior.
No remote was created, no code pushed, no production deployed and no real
migration run. Feature ViewModel and app shell decomposition remains the next
Android migration; service extraction does not complete that separate work.
