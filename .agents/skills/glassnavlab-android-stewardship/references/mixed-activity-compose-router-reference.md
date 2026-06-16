# Notmid Mixed Router Reference Notes

This is the local comparison companion for the reusable
`.agents/skills/android-mixed-activity-compose-router/SKILL.md` skill. Load that
standalone skill for cross-app rules. Load this reference only when Notmid work
needs the local reference-project interpretation that informed the router cleanup.

## Local Intent

The original reference-style question was:

```text
When Activity routing and Compose navigation are mixed, how can callers navigate
comfortably without knowing the execution surface?
```

The answer for Notmid is:

```text
feature emits a route contract
app resolves that contract into Compose stack mutation or Activity launch
```

Keep `RouteStack` useful, but treat it as the Compose back-stack portion of a
router-resolved `RoutePlan`, not as the normal feature caller API.

## Reference Pieces That Were Inspected

- `core/router/router-api/.../Router.kt`
- `core/router/router-api/.../JourneyGuidance.kt`
- `core/router/router-api/.../DeepLinkJourneyGuidance.kt`
- `core/router/router/src/.../ActivityRouter.kt`
- `core/router/router/src/.../ServiceRouter.kt`
- `core/router/router/src/.../JourneyMapper.kt`
- `core/lifecycle/.../LifecycleActivity.kt`
- `core/lifecycle/.../GenerateJourneyGuidance.kt`
- `core-app/scheme/scheme/.../ReferenceScheme*.kt`
- `core/collector/bundle-collector-stream-api/.../BundleSchemeData.kt`
- `feature/splash/splash/.../SchemeViewModel.kt`
- `feature/holder/main/deeplink-holder/.../DeepLinkViewModel.kt`

## Reference Mapping

```text
Reference JourneyGuidance
  -> caller-facing destination contract

Reference DeepLinkJourneyGuidance
  -> destination can be reached from a scheme/deep link

Reference JourneyMapper
  -> app-level route registry

Reference findByHostNames(level, hostList, value)
  -> resolve normalized URI nodes into one or more execution targets

Reference List<Intent>
  -> Notmid RoutePlan with a Compose RouteStack and/or ActivityRoute launch

Reference groupActivity(*intents).visit()
  -> app route coordinator executes Compose stack and/or Activity routes

Reference BundleSchemeData(nodeList, params)
  -> DeepLinkRequest plus typed route args
```

## What Notmid Should Borrow

- Feature API owns route/event/deep-link contracts.
- App gathers registered contracts and owns execution.
- Deep links resolve to an ordered plan, not only a top destination.
- Auth/session state can transform or defer the route plan.
- WebView stays Activity-backed when lifecycle, file chooser, permissions,
  reload, history, or fullscreen media need an Activity boundary.
- In-app events and external deep links for the same destination share the same
  route resolution path.

## What Notmid Should Not Copy

- KSP-generated guidance unless route registration becomes repetitive enough to
  justify generation.
- Hilt/Dagger as a router requirement.
- Activity-first `Intent` builders for normal Compose destinations.
- The reference project's role-specific `level` names or banking/product scheme constants.
- Feature screens consuming raw scheme strings for cross-feature routing.
- App module aggregation patterns that depend on almost every feature and core
  module automatically.

## Notmid Contract Direction

Current Notmid contracts should continue to point here:

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
  RouteCommand
  RouteEvent

:core:router:impl
  registry/DefaultRouteRegistry
  event/DefaultRouteEventPlanner
  deeplink/DefaultDeepLinkResolver, DeepLinkUrlPolicy, UriDeepLinkRequestParser
  deeplink/StaticRouteDeepLinkSpec, PrefixRouteDeepLinkSpec

:feature:*:api
  route data
  deep-link specs
  public route events

:feature:notmid:impl router package
  NotmidRouteGraph
  NotmidRouteEventMapper
  NotmidAppRouter binding

:core-app router package
  config/AppRouterBundleConfig, AppDeepLinkUrlConfig, DefaultAppRouterBundle
  planner/AppRoutePlanner, DefaultAppRoutePlanner
  deeplink/AppDeepLinkResolver, DefaultAppDeepLinkResolver
  runtime/AppRouterRuntime, DefaultAppRouterRuntime
  activity/ActivityRouteLauncherEffect, ActivityRouteLaunchHandler,
  DefaultActivityRouteLauncher

:app
  MainActivity external intent handoff
  auth/deferred route handling when app-global
  concrete launcher binding with feature-owned launch handlers
```

When the app route surface grows, introduce a `RouteIntent` or app-owned
`RoutePlan` layer before exposing more route execution construction to feature
UI.

## Local Review Questions

- Does a feature implementation depend on another feature implementation just to
  navigate?
- Does feature UI construct an Android `Intent` or manipulate `NavController`
  for a cross-feature target?
- Does a deep link resolve through the same route path as an in-app event?
- Does an `ActivityRoute` launch without accidentally replacing the Compose
  stack?
- Are app-link host/scheme/base-path rules kept in the product shell router
  bundle config and passed through `:core-app`, not duplicated in `:app` or
  feature screens?
- Could `RouteStack.entries` become `List<NavKey>` later without rewriting
  feature callers?
