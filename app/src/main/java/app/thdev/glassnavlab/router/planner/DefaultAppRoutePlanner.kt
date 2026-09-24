package app.thdev.glassnavlab.router.planner

import app.thdev.glassnavlab.core.navigation.runtime.RouteCommand
import app.thdev.glassnavlab.core.navigation.runtime.RouteEvent
import app.thdev.glassnavlab.core.navigation.runtime.RouteEventPlanner
import app.thdev.glassnavlab.core.navigation.runtime.RoutePlan
import app.thdev.glassnavlab.router.deeplink.AppDeepLinkResolver

class DefaultAppRoutePlanner(
    private val routeEventPlanner: RouteEventPlanner,
    private val deepLinkResolver: AppDeepLinkResolver,
) : AppRoutePlanner {
    override fun planFor(command: RouteCommand): RoutePlan {
        return command.plan
    }

    override fun planFor(event: RouteEvent): RoutePlan? {
        return routeEventPlanner.planFor(event)
    }

    override fun planForDeepLink(uriString: String): RoutePlan? {
        return deepLinkResolver.resolve(uriString)
    }
}
