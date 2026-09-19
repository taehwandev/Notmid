package app.thdev.glassnavlab.core.navigation.impl.event

import app.thdev.glassnavlab.core.navigation.runtime.RouteEvent
import app.thdev.glassnavlab.core.navigation.runtime.RouteEventHandler
import app.thdev.glassnavlab.core.navigation.runtime.RouteEventPlanner
import app.thdev.glassnavlab.core.navigation.runtime.RoutePlan

class DefaultRouteEventPlanner(
    private val handlers: List<RouteEventHandler>,
) : RouteEventPlanner {
    override fun planFor(event: RouteEvent): RoutePlan? {
        return handlers.firstNotNullOfOrNull { handler ->
            handler.planFor(event)
        }
    }
}
