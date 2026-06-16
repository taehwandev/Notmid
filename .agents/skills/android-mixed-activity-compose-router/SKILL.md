---
name: android-mixed-activity-compose-router
description: Use when designing, reviewing, or refactoring Android navigation where Compose destinations, Activity or Intent destinations, deep links, notifications, auth-gated routes, or future Nav3 back stacks need one caller-facing route contract across apps.
---

# Android Mixed Activity And Compose Router

## Purpose

Make navigation easy for callers when an Android app mixes Compose screens,
separate Activities, WebView, external intents, deep links, notifications, auth
gates, or a future Navigation 3 back stack.

The rule is:

```text
caller says where to go
app router decides how to go
```

Feature code should not need to know whether a destination is implemented as a
Compose route, Activity, Fragment, WebView, Custom Tab, or Nav3 `NavKey`.

## Start Here

1. Run `git status --short` and keep unrelated user changes out of the write set.
2. Inspect the smallest relevant navigation files:

   ```bash
   rg --files | rg 'router|route|navigation|deeplink|deep-link|NavHost|NavController|Activity'
   rg -n "Route|Router|DeepLink|Intent|NavController|NavHost|ActivityRoute|NavKey"
   ```

3. Identify the current execution surfaces:
   - Compose in the current Activity
   - separate app Activities
   - external intents, chooser, file picker, browser, Custom Tabs
   - app links, custom schemes, notification payloads, web redirects
   - auth/session-gated destinations
4. Identify the current caller surface:
   - direct `NavController` calls
   - raw `Intent` construction
   - string route commands
   - route events
   - typed destination contracts
5. Prefer moving callers to route events or route intents before changing the
   execution engine.

## Target Shape

Use this shape and shrink names to the app:

```text
feature implementation
  emits RouteEvent or RouteIntent

feature api
  owns route data, route specs, deep-link specs, and public route events

core router api
  owns pure contracts: Route, ComposeRoute, ActivityRoute, RouteIntent,
  RoutePlan, RouteStack, RouteEvent, DeepLinkRequest, DeepLinkMatch

core router impl
  owns registry, path matching, route intent resolution helpers

app
  owns route graph assembly, route coordinator, auth/deferred routing,
  host/scheme policy, pending deep-link consumption, Compose stack state,
  Activity or Intent launch dispatch
```

Keep `Context`, `Intent`, `Activity`, `NavController`, app-link host policy, and
auth/session checks out of pure router API modules. They belong to the app layer
or an Android-specific execution adapter.

## Public Caller Contract

Prefer high-level APIs:

```kotlin
router.dispatch(RouteIntent.OpenClip(clipId))
router.dispatch(RouteIntent.OpenWeb(url, title))
router.dispatch(RouteIntent.OpenDeepLink(uri))
```

or, when the app already uses feature route events:

```kotlin
onRouteEvent(FeedRouteEvent.ClipRequested(clipId))
onRouteEvent(ProfileRouteEvent.SettingsRequested)
```

Keep low-level execution artifacts app-owned:

```text
RouteIntent or RouteEvent -> RoutePlan -> RouteExecutor
```

`RouteStack` is useful, but it should usually be the router's resolved plan, not
something leaf UI constructs directly.

## Route Plan

For Compose-only or simple apps, an ordered stack is often enough:

```text
[Feed]
[Feed, ClipDetail(clipId)]
[Profile, ProfileSettings]
[WebViewRoute(url)]
```

When Activity and Compose execution can happen together, use an explicit plan:

```kotlin
data class RoutePlan(
    val composeStack: RouteStack? = null,
    val activityRoutes: List<ActivityRoute> = emptyList(),
    val policy: RoutePolicy = RoutePolicy.Default,
)
```

Do not overload one list with unclear semantics. A terminal Activity route
normally queues an Activity launch without replacing the current Compose stack
unless the plan explicitly provides a new Compose stack.

## Execution Rule

Only the app route coordinator decides execution:

```text
RouteIntent / RouteEvent / DeepLinkRequest
  -> route graph or registry
  -> RoutePlan
  -> auth/session/deferred route policy
  -> Compose stack mutation and/or Activity launch
```

Recommended behavior:

- `ComposeRoute` plans update the Compose back stack.
- terminal `ActivityRoute` plans queue Activity or Intent launch.
- Activity launch does not mutate Compose stack by default.
- deep links and notifications resolve through the same registry as in-app
  events.
- auth-required plans become deferred routes when signed out.
- pending deep links and Activity launch requests are consumed once.

## Deep Links

Normalize every external entrypoint into one request type before matching:

```kotlin
data class DeepLinkRequest(
    val rawUri: String,
    val source: DeepLinkSource,
    val referrer: String? = null,
)
```

Resolve to a match with metadata:

