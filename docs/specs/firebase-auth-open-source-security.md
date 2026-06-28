# Backend, Firebase Auth, And Open Source Security Spec

## Purpose

notmid is evolving into a real short-form, place-based social service with feed, map, capture, inbox/chat, and profile features. The product is server-first, with Firebase used as auxiliary infrastructure where it is the right tool. The repository is open source, so production access must never depend on obscured client files or committed private keys.

This spec defines what can be committed, how login should work, and how server-first modules with Firebase-assisted integrations should be shaped.

## Product Backend Scope

Use Firebase only when fake local data stops being useful and the server boundary needs that integration. The first real backend should cover:

- TypeScript API server for product reads/writes, auth verification, and route/deep-link support.
- Postgres for clip, place, profile, saved item, thread, and message metadata.
- Object storage for short video and thumbnail objects.
- Firebase Authentication as an identity provider if it remains the lowest-friction option.
- Firebase Cloud Messaging for chat and social notifications.
- Firebase App Check for client/app integrity signals.
- Firebase Emulator Suite only for Firebase pieces that need local rules/tests.

Do not add Firebase just to make the sample feel production-like. Add it when a feature needs real identity, push, app integrity, emulator coverage, or another explicit Firebase capability.

Current persistence boundary:

- `NOTMID_DATA_BACKEND=fixture` is the deterministic local default.
- `NODE_ENV=production` requires `NOTMID_DATA_BACKEND=postgres` and
  `DATABASE_URL`, and rejects fixture storage.
- `apps/api/db/postgres/schema.sql` and additive migration files such as
  `0002_chat_thread_access.sql` and `0003_user_relationships.sql` are the
  reviewed source artifacts for Postgres tables. Local verification checks them
  but does not apply them to a real database.
- `apps/api/src/postgresNotmidRepository.ts` provides a CI-safe Postgres
  repository adapter behind a small query-port abstraction; local verification
  uses a fake query client and does not require a database or committed
  credentials.
- `apps/api/src/postgresQueryClient.ts` wires `NOTMID_DATA_BACKEND=postgres` to
  a runtime Postgres.js query client. Config validation and CI health checks do
  not open a production database connection.
- `apps/api/src/postgresMigrations.ts` and `scripts/migrate-api-postgres.sh`
  provide the explicit Postgres migration workflow. `--plan` is CI/local safe;
  `--apply` requires `DATABASE_URL` and `NOTMID_MIGRATION_CONFIRM=apply`.
- `.github/workflows/api-postgres-migrations.yml` is the manual GitHub Actions
  migration job. It uses GitHub Environment approval, the environment-scoped
  `NOTMID_DATABASE_URL` secret, and typed confirmation before applying.
- Production migrations must run only through an approved deployment or
  migration job, not through general local verification.

## Auth Policy

MVP login should be low-friction but not anonymous-only:

- Browsing feed/map may work signed out or with an anonymous session.
- Capture/upload, chat, save, follow, report, and profile editing require an authenticated user.
- Support anonymous sign-in first, then link to a permanent provider so draft saves and onboarding state are preserved.
- Use Google sign-in as the first permanent provider.
- Avoid password auth in the first implementation unless there is a concrete need. It increases account recovery, abuse, and support surface.
- Keep provider-specific UI inside auth impl. Features consume only app auth state and commands.

Domain-level auth model should stay Firebase-free:

```text
:core:auth:api
  AuthUser
  AuthState
  AuthRepository
  SignInCommand

:core:auth:impl
  FirebaseAuthRepository
  Google credential bridge
  Anonymous-to-Google account linking
```

Features depend on `:core:auth:api`, not Firebase SDKs. If Firebase Auth is used, Android and web clients send Firebase ID tokens to `apps/api`; the API verifies tokens and maps them to notmid users.

## Local Fake Auth Contract

The first web/auth slice keeps Firebase optional and makes the auth boundary visible without committing credentials.

- `GET /v1/auth/status` returns signed-out fake mode by default.
- `POST /v1/auth/fake-sign-in` returns the deterministic local token `notmid-fake-local-dev-token`.
- The web login page stores that token in an HTTP-only `/notmid` cookie for local gated routes.
- Capture, saves, chats, profile edits, and moderation remain the protected action set.

