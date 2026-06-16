package app.thdev.glassnavlab.coreapp.router.runtime

import app.thdev.glassnavlab.core.router.route.Route
import app.thdev.glassnavlab.core.router.runtime.RouteEventSink
import app.thdev.glassnavlab.core.router.runtime.RoutePlan
import app.thdev.glassnavlab.core.router.runtime.RouteStack
import app.thdev.glassnavlab.core.router.runtime.Router

interface AppRouterRuntime : Router, RouteEventSink {
    val backStack: RouteStack
    val currentRoute: Route
    val pendingActivityRouteRequest: PendingActivityRouteRequest?

    fun navigateDeepLink(uriString: String)
    fun execute(plan: RoutePlan)
    fun consumeActivityRouteRequest(id: Long)
}