```kotlin
data class DeepLinkMatch(
    val plan: RoutePlan,
    val canonicalPath: String,
    val matchedSpecId: String,
    val authPolicy: RouteAuthPolicy = RouteAuthPolicy.AllowSignedOut,
)
```

Feature API modules own path patterns and typed argument creation. The app owns
allowed schemes, hosts, base paths, environment host policy, notification/web
redirect source handling, auth/deferred routing, and external Activity dispatch.

Do not parse app web URLs or custom schemes inside feature implementation
screens.

For Navigation 3-style apps, follow the same split:

- parse the incoming URL or intent into a typed route key or route plan.
- build the synthetic back stack from the resolved destination contract.
- keep Task stack policy in the app entrypoint or route coordinator.
- make Back and Up behavior explicit in tests for both current-task and
  new-task launches.

## Feature API Contract

Every externally addressable destination needs a stable contract:

```kotlin
data class ClipDetailRoute(val clipId: String) : ComposeRoute

object ClipDetailRouteSpec : RouteSpec<ClipDetailRoute> {
    fun create(clipId: String): ClipDetailRoute = ClipDetailRoute(clipId)
}

object ClipDeepLinkSpec : DeepLinkSpec {
    override fun match(request: DeepLinkRequest): DeepLinkMatch? = ...
}

sealed interface FeedRouteEvent : RouteEvent {
    data class ClipRequested(val clipId: String) : FeedRouteEvent
}
```

Activity-backed destinations use the same contract style:

```kotlin
data class WebViewRoute(
    val url: String,
    val title: String? = null,
) : ActivityRoute

object WebViewRouteSpec : ActivityRouteSpec<WebViewRoute> {
    fun create(url: String, title: String? = null): WebViewRoute = ...
}
```

The feature implementation renders the Compose screen or owns the Activity. It
should not be imported by other feature implementation modules only for
navigation.

## Reference-App Rule

When borrowing from a large Android reference app, copy the boundary lesson, not
the entire implementation.

Borrow:

- caller-facing journey or route contracts in feature API modules
- app-level registry or route graph assembly
- deep-link declarations near the destination contract
- URI normalization into path nodes and params before matching
- auth/session state transforming or deferring the final route plan
- tests that assert resolved route output

Avoid copying unless the target app already needs it:

- KSP or generated route guidance
- Hilt/Dagger module registration as a router requirement
- Activity-first `Intent` builders for Compose-only destinations
- product-specific user roles, levels, screen names, or scheme conventions
- feature screens consuming raw scheme strings for cross-feature routing

## Nav3 Compatibility

Keep route contracts typed so the app can bridge later:

```text
Route / ComposeRoute -> NavKey
RouteStack.entries -> List<NavKey>
DeepLinkMatch.plan -> synthetic Nav3 back stack
```

Do not put AndroidX Navigation types in the pure router API until the app has
adopted Nav3 as the execution engine. Add a bridge in the app or router
implementation boundary.

The useful Nav3 idea is ownership of the back stack as data. A route stack that
is already ordered, typed, and serializable can become `NavKey` entries later
without rewriting feature callers.

## Migration Order

1. Inventory direct `Intent`, `NavController`, and string route calls.
2. Define typed route contracts in feature API modules.
3. Add route events or route intents for caller-facing actions.
4. Add a central route graph or registry in the app layer.
5. Add a route coordinator that resolves events, intents, and deep links.
6. Move Activity launching behind an app-level dispatcher.
7. Move host, scheme, and base path policy out of feature code.
8. Replace feature implementation navigation calls with route events/intents.
9. Add tests for route resolution and Activity/Compose execution split.
10. Add a Nav3 adapter or generated registration only after the contract is
    stable.

## Verification

Minimum focused tests:

- route event resolves to the expected route plan
- deep link resolves to the expected ordered stack
- unknown deep link returns no match
- dynamic route arguments are preserved
- ActivityRoute queues an Activity launch without replacing Compose stack
- pending ActivityRoute or deep link is consumed once
- auth-required route becomes deferred when signed out
- feature implementation modules do not depend on other feature implementations

For docs-only work, verify paths and references. For code changes, run the
narrowest compile/test target that covers route contracts and the app route
coordinator.

## Review Checklist

- Feature implementation modules emit route events/intents instead of starting
  another feature or Activity directly.
- Feature API modules own route specs, activity specs, deep-link specs, and
  public route events.
- App owns route graph assembly, auth gating, host/scheme policy, pending route
  consumption, and Activity launch dispatch.
- Core router contracts are pure and testable without Android framework classes.
- Callers do not need to know whether a destination is Compose or
  Activity-backed.
- Deep links resolve to ordered plans, not only a top destination.
- Activity routes are terminal by default and do not accidentally clear the
  Compose stack.
- Tests cover both in-app events and external deep links for the same target.