This fake token is not a credential and must not be accepted in production mode. When `NOTMID_AUTH_MODE` moves to `firebase`, clients should send Firebase ID tokens to `apps/api` and the API should map verified identities into notmid users.

Current API auth boundary:

- `apps/api/src/authPolicy.ts` owns API auth context resolution and protected write checks.
- The deterministic fake token authenticates only when `NOTMID_AUTH_MODE=fake`.
- `NOTMID_AUTH_MODE=disabled` and `NOTMID_AUTH_MODE=firebase` reject fake-token writes.
- `NOTMID_AUTH_MODE=firebase` verifies Firebase SecureToken ID tokens in
  `apps/api/src/firebaseTokenVerifier.ts` with `FIREBASE_PROJECT_ID`, RS256
  issuer/audience/expiry checks, and Google's public SecureToken certificates
  before protected writes are allowed.
- `bash scripts/verify-api-auth-policy.sh` verifies those mode boundaries without requiring Firebase credentials.
- `bash scripts/verify-api-firebase-auth.sh` verifies signed-token acceptance,
  invalid-token rejection, provider mapping, and fake-token rejection with local
  generated keys.

Current web auth boundary:

- `NEXT_PUBLIC_NOTMID_AUTH_PROVIDER=fake` is the local default. Production must
  explicitly choose `firebase` or `disabled`; fake mode is rejected by config
  verification.
- `NEXT_PUBLIC_NOTMID_AUTH_PROVIDER=firebase` requires public Firebase web app
  values: API key, auth domain, project id, and app id. It also requires a
  public `NEXT_PUBLIC_NOTMID_GOOGLE_CLIENT_ID` so the first permanent provider
  is available in production. These values are public client config, not
  service-account credentials or OAuth client secrets.
- `apps/web/src/lib/notmidFirebaseClient.ts` uses Firebase Auth REST for
  anonymous sign-in. It does not use localStorage, sessionStorage,
  browser-local persistence, or a committed Firebase service account.
- `apps/web/src/app/notmid/login/FirebaseLoginActions.tsx` loads Google Identity
  Services for Google sign-in. The browser receives a short-lived Google ID
  token only in memory and posts it to
  `apps/web/src/app/notmid/login/firebase-session/google/route.ts`.
- The Google session route exchanges the Google ID token through Firebase Auth
  REST `accounts:signInWithIdp`, includes the current HTTP-only Firebase ID
  token when present to link an anonymous session, verifies the resulting
  Firebase ID token with `apps/api`, then sets the same HTTP-only session
  cookies used by anonymous sign-in.
- `apps/web/src/app/notmid/login/firebase-session/route.ts` accepts a Firebase
  ID token from the browser, asks `apps/api` to verify it through
  `GET /v1/auth/status`, and sets the HTTP-only `/notmid` cookie only after the
  API reports an authenticated user.
- The web session cookie stores the short-lived Firebase ID token as a bearer
  token for server-rendered web routes. The refresh token is stored in a
  separate HTTP-only cookie and can be exchanged by
  `apps/web/src/app/notmid/login/firebase-session/refresh/route.ts`; the refresh
  route verifies the new ID token with `apps/api` before updating cookies.
- `apps/web/src/middleware.ts` retries expired or near-expired Firebase sessions
  before `/notmid` server-rendered product routes continue, forwarding the
  refreshed ID token into the same request and writing the renewed HTTP-only
  cookies on the response.
- `apps/web/src/app/notmid/profile/settings/page.tsx` owns the server-side
  sign-out action for the web shell. It clears the Firebase ID-token cookie, the
  Firebase refresh-token cookie, and the legacy local fake session cookie.
- `apps/web/src/lib/notmidServerActions.ts` owns the server-action write
  boundary for protected web forms. Capture publish and chat send actions pass
  the HTTP-only access token to `apps/api`, retry once after a server-side
  Firebase refresh when the API returns 401, and redirect to login when no
  verified session is available.
