---
title: Router And WebView Contract
audience: Android engineers and AI agents
purpose: Notmid 라우터와 WebView ActivityRoute를 `core`/`core/app`/feature 경계로 재정의한다.
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
  registry/DefaultRouteRegistry
  event/DefaultRouteEventPlanner
  deeplink/DefaultDeepLinkResolver
  deeplink/DeepLinkUrlPolicy
  deeplink/UriDeepLinkRequestParser
  deeplink/StaticRouteDeepLinkSpec
  deeplink/PrefixRouteDeepLinkSpec

:core:app
  router/config/AppRouterBundleConfig
  router/config/AppDeepLinkUrlConfig
  router/config/DefaultAppRouterBundle
  router/planner/AppRoutePlanner
  router/planner/DefaultAppRoutePlanner
  router/deeplink/AppDeepLinkResolver
  router/deeplink/DefaultAppDeepLinkResolver
  router/runtime/AppRouterRuntime
  router/runtime/DefaultAppRouterRuntime
  router/runtime/PendingActivityRouteRequest
  router/runtime/rememberAppRouterRuntime
  router/activity/ActivityRouteLauncher
  router/activity/ActivityRouteLaunchHandler
  router/activity/DefaultActivityRouteLauncher
  router/activity/ActivityRouteLauncherEffect
  router test source owns module-local FakeAppDeepLinkResolver,
  and RecordingActivityRouteLauncher until external reuse justifies an
  assertions module

:feature:*:api
  route/ typed route contracts
  deeplink/ deep-link specs
  event/ route events

:feature:notmid:impl
  NotmidRouteGraph
  NotmidRouteEventMapper
  rememberNotmidAppRouter

:feature:webview:api
  route/WebViewRoute
  deeplink/WebViewDeepLinkSpec
  activity/WebViewActivityKeys

:feature:webview:impl
  NotmidWebViewActivity
  WebViewActivityRouteLaunchHandler

:app
  MainActivity pending deep-link handoff
  DefaultActivityRouteLauncher binding with feature launch handlers

:feature:webview:impl
  NotmidWebViewActivity
```

This is already more appropriate for Compose than the reference project's Activity/Fragment `JourneyGuidance` contract.

## Decision

Keep Notmid's route contract shape. `core:router` remains pure Kotlin, while
drop-in Android/Compose execution now lives in the `:core:app` router package.

Current reusable runtime:

```text
:core:app
```

An app should be able to add this module, provide product route registrations,
event handlers, host/base-path values, and Activity launch handlers, then get
the standard runtime behavior:

- `ComposeRoute` typed keys that the base/app shell maps to Compose content.
- `ActivityRoute` launch requests that do not mutate the Compose back stack by default.
- deep link input from `Activity` intent or feedback callbacks into a `RoutePlan`.
- route callback/event recording for feature UI tests.
- Nav3-style back stack ownership: the router runtime owns the ordered route key list and the product/base shell resolves Compose keys to content.
- Nav3 advanced deep-link behavior: app entry hands off external URI input, the product route bundle builds a synthetic back stack, and current-task/new-task launches remain separate Back/Up acceptance paths.

Still product-shell owned:

- Notmid route registrations.
- Notmid event-to-stack mapping.
- app host/scheme/base path values passed into `AppRouterBundleConfig`.
- auth-gated route deferral policy when added.

Still app-owned:

- Activity intent/deep-link handoff into the router runtime.
- concrete ActivityRoute launcher binding.

Excluded from this runtime:

- WebView runtime and URL policy.
- WebView Activity implementation.
- browser fallback and external URL handling.
- WebView-specific deep-link hardening.

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

### Product Shell And App

Product shell owns:

- route registrations through `AppRouterBundleConfig`.
- app host/scheme/base path values.
- top-level default route.
- cross-feature route event mapping.

App owns:

- external intent/deep-link handoff.
- concrete ActivityRoute launcher binding.
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

### Core App Router Package

Core app router owns reusable app-runtime execution:

- `AppRouterRuntime` state holder for current Compose `RouteStack`.
- pending `ActivityRoute` request queue and consume-on-success behavior.
- `DefaultAppRoutePlanner` that dispatches route commands, route event planner
  results, and deep-link resolver results.
- `DefaultAppDeepLinkResolver` as a thin adapter over a core router
  `DeepLinkResolver`.
- `DefaultAppRouterBundle` that creates default registry, deep-link resolver,
  route-event planner, app planner, and runtime from an `AppRouterBundleConfig`.
- `DefaultActivityRouteLauncher` and `ActivityRouteLaunchHandler` for reusable
  ActivityRoute dispatch without an app-local `when` block.
- `ActivityRouteLauncherEffect` for Compose lifecycle-safe pending launch
  consumption.

Core app router does not own:

- feature route keys or events.
- Notmid destination ids.
- WebView `Intent` construction.
- auth/session/deferred-route product policy.
- DI container bindings.

## Nav3 Advanced Deep-Link Application

The Android Navigation 3 advanced deep-link recipe is a better fit than a
simple "URL opens one destination" model. For Notmid, apply it as:

```text
Activity intent data
  -> product-shell AppRouterBundleConfig
  -> :core:app DefaultAppRouterBundle
  -> core router DefaultDeepLinkResolver
  -> product-provided host/base-path values
  -> product route registry and feature deep-link specs
  -> synthetic RoutePlan
  -> :core:app DefaultAppRouterRuntime
  -> base shell Compose route mapping and/or ActivityRoute launch effect
