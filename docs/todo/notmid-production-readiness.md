# notmid Production Readiness Notes

This note tracks the current baseline evidence before treating the active patch
as release-ready. It is intentionally separate from the product specs so the
specs stay focused on product behavior.

## Current Evidence

- Android compile, JVM tests, and debug APK assembly pass through
  `bash scripts/verify-local.sh`.
- Web/API typecheck, API build, Next.js production build, and route smoke pass
  through `bash scripts/verify-local.sh` and `bash scripts/smoke-web-api.sh`.
- The API serves the shared contract package OpenAPI document at
  `/openapi.json`; `bash scripts/verify-openapi-contract.sh` verifies required
  paths, schemas, and operation ids.
- API auth policy is centralized in `apps/api/src/authPolicy.ts`; `bash
  scripts/verify-api-auth-policy.sh` verifies the deterministic fake token is
  accepted only in `NOTMID_AUTH_MODE=fake` and rejected for protected writes in
  `disabled` and `firebase` modes.
- Firebase auth mode now has a server-side SecureToken verification boundary in
  `apps/api/src/firebaseTokenVerifier.ts`. `bash
  scripts/verify-api-firebase-auth.sh` verifies RS256 signature validation,
  issuer/audience/expiry enforcement, provider mapping, protected-write auth
  context, and fake-token rejection without committing Firebase credentials.
- Web Firebase Auth now has anonymous and Google provider paths. Anonymous
  sign-in still uses `apps/web/src/lib/notmidFirebaseClient.ts`, while Google
  sign-in uses Google Identity Services in
  `apps/web/src/app/notmid/login/FirebaseLoginActions.tsx` and exchanges the
  Google ID token through
  `apps/web/src/app/notmid/login/firebase-session/google/route.ts`. That route
  links to the current anonymous Firebase session when possible, asks the API to
  verify the Firebase ID token, and only then sets HTTP-only session cookies.
  `bash scripts/verify-web-firebase-auth-config.sh` verifies that production web
  auth rejects fake mode, requires public Firebase config and a public Google
  client id for Firebase mode, rejects client-visible private Firebase/Google
  values, avoids browser token storage, sets the `/notmid` ID-token cookie only
  after API verification, and keeps the refresh token in an HTTP-only cookie for
  server-side exchange.
- Web protected write forms now route through server actions in
  `apps/web/src/app/notmid/capture/page.tsx` and
  `apps/web/src/app/notmid/chats/[threadId]/page.tsx` and
  `apps/web/src/app/notmid/profile/settings/page.tsx`, with shared clip save
  wiring in `apps/web/src/lib/notmidClipActions.ts`. `bash
  scripts/verify-web-write-actions.sh` verifies capture publish, clip save,
  chat send, profile settings update, typed API errors, and action-level refresh
  retry wiring without requiring real Firebase credentials. `bash
  scripts/smoke-web-api.sh` also verifies protected capture publish, clip save,
  chat send, and profile settings API writes reject unauthenticated requests and
  accept the local fake auth token in fake mode.
- API product reads and local write simulations now go through
  `apps/api/src/notmidRepository.ts`, keeping Hono route handlers focused on
  transport/auth/status mapping while leaving a replacement boundary for future
  Postgres/Object Storage persistence. `bash
  scripts/verify-api-repository-boundary.sh` verifies read, write, validation,
  and not-found behavior through that boundary.
- API Postgres persistence now has a CI-safe adapter in
  `apps/api/src/postgresNotmidRepository.ts` behind a query-port abstraction.
  `bash scripts/verify-api-postgres-repository.sh` verifies feed/map/detail
  reads, authenticated write actor mapping, validation failures, parameterized
  SQL, and thread aggregation using a fake query client without provisioning a
  database or running migrations.
- API Postgres runtime wiring now uses `apps/api/src/postgresQueryClient.ts` to
  connect `NOTMID_DATA_BACKEND=postgres` to a Postgres.js query client.
  `bash scripts/verify-api-postgres-runtime.sh` verifies the wrapper and confirms
  the API can start and serve health in Postgres mode without opening a database
  connection during CI.
- API Postgres migrations now have an explicit guarded workflow in
  `apps/api/src/postgresMigrations.ts`, `apps/api/src/runPostgresMigrations.ts`,
  and `scripts/migrate-api-postgres.sh`. `bash
  scripts/verify-api-postgres-migrations.sh` verifies migration planning, apply
  guards, statement splitting, checksum drift detection, fake-client apply, and
  idempotent skip behavior without applying SQL to a real database.