- `bash scripts/verify-web-firebase-auth-config.sh` verifies production web auth
  config, client-visible secret rejection, anonymous ID-token acquisition
  wiring, Google Identity Services wiring, Firebase Google exchange/linking,
  HTTP-only cookie bridge wiring, server-side refresh-token exchange wiring, and
  middleware refresh retry wiring without requiring real Firebase credentials.
- `bash scripts/verify-web-write-actions.sh` verifies protected web write action
  wiring for capture publish, chat send, typed API errors, and action-level
  refresh retry wiring without requiring real Firebase credentials.

Current Android DI boundary:

- App Hilt modules provide BuildConfig-backed runtime config, the product API
  network client, the Firebase Identity Toolkit network client, repository
  selection, Firebase token providers, and the `NotmidAuthGateway`.
- `NotmidAppViewModel` is a Hilt ViewModel and receives repository/auth
  contracts from DI. `MainActivity` must not manually construct
  `OkHttpNotmidNetworkClient`, repositories, Firebase token providers, auth
  gateways, or a `ViewModelProvider.Factory`.
- The Android Credential Manager Google ID-token provider remains app-layer
  because it needs Android context and public client id config; feature modules
  consume only auth state/actions.

## Module Shape

Use `api` and `impl` only where it creates a useful boundary:

```text
apps/api
apps/web
packages/contracts
packages/api-client

:core:auth:api
:core:auth:impl

:core:network:api
:core:network:impl

:core:domain
  FeedRepository
  PlaceRepository
  ChatRepository
  CaptureRepository

:core:data:fake
  Deterministic fake service data for previews/tests

:core:data:firebase
  Firestore/Storage implementations of domain repositories

:feature:feed:api
:feature:feed:impl
:feature:map:api
:feature:map:impl
:feature:capture:api
:feature:capture:impl
:feature:inbox:api
:feature:inbox:impl
:feature:chat:api
:feature:chat:impl
:feature:profile:api
:feature:profile:impl
```

Do not create a `data:api` module if `:core:domain` already owns repository contracts. Do create `api/impl` for auth because auth state and sign-in commands are cross-feature contracts while Firebase is an implementation detail.

## Open Source Secret Rules

Treat these as never-commit files:

- `google-services.json` for real dev/staging/prod projects.
- Firebase Admin SDK JSON files such as `firebase-adminsdk-*.json`.
- Google Cloud service account JSON files.
- App Check debug tokens.
- OAuth client secrets.
- Keystore files, signing passwords, upload keys, and Play Console credentials.
- `.env`, `secrets.properties`, and generated CI credential files.

Allowed in the repository:

- `google-services.example.json` with placeholder values only.
- Documentation showing where local files should be placed.
- Firebase project IDs only if intentionally public.
- Security Rules source files after they contain deny-by-default, reviewed rules.

Firebase Android API keys are not authorization secrets, but they still identify the project. For an open-source repository, prefer not committing real production `google-services.json`; contributors should use their own Firebase project or a local development config.

## Firebase Project Layout

Use separate Firebase projects per environment:

```text
notmid-dev
notmid-staging
notmid-prod
```

Recommended Android application IDs:

```text
app.thdev.notmid.debug
app.thdev.notmid.staging
app.thdev.notmid
```

Each Firebase Android app must register the matching package name and signing certificate fingerprints. Debug and CI fingerprints must never be reused for production.

## Local And CI Configuration

Local development:

- Real `google-services.json` stays untracked.
- `app/google-services.example.json` may document the expected shape.
- Android local runtime values, including Maps provider keys, stay in root `local.properties`.
- Developers create their own Firebase project or request access to a shared dev project.
- App Check debug tokens stay in local environment variables or local secure storage, not files committed to Git.

CI:

- Generate `google-services.json` from encrypted CI secrets at build time.
- Prefer keyless Google Cloud access such as Workload Identity Federation where server-side deploy access is needed.
- If a service account key is unavoidable, store it only in the CI secret store, grant minimal IAM roles, rotate it, and delete it when no longer needed.
- Never print `google-services.json`, service account JSON, App Check debug tokens, or signing credentials in logs.

## API Key Restrictions

For every Firebase API key:

