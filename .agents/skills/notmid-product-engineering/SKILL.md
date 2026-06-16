---
name: notmid-product-engineering
description: Use when implementing notmid product features, Compose UI, design-system components, feature API/impl modules, route events, deep links, fake data, server/web/API contracts, Firebase-safe open-source behavior, or Stitch/web design alignment in this repository.
---

# notmid Product Engineering

## Purpose

Build notmid like a real open-source product, not a sample screen.

Use this skill when adding or changing:

- feature `api` / `impl` modules
- Compose screens and state holder boundaries
- `Notmid*` design-system components
- route events, route stacks, and deep links
- fake product data or domain contracts
- web/API monorepo scaffolding and shared contracts
- Firebase/auth/security docs or open-source config rules
- Stitch/web design briefs that must match Android implementation

If the task changes low-level Liquid Glass rendering, AGSL, backdrop capture, or gesture behavior, also use `android-liquid-glass-compose`.

## Start Here

1. Run `git status --short`.
2. Read only the relevant LLM wiki page:
   - `llm-wiki/notmid-overview.md` for product direction.
   - `llm-wiki/module-map.md` for module ownership.
   - `llm-wiki/design-system.md` for UI/component rules.
   - `llm-wiki/routing-deeplinks.md` for navigation and web links.
   - `llm-wiki/platform-backend.md` for web/API/contracts and server-first backend direction.
   - `llm-wiki/firebase-open-source.md` for auth/Firebase/secrets.
   - `docs/specs/android-commonization/README.md` when touching `core/app`, `assertions`, router/network/feedback commonization, WebView runtime, or app-shell structure.
   - `llm-wiki/implementation-checklist.md` before verifying or finishing.
3. Inspect the smallest matching source files with `rg --files` and `sed`.
4. Keep unrelated dirty worktree changes out of the write set.

## Product Rules

- First screen is the app shell, not a landing page.
- notmid is short video + places + map + chat.
- Browsing can work signed out; capture, save, chat, and profile edits require auth.
- notmid is server-first; Firebase is auxiliary for auth, push, app integrity, analytics, and tooling where useful.
- Real Firebase secrets, API keys, service accounts, and MCP credentials never go in repo.
- Fake data mode should remain deterministic and usable.

## Android Architecture Rules

- `:app` owns Android entry points, manifest/theme wiring, top-level composition,
  runtime config injection, and concrete platform launch binding. It must not
  own reusable router assembly, route registry construction, deep-link resolver
  construction, or feature event mapping.
- `:core:designsystem` owns `NotmidTheme`, tokens, Material3 wrappers, reusable Notmid components, visual feedback primitives, and Liquid Glass primitives. It must not collect feedback flows or show Android Toasts.
- `:core:model` remains pure Kotlin models. No Compose, Android, `Color`, `Dp`, or UI effect runtime contracts.
- `:core:feedback:api` owns pure feedback request/effect contracts. Split files and packages by role: `model/` for request/presentation/tone/action values and `effect/` for one-shot effects and delegates. Use `sealed interface` for closed feedback effect/action families; use regular interfaces for injection points such as delegates.
- `:core:domain` owns repository contracts and use cases.
- `:core:data` owns fake/static implementations and mapping into domain models.
- `:core:router:api` stays pure Kotlin route contracts.
- `:core:router:impl` owns default route registry/matching, not Android Activity launching.
- `:core:app` owns Android/Compose app-runtime common contracts such as router runtime, router bundle/config assembly, feedback hosts, permission adapters, ActivityRoute launch adapters, reusable WebView runtime, resources, and app-shell helpers.
- `:core:app` feedback packages must stay app-runtime only. Activity/Compose shells install `FeedbackHost`; feature/ViewModel code emits `:core:feedback:api` contracts. Do not put product copy, repositories, network error mapping, or feature route policy inside the host package.
- `:core:app` router packages must be split by role: `config/` for reusable
  bundle/config assembly, `planner/` for app route planning, `runtime/` for
  Compose back-stack and pending ActivityRoute state, `deeplink/` for app
  resolver adapters, and `activity/` for ActivityRoute launch contracts and
  launch handler registries. Do not put every router file in one package.
- `:core:router:impl` owns reusable pure router implementation: route registry,
  route event planner, URI-to-deep-link request parsing, scheme/host/base-path
  normalization, static/prefix deep-link specs, and deep-link resolver. Split
  implementation packages by role: `registry/`, `event/`, and `deeplink/`.
  `:core:app` may compose these defaults behind a reusable bundle config, but it
  must not own Notmid feature route policy, WebView Intent construction, auth
  policy, repositories, or feature impl imports. Add a separate router
  assertions Gradle module only after app-runtime fakes have multiple external
  test consumers.
- `:core:network:assertions` owns reusable `NotmidNetworkClient` test doubles,
  queued network responses/failures, request assertion subjects, safe header
  redaction helpers, and API error envelope fixtures. Do not keep duplicate
  fake network clients inside auth/data repository tests once this boundary can
  be reused.