- API Postgres migrations now also have a manual GitHub Actions operator path in
  `.github/workflows/api-postgres-migrations.yml`. `bash
  scripts/verify-api-postgres-migration-workflow.sh` verifies that it is
  workflow-dispatch only, read-only, environment-scoped, confirmation-gated, and
  wired to the guarded migration script. The runbook lives in
  `docs/release/api-postgres-migration-workflow.md`.
- API production storage/runtime config now rejects fixture storage in production.
  `NOTMID_DATA_BACKEND=fixture` remains the local default, while
  `NODE_ENV=production` requires `NOTMID_DATA_BACKEND=postgres` and
  `DATABASE_URL`. Config validation lives in `apps/api/src/runtimeConfig.ts`
  instead of the server routing entrypoint. `apps/api/db/postgres/schema.sql`
  and additive migration files such as `0002_chat_thread_access.sql` and
  `0003_user_relationships.sql` are the checked-in schema source artifacts, and `bash
  scripts/verify-api-persistence-config.sh` verifies production storage config
  plus non-destructive schema structure without running migrations.
- API responses now carry an `x-request-id` header, health includes the active
  request id, and stable `route_not_found` / `internal_error` responses include
  request ids without exposing raw exception messages. `bash
  scripts/verify-api-diagnostics.sh` verifies request-id propagation, baseline
  API security headers (`x-content-type-options`, `referrer-policy`,
  `x-frame-options`), 404, and 500 behavior through a local-only diagnostic
  failure endpoint.
- API JSON write boundaries now reject malformed JSON and non-object bodies with
  stable `400 invalid_json` responses that include the request id instead of
  falling through as empty objects. `bash scripts/verify-api-repository-boundary.sh`
  verifies invalid fake sign-in, capture publish, and profile settings bodies,
  while `bash scripts/verify-openapi-contract.sh` verifies mutating route error
  responses stay documented.
- Protected API writes now emit privacy-safe `notmid_api_audit` events for
  success, denied, and failed outcomes. Audit logs include request id, route,
  action, auth mode, actor id or `anonymous`, status, and outcome, but not
  bearer tokens or request body values. `bash scripts/verify-api-audit-logs.sh`
  verifies the log contract locally.
- API mutating routes now have a fixed-window in-memory rate limit for `POST`
  and `PATCH` requests under `/v1`. The default is
  `NOTMID_MUTATION_RATE_LIMIT_MAX=120` per
  `NOTMID_MUTATION_RATE_LIMIT_WINDOW_MS=60000`, with stable 429
  `rate_limited` responses and `retry-after` / `x-ratelimit-*` headers. `bash
  scripts/verify-api-rate-limit.sh` verifies per-client buckets without
  requiring Redis or paid infrastructure.
- Android release variant assembly passes with `./gradlew :app:assembleRelease`;
  the current artifact is `app-release-unsigned.apk`.
- Release build config now has a separate `NOTMID_RELEASE_API_BASE_URL` path so
  release variants do not inherit the Android emulator host default.
- Android runtime auth mode is now variant-specific. Debug defaults to local
  fake auth, while release defaults to `disabled`; release config/readiness
  gates reject `NOTMID_AUTH_MODE=fake` in release BuildConfig.
- Android `:core:network:*` now has a tested OkHttp-backed request boundary.
  `./gradlew :core:network:api:test :core:network:impl:test` verifies base URL
  normalization, encoded dynamic paths, GET/POST/PATCH requests, headers,
  JSON bodies, non-2xx response bodies, and invalid request failures before
  Android repositories switch from deterministic fake data to API-backed data.
- Android `:core:data` now includes `ApiNotmidContentRepository`, an API-backed
  implementation of `NotmidContentRepository` behind `NotmidNetworkClient`.
  It hydrates inbox chat messages from
  `/v1/inbox/threads/{threadId}/detail` into internal thread-message and
  attachment models before feature UI renders them. `./gradlew :core:data:test`
  verifies feed, map, capture draft, inbox summary/detail JSON mapping, plus
  typed HTTP, transport, and malformed-response failures.
- Android app-shell content now has a source selector and explicit async
  loading/error/ready state. Debug defaults to API-backed fixture content with
  `NOTMID_DEBUG_CONTENT_SOURCE=api`, while `static` remains an opt-in local
  fallback; release defaults to API-backed content with
  `NOTMID_RELEASE_CONTENT_SOURCE=api`. `./gradlew :app:testDebugUnitTest`
  and `./gradlew :app:compileDebugKotlin` verify the selector/state boundary,
  empty-content handling, and API error messaging without wiring screens
  directly to network code.