- Apply Android application restrictions by package name and SHA certificate fingerprint.
- Keep API restrictions to only APIs used by the app.
- Set quotas where the Google Cloud API supports quotas.
- Do not reuse a Firebase API key for non-Firebase services such as Maps, Places, or Gemini. Use separate restricted keys for those APIs.

Do not rely on API key restrictions for data access. Firestore and Storage access must be enforced by Firebase Security Rules and App Check.

## Android Local Properties

The Android app reads local runtime values from three sources, in this order:

1. Gradle property: `-PNOTMID_GOOGLE_MAPS_API_KEY=...`
2. Root `local.properties`
3. Environment variable: `NOTMID_GOOGLE_MAPS_API_KEY=...`

Use `local.properties.example` as the safe template. Real local values belong only in ignored `local.properties`:

```properties
NOTMID_DEBUG_API_BASE_URL=http://10.0.2.2:8787
NOTMID_RELEASE_API_BASE_URL=https://thdev.app
NOTMID_DEBUG_AUTH_MODE=fake
NOTMID_RELEASE_AUTH_MODE=disabled
NOTMID_DEBUG_FIREBASE_API_KEY=
NOTMID_RELEASE_FIREBASE_API_KEY=
NOTMID_DEBUG_FIREBASE_AUTH_REQUEST_URI=https://thdev.app/notmid/firebase-auth/android
NOTMID_RELEASE_FIREBASE_AUTH_REQUEST_URI=https://thdev.app/notmid/firebase-auth/android
NOTMID_DEBUG_GOOGLE_SERVER_CLIENT_ID=
NOTMID_RELEASE_GOOGLE_SERVER_CLIENT_ID=
NOTMID_DEBUG_MAP_PROVIDER=fake
NOTMID_RELEASE_MAP_PROVIDER=fake
NOTMID_GOOGLE_SERVER_CLIENT_ID=
NOTMID_GOOGLE_MAPS_API_KEY=
NOTMID_MAPBOX_ACCESS_TOKEN=
```

Client-side Firebase API keys, Google OAuth web client ids, and map keys are
packaged into debug/release APKs when set, so they must be treated as public
identifiers, not authorization secrets. Restrict them in Google Cloud or the map
provider console by package name, signing certificate fingerprint, API scope,
and quota.

Android release builds must not package local fake auth. Until Firebase Auth is
fully configured for a release environment, release candidates should use
`disabled`. Android `:core:auth:impl` now has `ApiVerifiedNotmidAuthGateway`,
which accepts
anonymous or Google Firebase ID tokens from an injected provider, sends them to
`GET /v1/auth/status`, and opens an in-memory notmid session only after the API
reports an authenticated Firebase user. The checked-in fallback provider is
unavailable when public Firebase config is absent, so no Firebase project values
or client secrets are committed. The checked-in REST provider can use a public,
provider-restricted Firebase API key for anonymous sign-in and can exchange an
injected Google ID token for a Firebase session. The Android app-layer
Credential Manager provider obtains the Google ID token with
`NOTMID_*_GOOGLE_SERVER_CLIENT_ID`, which should be the OAuth web client id used
for Sign in with Google. When an anonymous session exists, the Android REST
provider passes that Firebase ID token to `accounts:signInWithIdp` so Firebase
links the anonymous account to the Google provider. Use
`NOTMID_RELEASE_AUTH_MODE=firebase` only with `NOTMID_RELEASE_FIREBASE_API_KEY` and
`NOTMID_RELEASE_GOOGLE_SERVER_CLIENT_ID` injected from ignored local config or
CI secrets, and keep ID-token verification on the notmid API boundary.

## App Check

Enable App Check before any public Firebase backend is used:

- Use Play Integrity provider for release and staging builds.
- Use the debug provider only for debug builds and CI.
- Store CI debug tokens in encrypted secrets.
- Do not share debug builds with untrusted users when debug App Check is enabled.
- Turn on enforcement per Firebase product only after staging verifies auth, rules, and upload flows.

## Firestore Data Model Draft

Initial collections:

