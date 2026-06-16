package app.thdev.glassnavlab.navigation

import app.thdev.glassnavlab.feature.feed.api.ClipDetailRoute
import app.thdev.glassnavlab.feature.feed.api.FeedRoute
import app.thdev.glassnavlab.feature.inbox.api.ChatThreadRoute
import app.thdev.glassnavlab.feature.inbox.api.InboxRoute
import app.thdev.glassnavlab.feature.map.api.MapRoute
import app.thdev.glassnavlab.feature.map.api.PlaceDetailRoute
import app.thdev.glassnavlab.feature.profile.api.ProfileRoute
import app.thdev.glassnavlab.feature.profile.api.ProfileSettingsRoute
import app.thdev.glassnavlab.feature.webview.api.WebViewMode
import app.thdev.glassnavlab.feature.webview.api.WebViewRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppDeepLinkResolverTest {
    private val resolver = AppDeepLinkResolver()

    @Test
    fun feedDeepLinkResolvesFeedStack() {
        val plan = resolver.resolve("https://thdev.app/notmid/feed")

        assertEquals(
            listOf(FeedRoute),
            plan?.composeStack?.entries,
        )
    }

    @Test
    fun emptyNotmidDeepLinkResolvesDefaultFeedStack() {
        val plan = resolver.resolve("https://thdev.app/notmid")

        assertEquals(
            listOf(FeedRoute),
            plan?.composeStack?.entries,
        )
    }

    @Test
    fun mapDeepLinkResolvesMapStackFromFeatureSpec() {
        val plan = resolver.resolve("https://thdev.app/notmid/map")

        assertEquals(
            listOf(MapRoute),
            plan?.composeStack?.entries,
        )
    }

    @Test
    fun nestedSettingsDeepLinkResolvesOrderedStack() {
        val plan = resolver.resolve("https://thdev.app/notmid/profile/settings")

        assertEquals(
            listOf(ProfileRoute, ProfileSettingsRoute),
            plan?.composeStack?.entries,
        )
    }

    @Test
    fun clipDeepLinkResolvesOrderedFeedStack() {
        val plan = resolver.resolve("https://thdev.app/notmid/feed/clips/cafe-queue-check")

        assertEquals(
            listOf(FeedRoute, ClipDetailRoute("cafe-queue-check")),
            plan?.composeStack?.entries,
        )
    }

    @Test
    fun placeDeepLinkResolvesOrderedMapStack() {
        val plan = resolver.resolve("https://thdev.app/notmid/map/places/millo-roasters")

        assertEquals(
            listOf(MapRoute, PlaceDetailRoute("millo-roasters")),
            plan?.composeStack?.entries,
        )
    }

    @Test
    fun shortObjectDeepLinkResolvesThroughOwningTopLevelRoute() {
        val plan = resolver.resolve("https://thdev.app/notmid/places/millo-roasters")

        assertEquals(
            listOf(MapRoute, PlaceDetailRoute("millo-roasters")),
            plan?.composeStack?.entries,
        )
    }

    @Test
    fun chatDeepLinkResolvesOrderedInboxStack() {
        val plan = resolver.resolve("https://thdev.app/notmid/inbox/chats/clip-thread")

        assertEquals(
            listOf(InboxRoute, ChatThreadRoute("clip-thread")),
            plan?.composeStack?.entries,
        )
    }

    @Test
    fun unknownDeepLinkReturnsNull() {
        val command = resolver.resolve("https://thdev.app/notmid/feed/unknown")

        assertNull(command)
    }

    @Test
    fun webViewDeepLinkResolvesActivityRoute() {
        val plan = resolver.resolve(
            "https://thdev.app/notmid/web?url=https%3A%2F%2Fthdev.app%2Fhelp&title=Help&mode=Help",
        )

        assertEquals(
            listOf(
                WebViewRoute(
                    url = "https://thdev.app/help",
                    title = "Help",
                    mode = WebViewMode.Help,
                ),
            ),
            plan?.activityRoutes,
        )
    }

    @Test
    fun webViewDeepLinkRejectsNonWebUrl() {
        val command = resolver.resolve("https://thdev.app/notmid/web?url=javascript%3Aalert%281%29")

        assertNull(command)
    }
}