- Android `:core:auth:impl` now includes `ApiVerifiedNotmidAuthGateway`, which
  keeps Firebase mode behind the notmid API verification boundary. It obtains a
  Firebase ID token from an injected provider, calls `GET /v1/auth/status` with
  a bearer token, maps authenticated API responses into `NotmidAuthState`, and
  rejects fake providers, unavailable token providers, network failures,
  unverified sessions, and malformed API responses. `./gradlew
  :core:auth:impl:test` verifies the boundary without requiring a real Firebase
  project or committing `google-services.json`.
- Android Firebase auth now has a checked-in REST ID-token provider. It can call
  Identity Toolkit anonymous sign-in when a public `NOTMID_*_FIREBASE_API_KEY`
  is injected, and it can exchange an injected Google ID token through
  `accounts:signInWithIdp`. When an anonymous Android session exists, the
  provider includes that Firebase ID token in the Google exchange so Firebase
  links the anonymous account to the Google provider instead of silently
  replacing it. `ApiVerifiedNotmidAuthGateway.signOut()` clears the cached
  provider session. The app keeps Firebase sign-in work off the Compose click
  path with an explicit loading state. Release config gates reject
  `NOTMID_RELEASE_AUTH_MODE=firebase` unless `NOTMID_RELEASE_FIREBASE_API_KEY`
  is present.
- Android Google sign-in now has an app-layer Credential Manager provider in
  `app/src/main/java/app/thdev/glassnavlab/auth`. It obtains a Google ID token
  with the injected OAuth web client id (`NOTMID_*_GOOGLE_SERVER_CLIENT_ID`),
  passes that token into the existing Firebase Auth REST exchange, and still
  opens a notmid session only after `GET /v1/auth/status` verifies the Firebase
  ID token. `./gradlew :app:testDebugUnitTest` verifies missing client-id,
  cancellation, malformed-token, and reader-failure mapping without requiring a
  real Google account. Release config gates reject Firebase auth mode unless a
  Google server client id is injected.
- Android protected writes now have a `:core:domain` repository contract and
  `:core:data` static/API implementations for capture publish, clip save, chat
  send, and profile settings update. The API implementation adds
  `Authorization: Bearer <NotmidAuthState.session.accessToken>` only at the data
  boundary and returns typed missing-auth, HTTP, network, malformed-response, and
  invalid-request failures. The app shell wires the existing Capture, Chat, and
  Profile Settings controls to those writes without making feature screens own
  tokens or HTTP. Successful chat-send receipts are now merged into the loaded
  inbox `threadMessages` so the Android chat detail updates immediately after
  POST succeeds. `./gradlew :core:data:test` verifies the data boundary and
  `./gradlew :app:compileDebugKotlin` / `./gradlew :app:testDebugUnitTest`
  verify the Android wiring.
- Chat permission policy is now represented in the thread contract through
  `chatAccess`. Friend threads can send immediately; non-friend pending inbound
  threads expose accept/reject actions and block message sends until accepted.
  Fixture API mode enforces this with `403 chat_invite_required`, protected
  accept/reject routes, OpenAPI coverage, web server action wiring, and Android
  content-state merging for invite response receipts. Verified with the focused
  Gradle gate, `pnpm typecheck`, OpenAPI/API repository/web write checks, and
  `git diff --check`.
- Postgres-backed chat access now has an additive
  `0002_chat_thread_access` migration and repository policy. The adapter reads
  actor-specific `notmid_chat_thread_access` rows into `thread.chatAccess`,
  blocks `sendThreadMessage` with `403 chat_invite_required` while a non-friend
  invite is pending, and updates only `pending-inbound` rows for accept/reject.
  Verified with `pnpm typecheck`, `bash scripts/verify-api-postgres-repository.sh`,
  `bash scripts/verify-api-postgres-migrations.sh`,
  `bash scripts/verify-api-persistence-config.sh`, and `git diff --check`.
- Chat start is now a protected write at `POST /v1/inbox/threads`. The request
  carries a target handle, message body, and optional clip/place context; it
  never accepts client-provided relationship state. Fixture mode uses a
  deterministic relationship map, while Postgres mode reads
  `notmid_user_relationships`: friend targets create `accepted` access for both
  sides, and non-friend targets create `pending-outbound` for the requester plus
  `pending-inbound` for the recipient. The web clip detail page now starts a
  chat with the receipt creator through a server action. Verified with `pnpm
  typecheck`, OpenAPI/API repository/Postgres repository/migration/persistence
  checks, and `bash scripts/verify-web-write-actions.sh`.
