---
title: Router And WebView Contract
audience: Android engineers and AI agents
purpose: Notmid 라우터와 WebView ActivityRoute를 core/core-app/feature 경계로 재정의한다.
status: draft
owner: notmid Android architecture
source_of_truth: docs/specs/android-commonization
last_verified: 2026-06-16
applies_to: core router, app router, feature route contracts, WebView
related_pages:
  - docs/specs/notmid-router-architecture.md
  - llm-wiki/routing-deeplinks.md
  - 07-state-assertions-testing.md
external_sources:
  - https://developer.android.com/guide/navigation/navigation-3/recipes/deeplinks-advanced
---

# Router And WebView Contract

## Current State

Notmid already has the right foundation:

```text
:core:router:api
  Route
  ComposeRoute
  ActivityRoute
  TopLevelRoute
  DeepLinkSpec
  DeepLinkRequest
  RouteStack
  RoutePlan
  RouteEvent
  Router
  RouteEventSink

:core:router:impl
  DefaultRouteRegistry

:feature:*:api
  route/ typed route contracts
  deeplink/ deep-link specs
  event/ route events

:app
  AppRouter
  AppRouteCoordinator
  AppDeepLinkResolver
  NotmidRouteGraph
  AppActivityRouteLauncher

:feature:webview:api
  route/WebViewRoute
  deeplink/WebViewDeepLinkSpec
  activity/WebViewActivityKeys

:feature:webview:impl
  NotmidWebViewActivity
```

This is already more appropriate for Compose than the reference project's Activity/Fragment `JourneyGuidance` contract.

## Decision

Keep Notmid's route contract shape. Add testing and app-runtime extraction gradually.

Immediate target:

```text
:core:router:assertions
```

First router slice scope:

- `ComposeRoute` typed keys that the base/app shell maps to Compose content.
- `ActivityRoute` launch requests that do not mutate the Compose back stack by default.
- deep link input from `Activity` intent or feedback callbacks into a `RoutePlan`.
- route callback/event recording for feature UI tests.
- Nav3-style back stack ownership: the app owns the ordered route key list and the base shell resolves Compose keys to content.
- Nav3 advanced deep-link behavior: app entry builds a synthetic back stack and treats current-task and new-task launches as separate Back/Up acceptance paths.

Excluded from the first router slice:

- WebView runtime and URL policy.
- WebView Activity implementation.
- browser fallback and external URL handling.
- WebView-specific deep-link hardening.

Later optional target:

```text
:core-app:router:api
:core-app:router:impl
:core-app:router:assertions
```

Only create `core-app:router` when Android `ActivityRoute` launching, pending activity requests, auth-gated route deferral, or multiple app shells need a reusable runtime contract.

## Contract Ownership

### Feature API

Feature API owns:

- route data.
- route spec.
- deep-link spec.
- route event.
- small public model required to build a route.

Feature API does not own:

- app web host/scheme/base path.
- auth-gating decision.
- Activity launching.
- another feature's stack shape.

Example:

```text
feature:feed:api
  FeedRoute
  ClipDetailRoute
  FeedDeepLinkSpec
  ClipDeepLinkSpec
  FeedRouteEvent.ClipRequested
```

### App

App owns:

- route graph assembly.
- app host/scheme/base path.
- top-level default route.
- cross-feature route event mapping.
- ActivityRoute launch dispatch.
- auth-gated route deferral.

Example:

```text
FeedRouteEvent.ClipRequested(clipId)
  -> app maps to [FeedRoute, ClipDetailRoute(clipId)]
```

### Core Router

Core router owns:

- pure route contracts.
- route stack/plan invariants.
- registry/matching helpers.
- URL parsing without app-specific host policy.
- generic test assertions for ComposeRoute, ActivityRoute, deep-link route plans, and route callbacks.

Core router does not own:

- Notmid's host.
- `Context.startActivity`.
- login policy.
- product-specific default route.
- Compose entry providers, NavDisplay, or Activity-local Nav3 rendering.

## Nav3 Advanced Deep-Link Application

The Android Navigation 3 advanced deep-link recipe is a better fit than a
simple "URL opens one destination" model. For Notmid, apply it as:

```text
Activity intent data
  -> app deep-link request
  -> app host/base-path validation
  -> feature deep-link spec
  -> synthetic RoutePlan
  -> base shell Compose route mapping and/or ActivityRoute launch request
```

Rules:

- `MainActivity` is the external deep-link entrypoint. It should normalize
  `Intent.ACTION_VIEW` / `intent.data` into a request and hand that request to
  the app route coordinator.
- The app route coordinator builds the synthetic stack before rendering. Feature
  impl screens should only emit callbacks/events.
