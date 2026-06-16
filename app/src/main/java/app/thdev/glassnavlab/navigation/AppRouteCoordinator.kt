package app.thdev.glassnavlab.navigation

import app.thdev.glassnavlab.core.router.runtime.RouteCommand
import app.thdev.glassnavlab.core.router.runtime.RouteEvent
import app.thdev.glassnavlab.core.router.runtime.RoutePlan

internal class AppRouteCoordinator(
    private val deepLinkResolver: AppDeepLinkResolver = AppDeepLinkResolver(),
) {
    fun planFor(command: RouteCommand): RoutePlan {
        return when (command) {
            is RouteCommand.LaunchActivity -> command.plan
            is RouteCommand.SetComposeStack -> command.plan
        }
    }

    fun planFor(event: RouteEvent): RoutePlan? {
        return NotmidRouteEventMapper.planFor(event)
    }

    fun planForDeepLink(uriString: String): RoutePlan? {
        return deepLinkResolver.resolve(uriString)
    }
}
