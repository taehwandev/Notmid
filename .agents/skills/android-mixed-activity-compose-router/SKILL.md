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
  owns route data, top-level route metadata when needed, deep-link specs, and public route events
  split by role once more than one contract family exists:
    route/
    deeplink/
    event/
    destination/ or activity/ only when those are real caller-facing families

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

`RouteStack` is useful as an ordered Compose back stack, but it should usually
sit inside a `RoutePlan`, not be something leaf UI constructs directly.

## Route Plan

For Compose-only or simple apps, an ordered stack is often enough:

```text
[Feed]
[Feed, ClipDetail(clipId)]
[Profile, ProfileSettings]
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

`RouteStack` entries are Compose route keys only. Activity-backed destinations
belong in `RoutePlan.activityRoutes`.

## Contract Closure Rule

Kotlin `sealed interface` is an implementation-closed hierarchy. Direct
implementations must live in the same package and module as the sealed contract,
so it is the wrong default for cross-module route APIs.

Use `sealed interface` only when the module owns the whole closed family:

- app/core-owned execution commands such as `RouteCommand`.
- feature-owned public event families such as `FeedRouteEvent`.
- local result or policy families that should be exhaustively handled.

Keep cross-module extension contracts open with normal interfaces:

- `Route`, `ComposeRoute`, `ActivityRoute`, `TopLevelRoute`.
- `DeepLinkSpec`, `RouteRegistry`, `Router`, and route graph contracts.

Do not make an interface sealed when feature API modules, app modules, or future
platform adapters must add implementations independently.

Do not use Kotlin sealing to get an exhaustive `when` at the cost of closing a
real extension point:

- Do not seal `Route`, `ComposeRoute`, `ActivityRoute`, or `TopLevelRoute`.
  Adding a destination should be a feature API change, not a core router edit.
- Do not seal `DeepLinkSpec` or registry contracts. Adding a deep link should add
  a feature-owned spec and app registration, not change the core hierarchy.
- Do not make the core `RouteEvent` sealed. Keep it as an open marker; each
  feature can define its own sealed event family when it owns every event.
- Do not put feature route objects, Activity keys, or product destinations inside
  a sealed core list to satisfy compiler exhaustiveness.
- Do not add route-spec abstractions only to work around a sealed contract. Add
  a spec only when a caller needs pattern metadata without creating a route.

Extend the router by choosing the correct seam:

- New Compose destination: add a `ComposeRoute` in the owning feature API module,
  then map it to content in app, base shell, or feature implementation.
- New Activity-backed destination: add an `ActivityRoute` plus Activity key in
  the owning feature API module, then launch it from the app Activity dispatcher.
- New external URL or notification target: add a feature `DeepLinkSpec` that
  returns a `RoutePlan`, then register it in the app route graph.
- New in-app callback: add or extend the feature-owned sealed `RouteEvent`
  family, then map it to a `RoutePlan` in app code.
- New execution kind that every router must understand: add a new
  `RouteCommand` subtype in the core/router command module and update every
  executor, assertion helper, and test exhaustively.

If a sealed hierarchy needs third-party or cross-module extension later, that is
a contract change. Either keep the top-level contract open, or introduce a
same-module open child marker intentionally and document what exhaustiveness is
lost.

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
- keep current-task versus new-task stack policy in the app entrypoint or route
  coordinator.
- make Back and Up behavior explicit in tests for both current-task and
  new-task launches.

For the Navigation 3 advanced deep-link recipe:

- Treat the `Activity` as the external deep-link entrypoint. It reads the
  incoming intent data and passes only a normalized deep-link request to the
  route coordinator.
- The route coordinator builds a synthetic `RoutePlan` before rendering. Compose
  destinations carry a `RouteStack`; Activity-backed destinations carry
  `ActivityRoute` launch requests. Do not let feature screens reconstruct
  deep-link paths.
- The app or base shell owns the Compose route-to-entry mapping. In Nav3 terms,
  feature API modules provide navigation keys, while feature impl or app/base
  modules provide entry builders/content.
- Activity-local navigation can still use its own Nav3 back stack, but the
  top-level app router should pass it an `ActivityRoute` launch request instead
  of mixing that local stack into the app Compose stack.
- Current-task and new-task launches are different acceptance paths. Verify
  Back and Up behavior for both when deep links can enter an existing task or a
  freshly created task.
- Do not add AndroidX Navigation types to pure router API modules. Add the
  Nav3 bridge at the app/base or Android-specific router boundary.

## Feature API Contract

Every externally addressable destination needs a stable contract:

```kotlin
data class ClipDetailRoute(val clipId: String) : ComposeRoute

object ClipDeepLinkSpec : DeepLinkSpec {
    override fun match(request: DeepLinkRequest): DeepLinkMatch? = ...
}

sealed interface FeedRouteEvent : RouteEvent {
    data class ClipRequested(val clipId: String) : FeedRouteEvent
}
```

Do not keep unrelated route, deep-link, event, destination id, and Activity key
contracts in one catch-all navigation contract file. Prefer one public contract
per file and group files by import boundary: `route`, `deeplink`, `event`,
`destination`, or `activity`. Only keep a single feature contract file when it
contains exactly one small public contract family and no second caller needs a
separate import surface.

Activity-backed destinations use the same contract style:

```kotlin
data class WebViewRoute(
    val url: String,
    val title: String? = null,
) : ActivityRoute
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
RouteStack.entries -> List<NavKey> for Compose destinations
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
- deep link resolves to the expected route plan; Compose destinations assert the
  expected ordered stack
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
- Feature API modules own route keys, top-level route metadata when needed,
  deep-link specs, and public route events. Do not add a separate route-spec
  abstraction unless a real caller consumes pattern metadata without creating a
  route instance.
- App owns route graph assembly, auth gating, host/scheme policy, pending route
  consumption, and Activity launch dispatch.
- Core router contracts are pure and testable without Android framework classes.
- Callers do not need to know whether a destination is Compose or
  Activity-backed.
- Deep links resolve to ordered plans, not only a top destination.
- Activity routes are terminal by default and do not accidentally clear the
  Compose stack.
- Tests cover both in-app events and external deep links for the same target.
