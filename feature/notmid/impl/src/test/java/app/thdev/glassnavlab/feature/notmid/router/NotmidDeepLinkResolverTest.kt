package app.thdev.glassnavlab.feature.notmid.router

import app.thdev.glassnavlab.core.router.assertions.assertRoutePlan
import app.thdev.glassnavlab.feature.feed.api.route.ClipDetailRoute
import app.thdev.glassnavlab.feature.feed.api.route.FeedRoute
import app.thdev.glassnavlab.feature.inbox.api.route.ChatThreadRoute
import app.thdev.glassnavlab.feature.inbox.api.route.InboxRoute
import app.thdev.glassnavlab.feature.map.api.route.MapRoute
import app.thdev.glassnavlab.feature.map.api.route.PlaceDetailRoute
import app.thdev.glassnavlab.feature.profile.api.route.ProfileRoute
import app.thdev.glassnavlab.feature.profile.api.route.ProfileSettingsRoute
import app.thdev.glassnavlab.feature.webview.api.route.WebViewMode
import app.thdev.glassnavlab.feature.webview.api.route.WebViewRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotmidDeepLinkResolverTest {
    private val resolver = NotmidRouteGraph().deepLinkResolver

    @Test
    fun feedDeepLinkResolvesFeedStack() {
        val plan = resolver.resolve("https://thdev.app/notmid/feed")

        plan?.assertRoutePlan {
            hasComposeStack(FeedRoute)
            hasNoActivityRoutes()
        }
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

        plan?.assertRoutePlan {
            hasComposeStack(ProfileRoute, ProfileSettingsRoute)
            hasNoActivityRoutes()
        }
    }

    @Test
    fun clipDeepLinkResolvesOrderedFeedStack() {
        val plan = resolver.resolve("https://thdev.app/notmid/feed/clips/cafe-queue-check")

        plan?.assertRoutePlan {
            hasComposeStack(FeedRoute, ClipDetailRoute("cafe-queue-check"))
            hasNoActivityRoutes()
        }
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
