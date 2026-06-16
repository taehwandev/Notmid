# notmid

`notmid` is an open-source Android and web reference product for short video place discovery, map context, and place-aware chat.

The product direction is:

```text
not mid. show receipts.
```

This repository is intentionally shaped like a real service instead of a single Android sample. Android, web, server, shared contracts, product docs, and agent-facing project memory live together so URL contracts, API contracts, and feature behavior can evolve in one place.

## Repository Shape

```text
app/                 Android entry point
core/                Android core modules
feature/             Android feature api/impl modules
build-logic/         Android Gradle convention plugins

apps/
  api/               TypeScript API server
  web/               React/Next.js web app

packages/
  contracts/         shared product routes, DTOs, fake fixtures
  api-client/        typed web/server client wrapper

docs/                product and architecture specs
llm-wiki/            short task-oriented project memory for agents
.agents/skills/      repo-local agent skills
```

## Android

The Android app is Jetpack Compose based and keeps reusable UI inside `:core:designsystem`. Feature modules are split into `api` and `impl` where the boundary is useful. Cross-feature communication goes through route/event contracts instead of implementation dependencies.

The Liquid Glass bottom navigation reference still lives in this repository, now under the notmid design system:

```text
core/designsystem/src/main/java/app/thdev/glassnavlab/core/designsystem/component/liquidglass
```

Run Android:

```bash
./gradlew :app:installDebug
```

Verify Android modules:

```bash
./gradlew :app:compileDebugKotlin
./gradlew test
```

Run the full local verification gate:

```bash
bash scripts/verify-local.sh
```

Check public-repo secret hygiene before sharing or publishing changes:

```bash
bash scripts/verify-secret-hygiene.sh
```

Local Android configuration starts from the checked-in template:

```bash
cp local.properties.example local.properties
```

Debug Android builds default to `NOTMID_DEBUG_CONTENT_SOURCE=api` and read from
the local fixture API at `http://10.0.2.2:8787` on the emulator. Override
`NOTMID_DEBUG_CONTENT_SOURCE=static` only when intentionally running without the
API server.

Keep real API keys, map tokens, and signing values in ignored local or CI
configuration only.

Run device visual smoke on an unlocked device or emulator:

```bash
bash scripts/verify-android-smoke.sh
```

Check whether the release artifact is actually distributable:

```bash
bash scripts/verify-release-readiness.sh
```

That readiness gate must fail until release signing, non-baseline version values,
and the intended release API environment are supplied.

Android release versioning, signing inputs, tagging, and rollout gates are
defined in `docs/release/android-release-contract.md`.

CI runs the secret hygiene gate, local verification gate, Web/API smoke, and
release configuration gate. The release configuration gate checks that the
release variant assembles with a release channel and HTTPS API URL, but it does
not replace full release readiness because CI pull requests do not receive
signing secrets.

## Web And API

The web/API side is a separate pnpm workspace inside the same git repository. It does not participate in Gradle builds.

Run the API server:

```bash
pnpm install
pnpm api:dev
```

Run the web app:

```bash
pnpm web:dev
```

Local defaults:

```text
API: http://localhost:8787
Web: http://localhost:3000/notmid
```

The web app should open directly into the product shell, not a marketing landing page.

Smoke test web/API locally:

```bash
bash scripts/smoke-web-api.sh
```

Verify that the API serves the committed OpenAPI contract:

```bash
bash scripts/verify-openapi-contract.sh
```

Verify that local fake auth is accepted only in fake mode:

```bash
bash scripts/verify-api-auth-policy.sh
```

Verify that Firebase mode validates signed Firebase ID token shape, signature,
issuer, audience, expiry, and provider mapping without committing secrets:

```bash
bash scripts/verify-api-firebase-auth.sh
```

Verify that the web app can be configured for Firebase Auth without committing
secrets or allowing fake auth in production:

```bash
bash scripts/verify-web-firebase-auth-config.sh
```

Web Firebase login uses public `NEXT_PUBLIC_NOTMID_FIREBASE_*` values plus a
public `NEXT_PUBLIC_NOTMID_GOOGLE_CLIENT_ID`. The browser can obtain an
anonymous Firebase ID token or a Google Identity Services ID token. Google
tokens are exchanged for Firebase sessions through the Next.js session bridge,
which links to the current anonymous Firebase session when possible, asks the
API to verify the Firebase ID token, and only then stores the short-lived token
in an HTTP-only `/notmid` cookie. The Firebase refresh token is kept in a
separate HTTP-only `/notmid` cookie and can be exchanged by the server refresh
route; the `/notmid` middleware refreshes an expired or near-expired ID token
before server-rendered product routes continue. Tokens are not written to
browser storage. Profile settings includes a server-side sign-out action that
clears the Firebase ID-token cookie, Firebase refresh-token cookie, and legacy
local fake session cookie. Capture publish, clip save, profile settings update,
and chat send forms use server actions that call the existing API write
endpoints with the HTTP-only session token, retry once after a server-side
Firebase refresh on stale sessions, and avoid browser token storage.

