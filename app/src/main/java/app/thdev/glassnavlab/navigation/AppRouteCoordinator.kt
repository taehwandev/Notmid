package app.thdev.glassnavlab.navigation

import app.thdev.glassnavlab.core.router.RouteCommand
import app.thdev.glassnavlab.core.router.RouteEvent
import app.thdev.glassnavlab.core.router.RoutePlan

internal class AppRouteCoordinator(
    private val deepLinkResolver: AppDeepLinkResolver = AppDeepLinkResolver(),
) {
    fun planFor(command: RouteCommand): RoutePlan {
        return command.plan
    }

    fun planFor(event: RouteEvent): RoutePlan? {
        return NotmidRouteEventMapper.planFor(event)
    }

    fun planForDeepLink(uriString: String): RoutePlan? {
        return deepLinkResolver.resolve(uriString)
    }
}
