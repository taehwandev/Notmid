package app.thdev.glassnavlab.feature.notmid.router

import app.thdev.glassnavlab.core.router.runtime.RouteEvent
import app.thdev.glassnavlab.core.router.runtime.RouteEventHandler
import app.thdev.glassnavlab.core.router.runtime.RoutePlan
import app.thdev.glassnavlab.core.router.runtime.RouteStack
import app.thdev.glassnavlab.feature.notmid.api.event.NotmidRouteEvent
import javax.inject.Inject

class NotmidRouteEventHandler @Inject constructor(
    private val routeGraph: NotmidRouteGraph,
) : RouteEventHandler {
    override fun planFor(event: RouteEvent): RoutePlan? {
        return when (event) {
            is NotmidRouteEvent.DestinationSelected -> {
                RouteStack.single(routeGraph.destination(event.destinationId))
            }

            NotmidRouteEvent.SettingsRequested -> {
                routeGraph.settingsStack()
            }

            else -> null
        }?.let(RoutePlan::compose)
    }
}
