package app.thdev.glassnavlab.router.config

import app.thdev.glassnavlab.core.navigation.deeplink.DeepLinkSpec
import app.thdev.glassnavlab.core.navigation.route.TopLevelRoute
import app.thdev.glassnavlab.core.navigation.runtime.RouteEventHandler

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
    }
}
