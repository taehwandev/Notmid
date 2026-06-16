# notmid Monorepo Platform Spec

## Decision

notmid is managed as one git repository with separate build systems:

```text
Android: Gradle / Kotlin / Compose
Web: pnpm / React / Next.js
API: pnpm / TypeScript / Hono
Contracts: TypeScript package plus docs/OpenAPI later
```

Different languages are acceptable because each platform keeps its own build boundary. The shared surface is not source-code reuse between Kotlin and TypeScript; it is product contracts:

- canonical URLs
- deep-link route stacks
- API paths
- DTO shapes
- auth/session policy
- fake fixture data for local development

## Repository Layout

```text
app/
core/
feature/
build-logic/

apps/
  api/
  web/

packages/
  contracts/
  api-client/

docs/
llm-wiki/
.agents/skills/
```

## Ownership

`apps/api` owns:

- HTTP API routes
- auth token verification boundary
- product write policy
- future persistence integration
- Firebase Admin/FCM integration when needed

`apps/web` owns:

- React/Next.js app shell
- web route rendering
- shareable detail pages
- Open Graph/social preview surfaces later

`packages/contracts` owns:

- canonical notmid web paths
- shared TypeScript DTOs
- fake fixtures used by web/API local mode
- route stack resolver for server/web parity checks

As it grows, `packages/contracts` must split by caller-facing contract family:

- `routes/` for canonical paths and URL helpers.
- `dto/` or `schema/` for request/response shapes and validation contracts.
- `fixtures/` for deterministic local data.
- `parity/` for route/API parity resolvers when a separate caller needs them.

Do not hide all contract families behind one broad `index` export.

`packages/api-client` owns:

- typed fetch wrapper for web and server-side tools
- no product state
- no secrets

Android owns:

- Compose app shell
- Android route stack execution
- ActivityRoute dispatch
- native camera/map/chat surfaces

## Backend Direction

notmid is server-first:

```text
Android / Web
  -> notmid API Server
      -> Postgres / Redis / Object Storage
      -> Firebase Admin / FCM / App Check when useful
```

Firebase is auxiliary:

- Firebase Auth can provide identity tokens.
- The API server verifies tokens and maps them to notmid users.
- FCM handles push notifications.
- App Check can reduce abuse.
- Crashlytics/Analytics may be used by clients.
- Firestore may still be used for prototypes, but it should not become the long-term product contract if the server owns the domain.

## Local Mode

Fresh checkouts should work without production credentials:

- API serves fixture data.
- Web falls back to fixture data when API is unavailable.
- Android keeps deterministic fake repositories.
- `.env.example` files document local variables.
- real `.env`, service accounts, Firebase Admin SDK JSON, App Check debug tokens, and keystores stay ignored.

## URL Contract

Canonical URLs:

```text
/notmid
/notmid/feed
/notmid/map
/notmid/clips/{clipId}
/notmid/places/{placeId}
/notmid/chats/{threadId}
/notmid/profile
/notmid/profile/settings
```

The same URL should:

- render in `apps/web`
- resolve through `packages/contracts`
- map into ordered Android route stacks

Example:

```text
/notmid/profile/settings
  -> Profile
  -> ProfileSettings
```

## Verification

Android:

```bash
./gradlew :app:compileDebugKotlin
./gradlew test
```

Web/API after dependencies are installed:

```bash
pnpm install
pnpm typecheck
pnpm build:web
```

Patch hygiene:

```bash
git diff --check
```
