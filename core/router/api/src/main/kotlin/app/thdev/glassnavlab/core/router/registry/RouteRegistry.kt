package app.thdev.glassnavlab.core.router.registry

import app.thdev.glassnavlab.core.router.deeplink.DeepLinkRequest
import app.thdev.glassnavlab.core.router.deeplink.DeepLinkSpec
import app.thdev.glassnavlab.core.router.route.TopLevelRoute
import app.thdev.glassnavlab.core.router.runtime.RoutePlan
import app.thdev.glassnavlab.core.router.runtime.RouteStack

interface RouteRegistry {
    val defaultStack: RouteStack
    val topLevelRoutes: List<TopLevelRoute>
    val deepLinkSpecs: List<DeepLinkSpec>

    fun stackForDestination(destinationId: String): RouteStack?
    fun resolve(request: DeepLinkRequest): RoutePlan?
}