- Base shell code maps `ComposeRoute` keys to Compose content. Feature API owns
  route keys; feature impl or the base shell owns entry/content builders.
- `ActivityRoute` is an execution request. It can launch an Activity that owns a
  local Nav3 back stack, but that local stack must not be mixed into the app
  Compose stack.
- Current-task and new-task entry paths must be verified separately. Back should
  pop the synthetic stack when possible; Up should follow the app's explicit
  parent-stack policy.

## Router Assertions

`core:router:assertions` should provide WebView-free helpers:

```text
RecordingRouter
RecordingRouteEventSink
RoutePlanSubject
RouteStackSubject
TestComposeRoute
TestActivityRoute
TestRouteEvent
```

Required observations:

- number of navigate calls.
- last `RouteCommand`.
- last `RoutePlan`.
- emitted route events.
- pending ActivityRoute list.
- Compose route stack entries and Activity routes through `RoutePlan`.

Example test intent:

```kotlin
val router = RecordingRouter()
router.navigate(RouteCommand(RouteStack.of(FeedRoute, ClipDetailRoute("clip-1"))))

router.assertLastStack {
    hasEntries(FeedRoute, ClipDetailRoute("clip-1"))
}
```

The assertions module must depend on `:core:router:api` only. It must not depend on `:app` or feature implementation modules.

## WebView Current Contract

Current Notmid WebView is intentionally Activity-backed.

Keep that decision.

Reasons:

- WebView lifecycle is resource-heavy.
- file chooser and permissions are Activity-shaped.
- back history needs Activity-level handling.
- fullscreen media is easier at Activity boundary.
- external auth pages may have lifecycle behavior that should not live inside a Compose destination.

## WebView Next Contract

Short term:

```text
:feature:webview:api
  WebViewRoute(url, title, mode, javaScriptEnabled)
  WebViewDeepLinkSpec

:feature:webview:impl
  NotmidWebViewActivity
  Intent factory
```

Add hardening before feature growth:

- `http`/`https` only.
- URL allowlist or explicit external mode policy.
- JavaScript default should be mode-based, not always true.
- no arbitrary JavaScript interface until a bridge contract exists.
- file chooser callback lifecycle cleanup.
- WebView destroy path stays explicit.

Long term, if WebView becomes reusable:

```text
:core-app:webview:api
  WebViewRequest
  WebViewMode
  WebViewState
  WebViewNavigator
  WebFileChooserRequest
  WebViewSecurityPolicy

:core-app:webview:impl
  Compose/Android WebView holder
  WebViewClient/ChromeClient adapters
  file chooser and permission adapters

:core-app:webview:assertions
  FakeWebViewNavigator
  RecordingWebViewEventSink
  WebViewSecurityPolicyAssertions
```

Keep `:feature:webview:api` as Notmid's route target even if reusable runtime moves to `core-app:webview`.

## Auth-Gated Routes

Auth gating belongs to app shell or app route coordinator, not feature route data.

Pattern:

```text
feature impl emits route event
app resolves route plan
app checks auth policy for protected destinations/actions
app either executes plan or records deferred route/action
login success resumes deferred route/action
```

Feature UI can know that an action requires auth for rendering disabled/login prompts, but it should not construct login deep links by itself.

## Deep-Link Rules

Canonical host/base path remains app-owned:

```text
https://thdev.app/notmid
```

Feature deep-link specs should match route path segments after app strips the base path.

Good:

```text
AppDeepLinkResolver validates scheme/host/base path
NotmidRouteGraph resolves featurePathSegments
Feature DeepLinkSpec maps segments to RoutePlan
```

Bad:

```text
feature impl parses thdev.app
feature api hardcodes every app-level canonical URL detail
core router knows Notmid host
```

## Migration Steps

1. Add `:core:router:assertions`.
2. Move duplicated router test helpers from app tests into assertions.
3. Add assertions for `RoutePlan.compose` and `RoutePlan.activity`; do not mix
   `ActivityRoute` into `RouteStack`.
4. Add `RecordingRouteEventSink` and use it in feature route event tests.
5. Harden WebView route validation without moving modules.
6. Add `core-app:webview` only when a second WebView runtime surface appears or WebView state grows beyond `NotmidWebViewActivity`.

## Verification

Router:

```bash
./gradlew :core:router:api:test
./gradlew :core:router:impl:test
./gradlew :app:test --tests '*AppRouterTest'
./gradlew :app:test --tests '*AppDeepLinkResolverTest'
```

WebView:

```bash
./gradlew :feature:webview:impl:compileDebugKotlin
./gradlew :app:compileDebugKotlin
```

If WebView security policy changes, add focused tests for URL scheme, allowed host, JavaScript mode, and route parsing.
