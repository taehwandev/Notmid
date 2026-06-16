# notmid Router Architecture

## Direction

notmid should use a production-shaped router, not ad-hoc string navigation.

The design follows the useful part of a reference Android router idea:

- feature API modules own their journey/route contracts
- app-level router gathers those contracts
- deep links resolve through registered specs
- navigation is testable without feature implementation modules

The implementation is adapted for notmid's current shape:

- single Android Activity
- Compose screen stack today
- Activity launch support later
- web links must produce ordered route plans; Compose links carry route stacks

## Modules

```text
:core:router:api
  Route
  ComposeRoute
  ActivityRoute
  TopLevelRoute
  DeepLinkSpec
  DeepLinkRequest
  DeepLinkResolver
  RouteStack
  RoutePlan
  RouteCommand
  Router
  RouteEvent
  RouteEventHandler
  RouteEventPlanner
  RouteRegistry

:core:router:impl
  registry/DefaultRouteRegistry
  event/DefaultRouteEventPlanner
  deeplink/DefaultDeepLinkResolver
  deeplink/DeepLinkUrlPolicy
  deeplink/UriDeepLinkRequestParser
  deeplink/StaticRouteDeepLinkSpec
  deeplink/PrefixRouteDeepLinkSpec

:core:runtime
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
  route/FeatureRoute
  deeplink/FeatureDeepLinkSpec
  event/FeatureRouteEvent when needed

:feature:webview:api
  route/WebViewRoute
  deeplink/WebViewDeepLinkSpec
  activity/WebViewActivityKeys

:feature:*:impl
  Compose screens only

:feature:notmid:impl
  router/NotmidAppRouter binding
  router/NotmidRouteGraph
  router/NotmidRouteEventMapper
  product shell route registrations and event handlers

:feature:webview:impl
  WebViewActivity
  WebView intent factory
  WebViewActivityRouteLaunchHandler

:core:base
  BaseActivity external intent and pending deep-link handoff

:app
  MainActivity product wiring, auth gate, and concrete ActivityRoute launcher binding
```

## Rule

Feature implementation modules must not navigate directly to another feature implementation.

Allowed:

```text
feature:feed:impl -> emits OpenPlace(placeId)
feature:feed:api -> owns Feed route/event contracts
app -> converts OpenPlace(placeId) into [Map, PlaceDetail(placeId)]
```

Not allowed:

```text
feature:feed:impl -> depends on feature:map:impl
feature:feed:impl -> constructs Android Intent for another feature
feature:feed:impl -> parses app web URLs
```

## Contract Shape

Use plain interfaces for route contracts that feature modules and app adapters
must implement independently:

```text
Route
ComposeRoute
ActivityRoute
TopLevelRoute
DeepLinkSpec
RouteRegistry
Router
```

Use `sealed interface` only for closed families owned by one module:

```text
RouteCommand
FeatureRouteEvent
local route policy/result families
```

`RouteStack` is the ordered Compose back stack. Activity-backed destinations are
execution requests in `RoutePlan.activityRoutes`, not entries in `RouteStack`.

## Route Targets

Most app screens are `ComposeRoute`.

```text
FeedRoute
MapRoute
CaptureRoute
InboxRoute
ProfileRoute
ClipDetailRoute(clipId)
PlaceDetailRoute(placeId)
ChatThreadRoute(threadId)
```

WebView is intentionally an `ActivityRoute` because Android WebView lifecycle, reload behavior, file chooser, permission, history, and fullscreen media are usually cleaner in a dedicated Activity.

```text
WebViewRoute(url, title, mode)
  -> WebViewActivity
```

## Route Ownership

Each feature API owns its contract:

```kotlin
object FeedRoute : NotmidTopLevelRoute
object FeedDeepLinkSpec : NotmidStaticDeepLinkSpec(FeedRoute)
```

Dynamic routes should follow the same shape:

```kotlin
data class ClipDetailRoute(
    val clipId: String,
) : NotmidRoute

data class PlaceDetailRoute(
    val placeId: String,
) : NotmidRoute

object PlaceDeepLinkSpec : DeepLinkSpec
```

