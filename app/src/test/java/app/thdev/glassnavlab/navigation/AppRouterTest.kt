package app.thdev.glassnavlab.navigation

import app.thdev.glassnavlab.core.router.runtime.RouteCommand
import app.thdev.glassnavlab.core.router.runtime.RoutePlan
import app.thdev.glassnavlab.core.router.runtime.RouteStack
import app.thdev.glassnavlab.core.router.assertions.TestActivityRoute
import app.thdev.glassnavlab.core.router.assertions.assertRouteStack
import app.thdev.glassnavlab.feature.feed.api.route.ClipDetailRoute
import app.thdev.glassnavlab.feature.feed.api.event.FeedRouteEvent
import app.thdev.glassnavlab.feature.feed.api.route.FeedRoute
import app.thdev.glassnavlab.feature.inbox.api.route.ChatThreadRoute
import app.thdev.glassnavlab.feature.inbox.api.route.InboxRoute
import app.thdev.glassnavlab.feature.inbox.api.event.InboxRouteEvent
import app.thdev.glassnavlab.feature.map.api.route.MapRoute
import app.thdev.glassnavlab.feature.map.api.event.MapRouteEvent
import app.thdev.glassnavlab.feature.map.api.route.PlaceDetailRoute
import app.thdev.glassnavlab.feature.notmid.api.destination.NotmidDestinationIds
import app.thdev.glassnavlab.feature.notmid.api.event.NotmidRouteEvent
import app.thdev.glassnavlab.feature.profile.api.route.ProfileRoute
import app.thdev.glassnavlab.feature.profile.api.route.ProfileSettingsRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppRouterTest {
    @Test
    fun startsWithDefaultFeedRoute() {
        val router = AppRouter()

        assertEquals(
            listOf(FeedRoute),
            router.backStack.entries,
        )
    }

    @Test
    fun destinationSelectedNavigatesThroughRouteRegistry() {
        val router = AppRouter()

        router.onRouteEvent(
            NotmidRouteEvent.DestinationSelected(NotmidDestinationIds.MAP),
        )

        assertEquals(
            listOf(MapRoute),
            router.backStack.entries,
        )
    }

    @Test
    fun settingsEventBuildsOrderedProfileStack() {
        val router = AppRouter()

        router.onRouteEvent(NotmidRouteEvent.SettingsRequested)

        assertEquals(
            listOf(ProfileRoute, ProfileSettingsRoute),
            router.backStack.entries,
        )
    }

    @Test
    fun feedClipEventBuildsOrderedClipStack() {
        val router = AppRouter()

        router.onRouteEvent(FeedRouteEvent.ClipRequested("cafe-queue-check"))

        assertEquals(
            listOf(FeedRoute, ClipDetailRoute("cafe-queue-check")),
            router.backStack.entries,
        )
    }

    @Test
    fun mapPlaceEventBuildsOrderedPlaceStack() {
        val router = AppRouter()

        router.onRouteEvent(MapRouteEvent.PlaceRequested("millo-roasters"))

        assertEquals(
            listOf(MapRoute, PlaceDetailRoute("millo-roasters")),
            router.backStack.entries,
        )
    }

    @Test
    fun inboxChatEventBuildsOrderedChatStack() {
        val router = AppRouter()

        router.onRouteEvent(InboxRouteEvent.ChatThreadRequested("clip-thread"))

        assertEquals(
            listOf(InboxRoute, ChatThreadRoute("clip-thread")),
            router.backStack.entries,
        )
    }

    @Test
    fun directRouteCommandReplacesStack() {
        val router = AppRouter()

        router.navigate(RouteCommand(RouteStack.single(ProfileRoute)))

        router.backStack.assertRouteStack {
            hasEntries(ProfileRoute)
            containsOnlyComposeRoutes()
        }
    }

    @Test
    fun activityRouteCommandQueuesActivityLaunchWithoutReplacingComposeStack() {
        val router = AppRouter()
        val activityRoute = TestActivityRoute(
            route = "settings-activity",
            activityKey = "settings",
        )

        router.navigate(RouteCommand(activityRoute))

        assertEquals(
            listOf(FeedRoute),
            router.backStack.entries,
        )
        assertEquals(activityRoute, router.pendingActivityRouteRequest?.route)
    }

    @Test
    fun routePlanCanUpdateComposeStackAndQueueActivityLaunchTogether() {
        val router = AppRouter()
        val activityRoute = TestActivityRoute(
            route = "settings-activity",
            activityKey = "settings",
        )

        router.execute(
            RoutePlan(
                composeStack = RouteStack.single(ProfileRoute),
                activityRoutes = listOf(activityRoute),
            ),
        )

        assertEquals(
            listOf(ProfileRoute),
            router.backStack.entries,
        )
        assertEquals(activityRoute, router.pendingActivityRouteRequest?.route)
    }

    @Test
    fun deepLinkNavigationUsesRoutePlanExecution() {
        val router = AppRouter()

        router.navigateDeepLink("https://thdev.app/notmid/profile/settings")

        assertEquals(
            listOf(ProfileRoute, ProfileSettingsRoute),
            router.backStack.entries,
        )
    }

    @Test
    fun consumedActivityRouteClearsPendingRequest() {
        val router = AppRouter()
        val activityRoute = TestActivityRoute(
            route = "settings-activity",
            activityKey = "settings",
        )

        router.navigate(RouteCommand(activityRoute))
        val requestId = router.pendingActivityRouteRequest?.id ?: error("Missing activity route request.")

        router.consumeActivityRouteRequest(requestId)

        assertNull(router.pendingActivityRouteRequest)
    }
}
