package app.thdev.glassnavlab.feature.notmid.router

import app.thdev.glassnavlab.core.navigation.runtime.RouteEvent
import app.thdev.glassnavlab.core.navigation.runtime.RouteEventHandler
import app.thdev.glassnavlab.core.navigation.runtime.RoutePlan
import app.thdev.glassnavlab.core.navigation.runtime.RouteStack
import app.thdev.glassnavlab.core.navigation.notmid.NotmidRouteEvent
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