```text
/users/{uid}
  displayName
  photoUrl
  handle
  bio
  createdAt
  updatedAt

/clips/{clipId}
  ownerUid
  placeId
  caption
  videoPath
  thumbnailPath
  visibility
  likeCount
  commentCount
  createdAt

/places/{placeId}
  name
  region
  category
  geoHash
  lat
  lng
  coverClipId

/users/{uid}/savedClips/{clipId}
/users/{uid}/savedPlaces/{placeId}

/chatThreads/{threadId}
  participantUids: map<uid, true>
  lastMessage
  updatedAt

/chatThreads/{threadId}/messages/{messageId}
  senderUid
  kind
  text
  clipId
  placeId
  createdAt
```

Avoid storing secrets, private tokens, raw device identifiers, or unnecessary location history in Firestore.

## Storage Layout Draft

```text
/clips/{ownerUid}/{clipId}/original.mp4
/clips/{ownerUid}/{clipId}/thumbnail.jpg
/avatars/{uid}/profile.jpg
```

Upload writes must require `request.auth.uid == ownerUid`. Public read is allowed only for published content. Private draft uploads should require owner read access.

## Security Rules Baseline

Rules must start deny-by-default. Minimum policy:

- Users can read public profile fields.
- Users can update only their own profile document.
- Clip creation requires signed-in owner and matching `ownerUid`.
- Clip updates/deletes require owner.
- Public clips are readable by anyone if the product intentionally allows signed-out browsing.
- Chat threads and messages are readable only by participants.
- Message writes require signed-in sender who is a participant.
- Storage clip writes require signed-in owner and validated file path.
- Storage reads follow clip visibility or owner access.

Rules must be tested in the Firebase Emulator Suite before production deployment.

## Chat Policy

Chat is part of the service, but it should be place/clip aware rather than generic messaging:

- `Inbox` lists threads.
- `ChatRoom` supports text, clip attachment, place attachment, and route/share cards.
- Sharing a clip/place emits a feature navigation event from `:feature:*:api`; chat impl resolves it through app navigation, not by depending on another feature impl.
- Chat notifications use FCM after the server-side write path is defined.

## Abuse And Moderation

Auth, API restrictions, and App Check do not solve product abuse. Before public upload:

- Add report/block models.
- Add upload size and duration limits.
- Use server-side validation for publish transitions.
- Consider Cloud Functions for thumbnail generation, video moderation hooks, counter updates, and notification fanout.
- Keep client-written counters non-authoritative.

## Implementation Phases

1. Keep fake repositories and finish product-shaped feature modules.
2. Extend `:core:auth:api` and `:core:auth:impl` from the local release-safe
   boundary to anonymous and Google sign-in.
3. Add Firebase Emulator Suite and rules tests.
4. Add `:core:data:firebase` for Firestore read/write metadata.
5. Add Storage upload for capture drafts and published clips.
6. Enable App Check in staging, then production enforcement.
7. Add chat persistence, then FCM notification fanout.
8. Add CI secret generation and secret scanning before public Firebase config work.

## Required Review Gates

Before merging Firebase work:

- No real `google-services.json` or service account JSON in Git.
- No Firebase Admin SDK key in repo history.
- `git diff --check` passes.
- Android compile passes.
- Firestore and Storage rules tests pass in emulator.
- App Check debug provider is excluded from release builds.
- API keys have Android app restrictions and API restrictions.
- Production Firebase project is not used by debug builds.

## References

- Firebase API key guidance: https://firebase.google.com/docs/projects/api-keys
- Firebase Auth anonymous Android flow: https://firebase.google.com/docs/auth/android/anonymous-auth
- Firebase Auth Google sign-in Android flow: https://firebase.google.com/docs/auth/android/google-signin
- Android Credential Manager Sign in with Google: https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation
- Firebase Security Rules basics: https://firebase.google.com/docs/rules/basics
- Firestore Security Rules getting started: https://firebase.google.com/docs/firestore/security/get-started
- Firebase App Check with Play Integrity: https://firebase.google.com/docs/app-check/android/play-integrity-provider
- Firebase App Check debug provider: https://firebase.google.com/docs/app-check/android/debug-provider
- Google Cloud service account key best practices: https://cloud.google.com/iam/docs/best-practices-for-managing-service-account-keys
