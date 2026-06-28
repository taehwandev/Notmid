# Platform Backend

This page records Notmid backend, web, API, auth, and Android integration facts.
Reusable module-structure, ViewModel, data-flow, notice, and delegate rules live
in AgentPlayBook.

## Direction

notmid is a server-first product with Firebase as auxiliary infrastructure.

Use this mental model:

```text
Android / Web
  -> apps/api
      -> Postgres / Redis / Object Storage later
      -> Firebase Admin / FCM / App Check when useful
```

## Current Scaffold

```text
apps/api
  Hono TypeScript API
  fixture endpoints
  deep-link resolver endpoint
  repository boundary for local fixtures
  CI-safe Postgres repository adapter behind the query-port contract
  runtime Postgres query client wiring for NOTMID_DATA_BACKEND=postgres
  guarded Postgres migration runner with plan/apply modes
  additive chat thread access migration for friend/non-friend invite state
  additive user relationship migration for server-owned friend checks
  manual GitHub Actions migration job with environment approval and secret injection
  production storage config gate for fixture vs Postgres
  Postgres schema source artifacts under apps/api/db/postgres/
  Firebase SecureToken ID-token verification boundary
  strict JSON-object parsing for mutating request bodies
  privacy-safe audit logs for protected writes
  fixed-window mutation rate limit for POST/PATCH API requests
  protected profile settings read/update boundary
  protected capture publish, clip save, chat start/send, and profile settings writes
  request-id diagnostics and stable error responses

apps/web
  React/Next.js product shell
  /notmid canonical web surface
  detail routes for clips, places, map
  Firebase Auth anonymous ID-token acquisition through the public REST endpoint
  Google Identity Services -> Firebase Auth REST session exchange/linking path
  HTTP-only /notmid cookie bridge after API verifies Firebase ID tokens
  HTTP-only Firebase refresh-token exchange route for server-side token refresh
  /notmid middleware refresh retry before protected server-rendered routes continue
  profile settings server action that clears Firebase and legacy fake session cookies
  capture publish, clip save, profile edit, and chat start/send server actions backed by API write endpoints
  production web auth config gate for firebase/disabled vs local fake mode

packages/contracts
  route/path helpers
  shared DTOs or schemas
  deterministic fixture data
  parity resolvers only when route/API parity needs a reusable contract

packages/api-client
  typed fetch client
```

## Rules

- Do not put secrets in `packages/contracts`.
- Do not make Android depend on TypeScript packages directly.
- Keep shared behavior as URL/API docs and generated contracts later.
- Web and Android should use the same canonical URL shapes.
- API owns product write policy and token verification.
- API mutating routes must reject malformed JSON and non-object bodies with a
  stable `NotmidErrorResponse`; invalid bodies should not be treated as empty
  objects.
- Protected API writes must emit audit events without logging bearer tokens,
  raw request bodies, or secret values.
- Chat thread responses carry `chatAccess` so clients can render the current
  relationship gate without inventing policy locally. Friend threads use
  `accepted` access and can send immediately. Non-friend threads must move
  through invite states: `pending-inbound` exposes accept/reject actions,
  `pending-outbound` waits on the other account, `accepted` can send, and
  `rejected` cannot send. `POST /v1/inbox/threads/{threadId}/messages` remains
  the trusted enforcement boundary and returns stable `403 chat_invite_required`
  when a client tries to send before acceptance.
- Chat invite responses use protected writes at
  `POST /v1/inbox/threads` for starting a thread and at
  `POST /v1/inbox/threads/{threadId}/invite/accept` and
  `POST /v1/inbox/threads/{threadId}/invite/reject`. Fixture mode keeps this
  deterministic in memory for local testing; Postgres mode persists actor-level
  relationship and invite state in `notmid_chat_thread_access`, reads server-owned
  friend status from `notmid_user_relationships`, rejects message sends while the
  actor's invite is pending, and only updates pending inbound rows for
  accept/reject actions. Clients must not send `friend` or `non-friend`; they
  send the target handle and context, then render the returned `chatAccess`.
- Web can acquire Firebase ID tokens, but it must ask the API to verify them
  before opening the server-readable session cookie.
- Web Google sign-in uses the public Google OAuth client id and exchanges the
  Google ID token for a Firebase session through Next.js, linking to the current
  anonymous Firebase session when the HTTP-only access cookie is present.
- Web refresh tokens must stay in HTTP-only cookies and be exchanged server-side;
  they must not be stored in localStorage, sessionStorage, or client-visible config.
- Web `/notmid` middleware may refresh expired or near-expired Firebase ID
  tokens before server-rendered routes continue, but the refreshed ID token must
  still be verified by `apps/api` before cookies are updated.
- Protected web write actions must call `apps/api` with the server-readable
  session token, handle API 401 with one server-side Firebase refresh retry, and
  redirect to login when no verified session remains.
- The clip save path is the baseline protected engagement write: web forms post
  through a server action to `POST /v1/clips/{clipId}/save`; API auth remains
  the enforcement boundary, fixture mode stays deterministic, and Postgres mode
  records the user/clip row idempotently before bumping the public save count.
- Profile settings use `GET /v1/profile/settings` and
  `PATCH /v1/profile/settings`; the API remains the enforcement boundary for
  profile-edit, and Postgres mode stores display name and neighborhood in
  `notmid_users`.
