package app.thdev.glassnavlab.core.router.impl.event

import app.thdev.glassnavlab.core.router.runtime.RouteEvent
import app.thdev.glassnavlab.core.router.runtime.RouteEventHandler
import app.thdev.glassnavlab.core.router.runtime.RouteEventPlanner
import app.thdev.glassnavlab.core.router.runtime.RoutePlan

class DefaultRouteEventPlanner(
    private val handlers: List<RouteEventHandler>,
) : RouteEventPlanner {
    override fun planFor(event: RouteEvent): RoutePlan? {
        return handlers.firstNotNullOfOrNull { handler ->
            handler.planFor(event)
        }
    }
}
