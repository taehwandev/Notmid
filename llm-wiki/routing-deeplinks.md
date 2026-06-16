# Routing And Deep Links

## Direction

notmid uses typed route contracts, route events, and a registry. Avoid ad-hoc string navigation.

Feature API modules own route contracts. App shell assembles and executes route
plans.

Feature API contracts should be split by caller-facing role once they contain
more than one contract family:

```text
route/       route keys and typed route data
deeplink/    DeepLinkSpec implementations and path matchers
event/       UI-emitted route events
destination/ shared destination ids, when used by multiple features
activity/    Activity keys, when an ActivityRoute needs launcher lookup
```

Avoid catch-all navigation contract files that mix route data, deep-link
matching, events, destination ids, and Activity keys.

## Core Types

```text
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
Router
```

`RouteStack` is the ordered Compose back stack only. `ActivityRoute` execution
requests live in `RoutePlan.activityRoutes`. `RouteCommand` is the app/router
execution command family that chooses between setting a Compose stack and
launching Activity-backed destinations.

## Core App Runtime

Pure planning and matching are reusable through `:core:router:impl`; runtime
execution is reusable through the `:core-app` router package:

```text
AppRouterBundleConfig
  -> DefaultAppRouterBundle
  -> DefaultRouteRegistry + DefaultDeepLinkResolver + DefaultRouteEventPlanner
  -> DefaultAppDeepLinkResolver adapter + DefaultAppRoutePlanner
RoutePlan
  -> DefaultAppRouterRuntime
  -> Compose RouteStack and/or pending ActivityRoute request
pending ActivityRoute
  -> ActivityRouteLauncherEffect
  -> DefaultActivityRouteLauncher
  -> feature-owned ActivityRouteLaunchHandler
```

The Notmid product shell provides route registration, feature event handlers,
and host/base-path values to the reusable `core-app` bundle. Core router impl
owns URI parsing, base-path stripping, deep-link matching, and route-event
handler dispatch. The core-app runtime owns Compose state, pending request
queueing, lifecycle-safe consume-on-success behavior, and ActivityRoute launch
handler composition. The app owns only the external intent handoff and concrete
runtime binding.

## Route Targets

Compose routes:

```text
FeedRoute
MapRoute
CaptureRoute
InboxRoute
ProfileRoute
ClipDetailRoute(clipId)
PlaceDetailRoute(placeId)
ChatThreadRoute(threadId)
ProfileSettingsRoute
```

Activity route:

```text
WebViewRoute(url, title, mode)
```

WebView is intentionally an Activity because lifecycle, reload behavior, file chooser, permissions, history, and fullscreen media are cleaner there.

## Deep-Link Behavior

Deep links resolve to ordered route plans. Compose destinations produce an
ordered `RouteStack`; Activity-backed destinations produce an `ActivityRoute`
launch request.

```text
https://thdev.app/notmid
  -> [Feed]

https://thdev.app/notmid/feed
  -> [Feed]

https://thdev.app/notmid/profile/settings
  -> [Profile, ProfileSettings]

https://thdev.app/notmid/feed/clips/{clipId}
https://thdev.app/notmid/clips/{clipId}
  -> [Feed, ClipDetail(clipId)]

https://thdev.app/notmid/map/places/{placeId}
https://thdev.app/notmid/places/{placeId}
  -> [Map, PlaceDetail(placeId)]

https://thdev.app/notmid/inbox/chats/{threadId}
https://thdev.app/notmid/chats/{threadId}
  -> [Inbox, ChatThread(threadId)]

https://thdev.app/notmid/web?url={encodedUrl}
  -> RoutePlan(activityRoutes=[WebViewRoute(url)])
```

WebView URL accepts only `http` and `https`.

## Adding A Dynamic Screen

1. Add route data class in the owning `feature:*:api` `route/` package.
2. Add `DeepLinkSpec` in `deeplink/` when the destination is externally addressable.
3. Add feature route event in `event/` if UI opens it.
4. Register the top-level route or deep-link spec in the product-shell router bundle config when app-level resolution needs it.
5. Add a product-shell route event handler when UI opens it.
6. Render route in `NotmidShellScreen` or owning shell.
7. Add tests:
   - `AppDeepLinkResolverTest`
   - `AppRouterTest`

## Boundaries

Allowed:

```text
feature impl emits FeatureRouteEvent
app converts event to RoutePlan
app mutates Compose RouteStack or launches ActivityRoute
```

Not allowed:

```text
feature impl depends on another feature impl
feature impl parses app web URLs
feature impl starts another feature Activity directly
```