- `:feature:*:api` owns feature route contracts, deep-link specs, and route events. Split them by caller-facing role once more than one contract family exists: `route/`, `deeplink/`, `event/`, and `destination/` or `activity/` only when those are real import boundaries.
- `:feature:*:impl` owns Compose screens and emits events. It must not depend on another feature impl.
- `:*:assertions` modules own reusable test doubles, recording fakes, fixtures, and assertion helpers. They should depend on matching API contracts, not production impl modules.
- `:feature:*:assertions` exists only when route/display fixtures or recording helpers are reused across test boundaries.
- `apps/api` owns HTTP API, token verification boundary, and future persistence integrations.
- `apps/web` owns React/Next.js routes and shareable web surfaces.
- `packages/contracts` owns canonical web route helpers, TypeScript DTOs, and deterministic fixtures.
- `packages/api-client` owns typed fetch helpers. It must not own product state or secrets.

## Cross-Language Contract Rules

- Apply the same contract-family split to Kotlin, TypeScript, server code, web
  code, tests, and future shared packages.
- Do not put route helpers, DTOs/schemas, API clients, server handlers, route
  events, fixtures, and assertion helpers into one catch-all contract file or
  barrel export.
- `packages/contracts` should split by import boundary as it grows: route/path
  helpers, DTO or schema shapes, fixture data, and parity resolvers should be
  importable without pulling each other by default.
- `apps/api` should keep route handlers, request parsing/validation, product
  policy, repository ports, repository adapters, and error mapping in separate
  owners when more than one caller or test boundary needs them.
- `apps/web` should keep route rendering, server actions, session/cookie
  handling, API-client calls, and feature UI contracts separate. Web UI should
  not import server-only auth/session/runtime helpers through a broad barrel.
- `packages/api-client` owns typed transport helpers and errors only. It should
  not re-export product fixtures, web route rendering, server handlers, Android
  route contracts, or app state.

## Design-System Rules

- Prefer `Notmid*` wrappers over direct Material3 imports in feature/app modules.
- Use `NotmidText`, `NotmidButton`, `NotmidTextField`, `NotmidScaffold`, `NotmidTopAppBar`, and related wrappers.
- Use `NotmidBottomNavigation` for app bottom nav. Do not wire `LiquidGlassBottomNavigation` directly in features.
- Feature product cards may adapt models into generic design components:
  - `NotmidClipCard -> NotmidGradientSummaryCard`
  - `NotmidPlaceCard -> NotmidGradientHeroCard`
- Keep media-derived palettes in feature UI models; keep reusable styling in `:core:designsystem`.
- Feedback runtime orchestration belongs in `:core:app`. Keep reusable visual components and tokens in `:core:designsystem`, and keep caller-facing contracts in `:core:feedback:api`.

## Routing Rules

- For mixed Activity and Compose navigation changes, apply `.agents/skills/android-mixed-activity-compose-router/SKILL.md` first. Use `.agents/skills/glassnavlab-android-stewardship/references/mixed-activity-compose-router-reference.md` only for the Notmid/reference-project comparison notes. The caller should say where to go; the app router decides whether that becomes a Compose stack mutation, Activity launch, deferred auth route, or future Nav3 back stack.
- Deep links must resolve to ordered route plans, not just a top destination.
  Compose destinations carry a `RouteStack`; Activity-backed destinations carry
  `ActivityRoute` launch requests.
- Feature impl modules emit events. The product shell or owning feature runtime
  supplies route registrations and event handlers to the `:core:app` router
  bundle; `:app` should not reimplement registry/resolver/planner construction.
- WebView is an `ActivityRoute`; normal screens are `ComposeRoute`.
- Keep `RouteStack` and `RouteCommand` as router execution artifacts where possible. Feature UI should prefer route events or route intents so callers do not need to know whether a destination is Activity-backed or Compose-backed.
- Kotlin route contracts that feature modules must implement stay open
  interfaces. Use `sealed interface` only for closed families owned by one
  module, such as feature route events or core router execution commands. Do not
  seal core route/spec/registry contracts only to make `when` exhaustive.
- The product shell owns scheme/host/base path values and feature route/event
  registrations. `:core:app` owns the reusable bundle assembly from those
  values. `:app` owns external intent entry, pending deep-link handoff, and
  concrete platform launch binding. Feature API owns route data, top-level route
  metadata when needed, deep-link specs, and public route events.
- For Nav3-style deep links, `MainActivity` is the intent entrypoint, `:app`
  builds the synthetic route plan, and the app/base shell maps Compose route keys
  to screen content. Feature impl modules should not parse URLs or own the
  synthetic stack.
- If a deep link can enter an existing task or a new task, test Back and Up behavior for both paths before treating the route as stable.
- New dynamic screens should add:
  - route data class under the owning feature API `route/` package
  - `DeepLinkSpec` under `deeplink/` when externally addressable
  - route event under `event/` when opened from UI
  - product-shell router registration when external links or top-level navigation need it
  - `NotmidAppRouterTest` and `NotmidDeepLinkResolverTest`

## Verification

Choose the narrowest useful gate:

```bash
./gradlew :app:compileDebugKotlin
./gradlew test
git diff --check
```

For design-system or route changes, run all three before final response.
