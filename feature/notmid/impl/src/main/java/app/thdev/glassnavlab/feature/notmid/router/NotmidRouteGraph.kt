package app.thdev.glassnavlab.feature.notmid.router

import app.thdev.glassnavlab.core.router.deeplink.DeepLinkRequest
import app.thdev.glassnavlab.core.router.registry.RouteRegistry
import app.thdev.glassnavlab.core.router.runtime.RoutePlan
import app.thdev.glassnavlab.core.router.runtime.RouteStack
import app.thdev.glassnavlab.core.runtime.router.deeplink.AppDeepLinkResolver
import app.thdev.glassnavlab.core.runtime.router.config.AppDeepLinkUrlConfig
import app.thdev.glassnavlab.core.runtime.router.config.AppRouterBundle
import app.thdev.glassnavlab.core.runtime.router.config.AppRouterBundleConfig
import app.thdev.glassnavlab.core.runtime.router.config.DefaultAppRouterBundle
import app.thdev.glassnavlab.feature.capture.api.deeplink.CaptureDeepLinkSpec
import app.thdev.glassnavlab.feature.capture.api.route.CaptureRoute
import app.thdev.glassnavlab.feature.feed.api.deeplink.ClipDeepLinkSpec
import app.thdev.glassnavlab.feature.feed.api.deeplink.FeedDeepLinkSpec
import app.thdev.glassnavlab.feature.feed.api.route.ClipDetailRoute
import app.thdev.glassnavlab.feature.feed.api.route.FeedRoute
import app.thdev.glassnavlab.feature.inbox.api.deeplink.ChatDeepLinkSpec
import app.thdev.glassnavlab.feature.inbox.api.deeplink.InboxDeepLinkSpec
import app.thdev.glassnavlab.feature.inbox.api.route.ChatThreadRoute
import app.thdev.glassnavlab.feature.inbox.api.route.InboxRoute
import app.thdev.glassnavlab.feature.map.api.deeplink.MapDeepLinkSpec
import app.thdev.glassnavlab.feature.map.api.deeplink.PlaceDeepLinkSpec
import app.thdev.glassnavlab.feature.map.api.route.MapRoute
import app.thdev.glassnavlab.feature.map.api.route.PlaceDetailRoute
import app.thdev.glassnavlab.feature.notmid.api.route.NotmidRoute
import app.thdev.glassnavlab.feature.profile.api.deeplink.ProfileDeepLinkSpec
import app.thdev.glassnavlab.feature.profile.api.deeplink.ProfileSettingsDeepLinkSpec
import app.thdev.glassnavlab.feature.profile.api.route.ProfileRoute
import app.thdev.glassnavlab.feature.profile.api.route.ProfileSettingsRoute
import app.thdev.glassnavlab.feature.webview.api.deeplink.WebViewDeepLinkSpec

internal object NotmidRouteGraph {
    private val bundle: AppRouterBundle = DefaultAppRouterBundle(
        config = AppRouterBundleConfig(
            defaultRoute = FeedRoute,
            topLevelRoutes = listOf(
                FeedRoute,
                MapRoute,
                CaptureRoute,
                InboxRoute,
                ProfileRoute,
            ),
            deepLinkSpecs = listOf(
                ClipDeepLinkSpec,
                PlaceDeepLinkSpec,
                ChatDeepLinkSpec,
                ProfileSettingsDeepLinkSpec,
                FeedDeepLinkSpec,
                MapDeepLinkSpec,
                CaptureDeepLinkSpec,
                InboxDeepLinkSpec,
                ProfileDeepLinkSpec,
                WebViewDeepLinkSpec,
            ),
            deepLinkUrlConfig = AppDeepLinkUrlConfig.withBasePath(
                scheme = APP_WEB_SCHEME,
                host = APP_WEB_HOST,
                basePath = APP_WEB_BASE_PATH,
            ),
            routeEventHandlers = NotmidRouteEventMapper.handlers,
        ),
    )

    val routerBundle: AppRouterBundle = bundle
    val registry: RouteRegistry = bundle.registry
    val defaultRoute: NotmidRoute = FeedRoute
    val deepLinkResolver: AppDeepLinkResolver = AppDeepLinkResolver { uriString ->
        bundle.routePlanner.planForDeepLink(uriString)
    }

    fun destination(destinationId: String): NotmidRoute {
        return registry.stackForDestination(destinationId)
            ?.topRoute as? NotmidRoute
            ?: defaultRoute
    }

    fun settingsStack(): RouteStack {
        return RouteStack.of(
            ProfileRoute,
            ProfileSettingsRoute,
        )
    }

    fun clipStack(clipId: String): RouteStack {
        return RouteStack.of(
            FeedRoute,
            ClipDetailRoute(clipId),
        )
    }

    fun placeStack(placeId: String): RouteStack {
        return RouteStack.of(
            MapRoute,
            PlaceDetailRoute(placeId),
        )
    }

    fun chatThreadStack(threadId: String): RouteStack {
        return RouteStack.of(
            InboxRoute,
            ChatThreadRoute(threadId),
        )
    }

    fun resolveDeepLink(request: DeepLinkRequest): RoutePlan? {
        return registry.resolve(request)
    }

    private const val APP_WEB_SCHEME = "https"
    private const val APP_WEB_HOST = "thdev.app"
    private const val APP_WEB_BASE_PATH = "notmid"
}
