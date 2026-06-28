package app.thdev.glassnavlab.feature.notmid.router

import app.thdev.glassnavlab.core.router.runtime.RouteEvent
import app.thdev.glassnavlab.core.router.runtime.RouteEventHandler
import app.thdev.glassnavlab.core.router.runtime.RoutePlan
import app.thdev.glassnavlab.feature.feed.api.event.FeedRouteEvent
import javax.inject.Inject

class FeedRouteEventHandler @Inject constructor(
    private val routeGraph: NotmidRouteGraph,
) : RouteEventHandler {
    override fun planFor(event: RouteEvent): RoutePlan? {
        return when (event) {
            is FeedRouteEvent.ClipRequested -> routeGraph.clipStack(event.clipId)
            else -> null
        }?.let(RoutePlan::compose)
    }
}
