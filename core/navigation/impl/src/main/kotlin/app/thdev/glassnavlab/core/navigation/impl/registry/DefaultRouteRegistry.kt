package app.thdev.glassnavlab.core.navigation.impl.registry

import app.thdev.glassnavlab.core.navigation.deeplink.DeepLinkRequest
import app.thdev.glassnavlab.core.navigation.deeplink.DeepLinkSpec
import app.thdev.glassnavlab.core.navigation.route.TopLevelRoute
import app.thdev.glassnavlab.core.navigation.registry.RouteRegistry
import app.thdev.glassnavlab.core.navigation.runtime.RoutePlan
import app.thdev.glassnavlab.core.navigation.runtime.RouteStack

class DefaultRouteRegistry(
    defaultRoute: TopLevelRoute,
    override val topLevelRoutes: List<TopLevelRoute>,
    override val deepLinkSpecs: List<DeepLinkSpec>,
) : RouteRegistry {
    override val defaultStack: RouteStack = RouteStack.single(defaultRoute)

    override fun stackForDestination(destinationId: String): RouteStack? {
        return topLevelRoutes
            .firstOrNull { it.destinationId == destinationId }
            ?.let(RouteStack::single)
    }

    override fun resolve(request: DeepLinkRequest): RoutePlan? {
        if (request.pathSegments.isEmpty()) return RoutePlan.compose(defaultStack)

        return deepLinkSpecs
            .sortedByDescending(DeepLinkSpec::priority)
            .firstNotNullOfOrNull { spec -> spec.match(request) }
    }
}
