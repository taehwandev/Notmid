package app.thdev.glassnavlab.feature.notmid.router

import app.thdev.glassnavlab.core.router.runtime.RouteEvent
import app.thdev.glassnavlab.core.router.runtime.RouteEventHandler
import app.thdev.glassnavlab.core.router.runtime.RoutePlan
import app.thdev.glassnavlab.feature.map.api.event.MapRouteEvent
import javax.inject.Inject

class MapRouteEventHandler @Inject constructor(
    private val routeGraph: NotmidRouteGraph,
) : RouteEventHandler {
    override fun planFor(event: RouteEvent): RoutePlan? {
        return when (event) {
            is MapRouteEvent.PlaceRequested -> routeGraph.placeStack(event.placeId)
            else -> null
        }?.let(RoutePlan::compose)
    }
}