Activity routes follow the same contract style:

```kotlin
data class WebViewRoute(
    val url: String,
) : ActivityRoute

object WebViewDeepLinkSpec : DeepLinkSpec
```

## Deep Link Behavior

Deep links must resolve to ordered route plans, not just a destination. Compose
destinations use `RouteStack`; Activity-backed destinations use
`RoutePlan.activityRoutes`.

```text
https://thdev.app/notmid
  -> [Feed]

https://thdev.app/notmid/feed
  -> [Feed]

https://thdev.app/notmid/profile/settings
  -> [Profile, ProfileSettings]

https://thdev.app/notmid/feed/clips/{clipId}
  -> [Feed, ClipDetail(clipId)]

https://thdev.app/notmid/map/places/{placeId}
  -> [Map, PlaceDetail(placeId)]

https://thdev.app/notmid/inbox/chats/{threadId}
  -> [Inbox, ChatThread(threadId)]

https://thdev.app/notmid/web?url={encodedUrl}
  -> RoutePlan(activityRoutes=[WebViewRoute(url)])
```

The parser also accepts the shorter canonical object paths:

```text
https://thdev.app/notmid/clips/{clipId}
https://thdev.app/notmid/places/{placeId}
https://thdev.app/notmid/chats/{threadId}
```

Those still resolve through the owning top-level route first, so browser links and in-app events share the same stack shape.

## Why Registry

Without a registry, app routing becomes a growing chain of string checks:

```text
if segment == "feed"
if segment == "map"
if segment == "profile" && child == "settings"
```

With a registry:

- each feature adds a route spec and deep link spec
- app adds the spec to one registry list
- tests assert stack output
- feature implementation stays independent

## Why Core Router Impl And Core App Router Package

`DefaultRouteRegistry`, `DefaultDeepLinkResolver`, and
`DefaultRouteEventPlanner` do not execute navigation. They parse, normalize,
match, and resolve reusable route inputs into a `RoutePlan`.

Reusable execution belongs in the `:core:runtime` router package only after a
`RoutePlan` exists because it needs Compose runtime state and ActivityRoute
side-effect orchestration, but it must not know Notmid feature contracts.

The drop-in shape is:

```text
app provides:
  ActivityRouteLauncher binding

product shell provides:
  AppRouterBundleConfig
  RouteEventHandler list
  host/scheme/base-path values
  feature route and deep-link registrations

core router impl provides:
  DefaultDeepLinkResolver
  DefaultRouteEventPlanner
  DefaultRouteRegistry

:core:runtime provides:
  DefaultAppRouterBundle
  DefaultAppDeepLinkResolver adapter
  DefaultAppRoutePlanner
  DefaultAppRouterRuntime
  DefaultActivityRouteLauncher
  ActivityRouteLauncherEffect
```

This keeps DI optional. A future app-level DI container can bind these
interfaces, but the reusable runtime works today with constructors and Compose
`remember`.

## Activity Support

When a feature needs a separate Activity, do not replace the route system.

Use `ActivityRoute`:

```kotlin
interface ActivityRoute {
    val activityKey: String
}
```

Then `:core:runtime` runtime queues execution and the ActivityRoute launcher binding
decides the concrete Android launch through feature-owned launch handlers:

```text
RouteCommand.SetComposeStack(RouteStack.single(CaptureRoute))
  -> Compose destination today
  -> Activity launcher tomorrow if Capture becomes a separate Activity

RouteCommand.LaunchActivity(WebViewRoute)
  -> WebViewActivity launch
```

The feature API remains stable either way.

## Testing Strategy

Minimum tests:

- deep link string resolves to expected `RoutePlan`; Compose links assert the
  expected `RouteStack`
- unknown deep link returns null
- route event resolves to expected stack
- default app route is Feed
- dynamic route arguments are parsed and preserved
- activity route commands queue an Activity launch request without replacing Compose stack

Current tests:

- `AppDeepLinkResolverTest`
- `AppRouterTest`
