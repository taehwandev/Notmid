package app.thdev.glassnavlab.core.navigation.assertions

import app.thdev.glassnavlab.core.navigation.runtime.RouteEvent
import app.thdev.glassnavlab.core.navigation.runtime.RouteEventPlanner
import app.thdev.glassnavlab.core.navigation.runtime.RoutePlan

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
