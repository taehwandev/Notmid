package app.thdev.glassnavlab.core.router.assertions

import app.thdev.glassnavlab.core.router.runtime.RouteEvent
import app.thdev.glassnavlab.core.router.runtime.RouteEventPlanner
import app.thdev.glassnavlab.core.router.runtime.RoutePlan

class FakeRouteEventPlanner(
    private val plansByEvent: Map<RouteEvent, RoutePlan> = emptyMap(),
) : RouteEventPlanner {
    val requestedEvents: List<RouteEvent>
        get() = mutableRequestedEvents.toList()

    private val mutableRequestedEvents = mutableListOf<RouteEvent>()

    override fun planFor(event: RouteEvent): RoutePlan? {
        mutableRequestedEvents += event
        return plansByEvent[event]
    }
}
