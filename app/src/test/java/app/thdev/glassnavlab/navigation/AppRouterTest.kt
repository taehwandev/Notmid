package app.thdev.glassnavlab.navigation

import app.thdev.glassnavlab.core.router.RouteCommand
import app.thdev.glassnavlab.core.router.RoutePlan
import app.thdev.glassnavlab.core.router.RouteStack
import app.thdev.glassnavlab.feature.feed.api.ClipDetailRoute
import app.thdev.glassnavlab.feature.feed.api.FeedRouteEvent
import app.thdev.glassnavlab.feature.feed.api.FeedRoute
import app.thdev.glassnavlab.feature.inbox.api.ChatThreadRoute
import app.thdev.glassnavlab.feature.inbox.api.InboxRoute
import app.thdev.glassnavlab.feature.inbox.api.InboxRouteEvent
import app.thdev.glassnavlab.feature.map.api.MapRoute
import app.thdev.glassnavlab.feature.map.api.MapRouteEvent
import app.thdev.glassnavlab.feature.map.api.PlaceDetailRoute
import app.thdev.glassnavlab.feature.notmid.api.NotmidDestinationIds
import app.thdev.glassnavlab.feature.notmid.api.NotmidRouteEvent
import app.thdev.glassnavlab.feature.profile.api.ProfileRoute
import app.thdev.glassnavlab.feature.profile.api.ProfileSettingsRoute
import app.thdev.glassnavlab.feature.webview.api.WebViewRouteSpec
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

        assertEquals(
            listOf(ProfileRoute),
            router.backStack.entries,
        )
    }

    @Test
    fun activityRouteCommandQueuesActivityLaunchWithoutReplacingComposeStack() {
        val router = AppRouter()
        val webViewRoute = WebViewRouteSpec.create(
            url = "https://thdev.app/help",
            title = "Help",
        )

        router.navigate(RouteCommand(webViewRoute))

        assertEquals(
            listOf(FeedRoute),
            router.backStack.entries,
        )
        assertEquals(webViewRoute, router.pendingActivityRouteRequest?.route)
    }

    @Test
    fun routePlanCanUpdateComposeStackAndQueueActivityLaunchTogether() {
        val router = AppRouter()
        val webViewRoute = WebViewRouteSpec.create(url = "https://thdev.app/help")

        router.execute(
            RoutePlan(
                composeStack = RouteStack.single(ProfileRoute),
                activityRoutes = listOf(webViewRoute),
            ),
        )

        assertEquals(
            listOf(ProfileRoute),
            router.backStack.entries,
        )
        assertEquals(webViewRoute, router.pendingActivityRouteRequest?.route)
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
    fun activityRouteDeepLinkQueuesActivityLaunchWithoutReplacingComposeStack() {
        val router = AppRouter()

        router.navigateDeepLink("https://thdev.app/notmid/web?url=https%3A%2F%2Fthdev.app%2Fhelp")

        assertEquals(
            listOf(FeedRoute),
            router.backStack.entries,
        )
        assertEquals(
            WebViewRouteSpec.create(url = "https://thdev.app/help"),
            router.pendingActivityRouteRequest?.route,
        )
    }

    @Test
    fun consumedActivityRouteClearsPendingRequest() {
        val router = AppRouter()
        val webViewRoute = WebViewRouteSpec.create(url = "https://thdev.app/help")

        router.navigate(RouteCommand(webViewRoute))
        val requestId = router.pendingActivityRouteRequest?.id ?: error("Missing activity route request.")

        router.consumeActivityRouteRequest(requestId)

        assertNull(router.pendingActivityRouteRequest)
    }
}
