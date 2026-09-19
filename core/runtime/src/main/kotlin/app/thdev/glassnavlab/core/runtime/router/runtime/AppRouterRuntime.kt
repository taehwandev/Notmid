package app.thdev.glassnavlab.core.runtime.router.runtime

import app.thdev.glassnavlab.core.navigation.route.Route
import app.thdev.glassnavlab.core.navigation.runtime.RouteEventSink
import app.thdev.glassnavlab.core.navigation.runtime.RoutePlan
import app.thdev.glassnavlab.core.navigation.runtime.RouteStack
import app.thdev.glassnavlab.core.navigation.runtime.Router

interface AppRouterRuntime : Router, RouteEventSink {
    val backStack: RouteStack
    val currentRoute: Route
    val pendingActivityRouteRequest: PendingActivityRouteRequest?

    fun navigateDeepLink(uriString: String)
    fun execute(plan: RoutePlan)
    fun consumeActivityRouteRequest(id: Long)
}
