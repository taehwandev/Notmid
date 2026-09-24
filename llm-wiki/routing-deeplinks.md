# Routing And Deep Links

This page records Notmid route targets and canonical deep-link behavior. General
router architecture, feature API/impl split rules, ActivityRoute handling, and
ViewModel event flow live in Tao Agent OS.

## Current Owners

```text
:core:navigation:api  pure route contracts and route plan types
:core:navigation:impl reusable registry, event planner, URL parsing, deep-link matching
:core:runtime         Compose route runtime and ActivityRoute launch runtime
:feature:*:api        feature route data, deep-link specs, public route events
:feature:auth:ui      login screen state, actions, and authentication requests
:app                  injected Notmid route graph/factory, event handler bindings, shell rendering, Android entrypoint
```

Production route-event registration uses Hilt multibinding. Add a new
`RouteEventHandler` for the product route family in `:app` and bind it with
`@IntoSet`; do not add another cast entry to a central app-shell list or hide
the product route graph in an `object`.

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

WebView is intentionally Activity-backed because lifecycle, reload behavior,
file chooser, permissions, history, and fullscreen media are cleaner there.

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

## Contract Tests

```text
AppDeepLinkResolverTest
AppRouterTest
```