- Android `:core:network:api` owns API paths/request/response contracts. The
  suspend `NotmidNetworkClient.execute` returns `NotmidNetworkResponse` for
  successful client execution and throws `NotmidNetworkException` only for
  transport, timeout, or invalid-request failures. `:core:network:impl` uses
  OkHttp and covers GET/POST/PATCH, headers, response bodies, and invalid
  request exceptions with JVM tests.
- Android `:core:data` has `ApiNotmidContentRepository`, which maps `/v1/feed`,
  `/v1/map`, `/v1/capture/draft`, `/v1/inbox/threads`, and
  `/v1/inbox/threads/{threadId}/detail` JSON into `NotmidDestination` models
  behind `NotmidNetworkClient`. Inbox detail hydration maps thread messages and
  clip/place attachments into internal models before feature UI sees them.
  `MainActivity` now selects static or API-backed content through
  `NOTMID_CONTENT_SOURCE` and loads content through an explicit
  loading/error/ready state before rendering the app shell. Debug defaults to
  the deterministic fixture API at the Android emulator host URL, static
  remains an opt-in fallback, and release gates reject static content.
- Android protected writes use `NotmidProtectedWriteRepository` in
  `:core:domain` and static/API implementations in `:core:data`. Capture
  publish, clip save, chat send, and profile settings update all use the current
  `NotmidAuthState.session.accessToken` only at the data boundary. The suspend
  repository methods return receipts on success and throw
  `NotmidProtectedWriteException` with typed failures for missing auth, invalid
  input, HTTP status, transport failure, and malformed API responses. Successful
  chat-send receipts are merged by `NotmidAppViewModel` into the loaded inbox
  `threadMessages` before toast/alert effects are emitted, so the chat screen
  reflects the write without feature UI owning refresh or network logic. Feature
  screens emit callbacks; they do not own bearer tokens or HTTP clients.
- Android chat invite accept/reject follows the same protected-write path. The
  API/static repository returns a thread receipt, `NotmidAppViewModel` merges it
  into loaded inbox content, and the feature chat screen unlocks or keeps the
  composer disabled from `thread.chatAccess.canSendMessage`. Feature UI never
  decides whether two users are friends; it only renders `chatAccess` and emits
  accept/reject callbacks.
- Android `:core:auth:impl` has an API-verified Firebase auth gateway. It asks a
  Firebase ID-token provider for anonymous or Google tokens, calls
  `GET /v1/auth/status` with `Authorization: Bearer <id-token>`, and opens an
  in-memory notmid session only after the API reports an authenticated user.
  The checked-in REST provider can create anonymous Firebase sessions with a
  public Firebase API key and can exchange an injected Google ID token through
  Identity Toolkit before the notmid API verification call. When the current
  Android session is anonymous, the Google exchange includes that Firebase ID
  token so Firebase links the account. The app-layer Android Credential Manager
  provider obtains the Google ID token with the injected OAuth web client id.
  Debug fake auth remains deterministic; release fake auth stays blocked.
- Web fake auth is local-only; production must explicitly choose firebase or
  disabled with `NEXT_PUBLIC_NOTMID_AUTH_PROVIDER`.
- Production API config must use `NOTMID_DATA_BACKEND=postgres` with
  `DATABASE_URL`; fixture storage is local-only.
- Firebase Auth tokens, if used, are verified by the API server through
  `apps/api/src/firebaseTokenVerifier.ts` before protected writes are allowed.
- FCM/App Check can be added without making Firebase the domain owner.

## Error Handling Contract

Use one typed failure path per boundary instead of string-matching raw
exceptions in callers:

```text
API route/input/auth/rate-limit failure
  -> stable NotmidErrorResponse { error.code, message, requestId? }
  -> packages/api-client NotmidApiRequestError { status, statusText, apiError? }
  -> web server action redirect/retry decision or Android typed failure state
```

- `apps/api` maps malformed JSON, validation failures, auth failures, not-found
  results, rate limits, and unhandled exceptions into stable error codes. 500
  responses must include the request id and must not expose raw exception
  messages.
- Repository implementations return `NotmidRepositoryResult` for expected
  product failures such as validation and not-found. Hono route handlers own
  transport status mapping, request ids, auth, rate-limit headers, and audit
  events.
- Protected web actions refresh a Firebase ID token once after API 401, then
  redirect to login if no verified session remains. They should use
  `NotmidApiRequestError.status` and `apiError?.code` for expected redirects,
  and rethrow unexpected failures.
- Android content reads keep raw network failures in `:core:network`, map
  API-backed read failures to `ApiNotmidContentException`, and expose app-shell
  loading/error/ready state through `NotmidAppViewModel`. Inbox chat detail
  messages are normal content-read data, not write effects; a failed detail
  read raises the same typed content exception path and the ViewModel decides
  the visible app-shell error state. Android protected writes throw
  `NotmidProtectedWriteException`; `NotmidAppViewModel` catches it and keeps
  feature screens from seeing raw API errors or envelopes.
- Retries must be explicit. Rate-limited responses use `retry-after`; protected
  web write refresh retry is limited to one session refresh; mutating writes
  should not be retried automatically unless the API path is idempotent or the
  caller owns duplicate handling.
- Logs and audit events may include request id, route, action, auth mode,
  actor id, status, and outcome. They must not include bearer tokens, refresh
  tokens, raw request bodies, private Firebase values, or raw personal data.

## Local Defaults

```text
API: http://localhost:8787
Web: http://localhost:3000/notmid
```

## Next Backend Steps

1. Choose the API hosting platform, managed Postgres provider, backup policy,
   and staging/production secret ownership for the migration workflow.
2. Add Firebase Admin only for push/app integrity or identity features that need
   privileged server SDK access.