```

Rules:

- `MainActivity` is the external deep-link entrypoint. It should normalize
  `Intent.ACTION_VIEW` / `intent.data` into a URI string and hand it to the
  app-router runtime. Core router impl owns URI parsing; the app provides
  host/base-path values and registered specs.
- The app route planner builds the synthetic stack before rendering. Feature
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
:core:app
  webview API package:
    WebViewRequest
    WebViewMode
    WebViewState
    WebViewNavigator
    WebFileChooserRequest
    WebViewSecurityPolicy
  webview implementation package:
    Compose/Android WebView holder
    WebViewClient/ChromeClient adapters
    file chooser and permission adapters
  test source:
    FakeWebViewNavigator
    RecordingWebViewEventSink
    WebViewSecurityPolicyAssertions
```

Keep `:feature:webview:api` as Notmid's route target even if reusable runtime moves into the `:core:app` webview package.

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

Feature deep-link specs should match route path segments after the core router
resolver strips the app-provided base path.

Good:

```text
DefaultAppRouterBundle composes AppRouterBundleConfig into registry/resolver/planner/runtime defaults
DefaultDeepLinkResolver validates scheme/host/base path from DeepLinkUrlPolicy
product-shell route graph registers feature DeepLinkSpec values
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
2. Add `:core:app` as the single Android/Compose runtime module.
3. Move pure route test helpers into `:core:router:assertions`; keep app-runtime
   fakes in `:core:app` test source until external reuse appears.
4. Add assertions for `RoutePlan.compose` and `RoutePlan.activity`; do not mix
   `ActivityRoute` into `RouteStack`.
5. Add `RecordingRouteEventSink` and use it in feature route event tests.
6. Move reusable app runtime state, deep-link URI normalization, and pending
   ActivityRoute consume effect into the `:core:app` router package.
7. Keep Notmid graph, feature event mapping, and WebView Activity launch in
   `:app`.
8. Harden WebView route validation without moving modules.
9. Add a `:core:app` webview package only when a second WebView runtime surface appears or WebView state grows beyond `NotmidWebViewActivity`.

## Verification

Router:

```bash
./gradlew :core:router:api:test
./gradlew :core:router:impl:test
./gradlew :core:app:testDebugUnitTest
./gradlew :app:test --tests '*AppRouterTest'
./gradlew :app:test --tests '*AppDeepLinkResolverTest'
```

WebView:

```bash
./gradlew :feature:webview:impl:compileDebugKotlin
./gradlew :app:compileDebugKotlin
```

If WebView security policy changes, add focused tests for URL scheme, allowed host, JavaScript mode, and route parsing.
