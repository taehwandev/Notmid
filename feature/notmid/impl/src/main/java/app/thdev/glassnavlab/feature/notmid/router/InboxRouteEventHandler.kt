package app.thdev.glassnavlab.feature.notmid.router

import app.thdev.glassnavlab.core.router.runtime.RouteEvent
import app.thdev.glassnavlab.core.router.runtime.RouteEventHandler
import app.thdev.glassnavlab.core.router.runtime.RoutePlan
import app.thdev.glassnavlab.feature.inbox.api.event.InboxRouteEvent
import javax.inject.Inject

class InboxRouteEventHandler @Inject constructor(
    private val routeGraph: NotmidRouteGraph,
) : RouteEventHandler {
    override fun planFor(event: RouteEvent): RoutePlan? {
        return when (event) {
            is InboxRouteEvent.ChatThreadRequested -> routeGraph.chatThreadStack(event.threadId)
            else -> null
        }?.let(RoutePlan::compose)
    }
}
