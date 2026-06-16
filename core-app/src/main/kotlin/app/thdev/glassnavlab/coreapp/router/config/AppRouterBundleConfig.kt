package app.thdev.glassnavlab.coreapp.router.config

import app.thdev.glassnavlab.core.router.deeplink.DeepLinkSpec
import app.thdev.glassnavlab.core.router.route.TopLevelRoute
import app.thdev.glassnavlab.core.router.runtime.RouteEventHandler

data class AppRouterBundleConfig(
    val defaultRoute: TopLevelRoute,
    val topLevelRoutes: List<TopLevelRoute>,
    val deepLinkSpecs: List<DeepLinkSpec>,
    val deepLinkUrlConfig: AppDeepLinkUrlConfig,
    val routeEventHandlers: List<RouteEventHandler>,
) {
    init {
        require(topLevelRoutes.isNotEmpty()) { "At least one top-level route is required." }
        require(defaultRoute in topLevelRoutes) {
            "The default route must also be registered as a top-level route."
        }
        require(routeEventHandlers.isNotEmpty()) {
            "At least one route event handler is required."
        }
    }
}
