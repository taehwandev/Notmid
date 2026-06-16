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

- `:app` owns Android entry points, app router, deep-link resolver, activity route launcher, and top-level composition.
- `:core:designsystem` owns `NotmidTheme`, tokens, Material3 wrappers, reusable Notmid components, and Liquid Glass primitives.
- `:core:model` remains pure Kotlin models. No Compose, Android, `Color`, or `Dp`.
- `:core:domain` owns repository contracts and use cases.
- `:core:data` owns fake/static implementations and mapping into domain models.
- `:core:router:api` stays pure Kotlin route contracts.
- `:core:router:impl` owns default route registry/matching, not Android Activity launching.
- `:feature:*:api` owns feature route contracts, deep-link specs, and route events.
- `:feature:*:impl` owns Compose screens and emits events. It must not depend on another feature impl.
- `apps/api` owns HTTP API, token verification boundary, and future persistence integrations.
- `apps/web` owns React/Next.js routes and shareable web surfaces.
- `packages/contracts` owns canonical web route helpers, TypeScript DTOs, and deterministic fixtures.
- `packages/api-client` owns typed fetch helpers. It must not own product state or secrets.

## Design-System Rules

- Prefer `Notmid*` wrappers over direct Material3 imports in feature/app modules.
- Use `NotmidText`, `NotmidButton`, `NotmidTextField`, `NotmidScaffold`, `NotmidTopAppBar`, and related wrappers.
- Use `NotmidBottomNavigation` for app bottom nav. Do not wire `LiquidGlassBottomNavigation` directly in features.
- Feature product cards may adapt models into generic design components:
  - `NotmidClipCard -> NotmidGradientSummaryCard`
  - `NotmidPlaceCard -> NotmidGradientHeroCard`
- Keep media-derived palettes in feature UI models; keep reusable styling in `:core:designsystem`.

## Routing Rules

- For mixed Activity and Compose navigation changes, apply `.agents/skills/android-mixed-activity-compose-router/SKILL.md` first. Use `.agents/skills/glassnavlab-android-stewardship/references/mixed-activity-compose-router-reference.md` only for the Notmid/FirFin comparison notes. The caller should say where to go; the app router decides whether that becomes a Compose stack mutation, Activity launch, deferred auth route, or future Nav3 back stack.
- Deep links must resolve to ordered stacks, not just a top destination.
- Feature impl modules emit events; `:app` converts events into route stacks.
- WebView is an `ActivityRoute`; normal screens are `ComposeRoute`.
- Keep `RouteStack` and `RouteCommand` as router execution artifacts where possible. Feature UI should prefer route events or route intents so callers do not need to know whether a destination is Activity-backed or Compose-backed.
- App owns scheme/host/base path policy, auth/deferred routing, pending deep-link consumption, and Activity launch dispatch. Feature API owns route data, route specs, deep-link specs, and public route events.
- New dynamic screens should add:
  - route data class in feature API
  - `RouteSpec`
  - `DeepLinkSpec`
  - route event when opened from UI
  - `NotmidRouteGraph` registration
  - `AppRouterTest` and `AppDeepLinkResolverTest`

## Verification

Choose the narrowest useful gate:

```bash
./gradlew :app:compileDebugKotlin
./gradlew test
git diff --check
```

For design-system or route changes, run all three before final response.