- VibeGuard security, cost, data, and environment gates are green. Repository
  and structure remain review warnings because CI workflow files changed and
  some docs/lockfiles are large.
- Production dependency audit currently passes with `npm exec --yes
  pnpm@10.12.1 -- audit --prod`. The API Hono dependency is pinned above the
  `4.12.21` advisory floor through the workspace lockfile, and the root
  `pnpm.overrides` entry keeps transitive `postcss` on a patched release while
  Next.js still owns the web build pipeline.
- Stitch is connected: the notmid Stitch project has a `notmid` design system
  reachable through the Stitch connector. Private Stitch project and asset IDs
  should stay out of committed docs.
- The Android debug APK installs and launches on the connected device, and the
  Notmid activity can become the focused app behind keyguard.
- `bash scripts/verify-android-smoke.sh` was executed on 2026-05-30. It built,
  installed, and launched the app, then failed with `mCurrentFocus=NotificationShade`;
  this proves the script catches the current locked-device blocker instead of
  accepting a lock-screen screenshot.
- `bash scripts/verify-release-readiness.sh` was executed on 2026-05-30. It
  assembled the release variant and confirmed the release API URL does not use
  the Android emulator host, release auth does not use local fake mode, release
  content uses the API boundary, and Firebase release auth has the required
  public client identifiers when enabled. It then failed because the artifact is
  unsigned and still uses the local baseline version `1 / 1.0`.
- `docs/release/android-release-contract.md` now defines the Android release
  artifact, monthly CalVer `versionName`, monotonic `versionCode`, signing input
  names, release gates, tagging, and forward-fix expectations.
- GitHub Actions CI now runs `bash scripts/verify-local.sh`,
  `bash scripts/smoke-web-api.sh`, `bash scripts/verify-secret-hygiene.sh`,
  `bash scripts/verify-openapi-contract.sh`,
  `bash scripts/verify-api-auth-policy.sh`,
  `bash scripts/verify-api-firebase-auth.sh`,
  `bash scripts/verify-api-repository-boundary.sh`,
  `bash scripts/verify-api-postgres-repository.sh`,
  `bash scripts/verify-api-postgres-runtime.sh`,
  `bash scripts/verify-api-postgres-migrations.sh`,
  `bash scripts/verify-api-postgres-migration-workflow.sh`,
  `bash scripts/verify-api-persistence-config.sh`,
  `bash scripts/verify-api-diagnostics.sh`,
  `bash scripts/verify-api-audit-logs.sh`,
  `bash scripts/verify-api-rate-limit.sh`,
  `bash scripts/verify-web-api-production-config.sh`,
  `bash scripts/verify-web-firebase-auth-config.sh`,
  `bash scripts/verify-web-write-actions.sh`, and
  `bash scripts/verify-release-config.sh` on pull requests, pushes to `main`,
  and manual dispatch.
- `bash scripts/verify-web-api-production-config.sh` verifies that API
  production mode rejects localhost origins, missing production origins, local
  fake auth, local diagnostic failure endpoints, and Firebase auth without a
  Firebase project id. It also verifies that the web runtime rejects missing or
  localhost API URLs in production, rejects Firebase web auth without the public
  Google client id, and accepts explicit disabled or complete Firebase web auth
  modes.
- `bash scripts/verify-web-firebase-auth-config.sh` verifies the web Firebase
  Auth config, Google provider exchange/linking route, session bridge, and
  refresh-token exchange route without requiring a real Firebase project.
- `bash scripts/verify-web-write-actions.sh` verifies protected web write action
  wiring for capture publish, clip save, chat send, and profile settings update
  without requiring a real Firebase project.
- `bash scripts/verify-release-readiness.sh` now enforces the documented
  Android version contract. With temporary `NOTMID_VERSION_NAME=26.05.1` and
  `NOTMID_VERSION_CODE=26050101`, the version contract passes and the gate
  still fails on unsigned release output as intended.
- Android release config gates now also reject
  `NOTMID_CONTENT_SOURCE=static`, so a distributable build cannot silently ship
  fixture content.

## Open Release Blockers

- Android visual signoff is not complete until `bash scripts/verify-android-smoke.sh`
  captures the unlocked app UI, not the lock screen or notification shade.
- The current Android release artifact is still development-shaped:
  `versionCode = 1`, `versionName = "1.0"`, and unsigned. The release contract
  is documented, but real signing, version, and release auth provider inputs
  have not been supplied.
