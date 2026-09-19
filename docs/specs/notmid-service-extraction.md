# Service repository extraction

Status: preparation in progress. The destination repository and history policy
still need to be selected. No service source has been deleted or moved yet.

The accepted target is an Android repository plus a separate Web/API repository.
The boundary is HTTP, as specified in
[the target ARD](notmid-target-boundary-ard.md). Android SDK integration and
client adapters stay in Android; server handlers, authorization enforcement,
persistence, SQL migrations and web rendering move with the services.

## Prepared boundaries

- [Pinned HTTP contract](../contracts/README.md): Android tests consume JSON,
  with no TypeScript import or running service.
- `scripts/verify-android.sh`: patch/secret checks, all Gradle tests and Debug
  assembly. Accepts Gradle arguments, for example `--offline`.
- `scripts/verify-services.sh`: existing Web/API typechecks, API checks and Web
  build. It does not invoke Gradle.
- `scripts/verify-local.sh`: preserves the current combined local check by
  invoking the two scripts in sequence.
- CI has independent `android` and `services` jobs. The Android job installs no
  Node/pnpm; the service job installs no Java or Android SDK. Release APK checks
  stay in the Android job. The service job remains until extraction succeeds.

## Transfer inventory

| Move to service repository | Keep in Android repository |
| --- | --- |
| `apps/api`, `apps/web` | `app`, `core`, `feature`, `build-logic`, Gradle wrapper/config |
| `packages/contracts`, `packages/api-client` | pinned OpenAPI artifact and Kotlin adapters/tests |
| package manifest, pnpm workspace/lockfile, TypeScript base config | Android release and device smoke scripts |
| API/Postgres migration workflow and service CI job | Android CI job |
| API/Web verification, smoke and migration scripts | Android verification entrypoint |
| API-only database migration runbook | Android release contract |

Shared secret hygiene checks and ignore rules need copies in both repositories.
Split mixed product/backend/Firebase documentation and environment templates by
ownership. Copy only tracked source and empty templates: ignored runtime
credentials, local environment, dependencies and build outputs are not transfer
inputs. Preserve existing local metering integration responsibilities when
replacing any workflow entrypoint that contains them.

## Cutover acceptance

1. Choose destination and history policy; record the original source revision.
2. Materialize the service repository and compare every transferred source file
   against that revision before removing the Android-side copy.
3. Run service typechecks, checks and Web/API smoke in the new repository. Ensure
   no required script still reaches into this Android checkout.
4. Remove transferred service source/config and the service CI job from Android;
   make local verification Android-only and update documentation references.
5. Run Android tests and assembly without the transferred trees. Verify the
   Kotlin contract tests still consume only the pinned JSON artifact.
6. Commit each repository's reviewed change. Remote creation, push, production
   deployment, credentials and real database migrations are separate actions.

The destination choice does not block the prepared contract tests and build
separation. It does block claiming that external extraction is complete.