Verify protected web write action wiring without real Firebase credentials:

```bash
bash scripts/verify-web-write-actions.sh
```

Verify that API routes go through the server-side repository boundary:

```bash
bash scripts/verify-api-repository-boundary.sh
```

The Web/API smoke check also exercises protected capture publish, clip save,
profile settings update, and chat send API writes, verifying unauthenticated
requests fail and local fake auth can complete the write path:

```bash
bash scripts/smoke-web-api.sh
```

Verify the CI-safe Postgres repository adapter with a fake query client:

```bash
bash scripts/verify-api-postgres-repository.sh
```

Verify that the Postgres runtime backend is wired to the API without requiring a
local database during CI:

```bash
bash scripts/verify-api-postgres-runtime.sh
```

Verify the explicit Postgres migration workflow without applying it to a real
database:

```bash
bash scripts/verify-api-postgres-migrations.sh
```

Verify that the manual GitHub Actions migration workflow stays environment
scoped, confirmation gated, and CI-safe:

```bash
bash scripts/verify-api-postgres-migration-workflow.sh
```

Preview the migration plan:

```bash
bash scripts/migrate-api-postgres.sh --plan
```

Apply migrations only from an approved deploy or migration run with a server-side
database URL:

```bash
NOTMID_MIGRATION_CONFIRM=apply DATABASE_URL='<server-side-postgres-url>' bash scripts/migrate-api-postgres.sh --apply
```

The GitHub Actions runbook is in
`docs/release/api-postgres-migration-workflow.md`.

Verify that production API config cannot run with fixture storage and that the
checked-in Postgres schema stays non-destructive:

```bash
bash scripts/verify-api-persistence-config.sh
```

Verify that API errors include request ids without leaking raw exceptions:

```bash
bash scripts/verify-api-diagnostics.sh
```

Verify that protected API writes produce privacy-safe audit events without
logging bearer tokens or request body values:

```bash
bash scripts/verify-api-audit-logs.sh
```

Verify that mutating API routes have a CI-safe abuse boundary:

```bash
bash scripts/verify-api-rate-limit.sh
```

Check that API production runtime config cannot accidentally use local fake auth
or localhost origins:

```bash
bash scripts/verify-web-api-production-config.sh
```

For production web auth, set `NEXT_PUBLIC_NOTMID_AUTH_PROVIDER=firebase` plus
the public Firebase web app config values and
`NEXT_PUBLIC_NOTMID_GOOGLE_CLIENT_ID` in deployment config. Use
`NEXT_PUBLIC_NOTMID_AUTH_PROVIDER=disabled` only for a browse-only deployment;
`fake` is local-only.

For Android release auth, set `NOTMID_RELEASE_AUTH_MODE=firebase` with
`NOTMID_RELEASE_FIREBASE_API_KEY` and
`NOTMID_RELEASE_GOOGLE_SERVER_CLIENT_ID` in ignored local config or CI secret
storage. The Android Google client id is a public OAuth web client id used by
Credential Manager; it is not an OAuth client secret.

## Backend Direction

notmid is server-first:

```text
Android / Web
  -> notmid API Server
      -> Postgres / Redis / Object Storage
      -> Firebase Admin / FCM / App Check when useful
```

Firebase is an auxiliary platform, not the primary product database contract. It can support identity, push notifications, app integrity, crash reporting, analytics, emulator-based tests, or static/web hosting. Production secrets and service account keys must never be committed.

## Deep Links

Web links are product contracts. The same URL should work on web and resolve into an ordered Android route stack.

Examples:

```text
https://thdev.app/notmid
https://thdev.app/notmid/clips/{clipId}
https://thdev.app/notmid/places/{placeId}
https://thdev.app/notmid/profile/settings
```

## Open Source Safety

Never commit:

```text
.env
google-services.json
Firebase Admin SDK JSON
service account JSON
App Check debug tokens
keystores
production API secrets
```

Commit example templates only, such as `.env.example` files with placeholder values.