- `bash scripts/verify-release-readiness.sh` is the distribution gate for the
  Android release artifact. It must fail while the release APK is unsigned,
  still uses baseline version `1 / 1.0`, or points at the emulator API host.
- `bash scripts/verify-release-config.sh` is the CI-safe release configuration
  gate. It must pass before CI is green, but it intentionally does not validate
  signing secrets or production rollout readiness.
- `bash scripts/verify-secret-hygiene.sh` is the public-repo safety gate for
  visible commit candidates. It fails on tracked local env files, Firebase
  service config, service-account files, keystores, private keys, and non-empty
  sensitive values in example config templates.
- The app remains deterministic fake/local mode by default. That is correct for
  open-source baseline work, but a production deployment still needs server
  `NOTMID_AUTH_MODE=firebase`, `FIREBASE_PROJECT_ID`, web
  `NEXT_PUBLIC_NOTMID_AUTH_PROVIDER=firebase`, public Firebase web config,
  `NEXT_PUBLIC_NOTMID_GOOGLE_CLIENT_ID`, signing, and environment injection.
- The web Firebase session bridge now stores the short-lived Firebase ID token
  and refresh token in separate HTTP-only cookies after API verification, and a
  server refresh route can exchange the refresh token for a new ID token before
  updating cookies.
- Protected `/notmid` server-rendered navigation now has middleware refresh
  retry wiring for expired or near-expired Firebase ID tokens.
- Capture publish and chat send web forms now use server actions backed by the
  existing API write endpoints. The shared action helper retries once after
  server-side Firebase refresh when the API returns 401, then redirects to login
  if no verified session remains.
- Profile settings now has a server-side sign-out action that clears Firebase
  session cookies and the legacy fake local session cookie.
- Google account sign-in/linking is now wired on the web through Google
  Identity Services and Firebase Auth REST. A real production deployment still
  must create the Google OAuth client, enable the Firebase Google provider, and
  inject the public client id through deployment config.
- Google account sign-in/linking is now wired on Android through Credential
  Manager and Firebase Auth REST. A real production deployment still must create
  the Android package/SHA entries and OAuth web client, enable the Firebase
  Google provider, and inject `NOTMID_RELEASE_GOOGLE_SERVER_CLIENT_ID` through
  ignored local config or CI secrets.
- API production runtime config now blocks fake auth and localhost web origins,
  blocks fixture storage, and Firebase mode requires `FIREBASE_PROJECT_ID`.
  The Postgres schema artifact, repository adapter, runtime query client, and
  guarded migration workflow are present. The manual GitHub Actions migration
  job is connected to environment-scoped secret injection, but a real API
  hosting platform, managed database provisioning, backup/restore policy, and
  production environment secret creation are still not complete.
- VibeGuard still reports oversized documentation and `pnpm-lock.yaml` line
  counts. These are review-scope warnings, not runtime blockers.

## Release Gate

Before calling this patch production-release ready:

1. Run `bash scripts/verify-local.sh`.
2. Run `bash scripts/smoke-web-api.sh`.
3. Run `bash scripts/verify-secret-hygiene.sh`.
4. Run `bash scripts/verify-openapi-contract.sh`.
5. Run `bash scripts/verify-api-auth-policy.sh`.
6. Run `bash scripts/verify-api-firebase-auth.sh`.
7. Run `bash scripts/verify-api-repository-boundary.sh`.
8. Run `bash scripts/verify-api-postgres-repository.sh`.
9. Run `bash scripts/verify-api-postgres-runtime.sh`.
10. Run `bash scripts/verify-api-postgres-migrations.sh`.
11. Run `bash scripts/verify-api-postgres-migration-workflow.sh`.
12. Run `bash scripts/verify-api-persistence-config.sh`.
13. Run `bash scripts/verify-api-diagnostics.sh`.
14. Run `bash scripts/verify-api-audit-logs.sh`.
15. Run `bash scripts/verify-api-rate-limit.sh`.
16. Run `bash scripts/verify-web-api-production-config.sh`.
17. Run `bash scripts/verify-web-firebase-auth-config.sh`.
18. Run `bash scripts/verify-web-write-actions.sh`.
19. Run `bash scripts/verify-release-config.sh`.
20. Run `bash scripts/verify-android-smoke.sh` on an unlocked device or emulator.
21. Run `npm exec --yes pnpm@10.12.1 -- audit --prod`.
22. Run VibeGuard audit.
23. Run `bash scripts/verify-release-readiness.sh`.
24. Confirm signing, versioning, environment variables, and server auth mode for
   the intended release channel.
