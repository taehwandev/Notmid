package app.thdev.glassnavlab.core.navigation.registry

import app.thdev.glassnavlab.core.navigation.deeplink.DeepLinkRequest
import app.thdev.glassnavlab.core.navigation.deeplink.DeepLinkSpec
import app.thdev.glassnavlab.core.navigation.route.TopLevelRoute
import app.thdev.glassnavlab.core.navigation.runtime.RoutePlan
import app.thdev.glassnavlab.core.navigation.runtime.RouteStack

interface RouteRegistry {
    val defaultStack: RouteStack
    val topLevelRoutes: List<TopLevelRoute>
    val deepLinkSpecs: List<DeepLinkSpec>

    fun stackForDestination(destinationId: String): RouteStack?
    fun resolve(request: DeepLinkRequest): RoutePlan?
}
