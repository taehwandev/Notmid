package app.thdev.glassnavlab.router

import app.thdev.glassnavlab.core.navigation.runtime.RouteEventHandler
import app.thdev.glassnavlab.router.runtime.AppRouterRuntime
import javax.inject.Inject

class NotmidAppRouterFactory @Inject constructor(
    private val routeGraph: NotmidRouteGraph,
    private val routeEventHandlers: Set<@JvmSuppressWildcards RouteEventHandler>,
) {
    fun createRuntime(): AppRouterRuntime {
        return routeGraph.routerBundle(routeEventHandlers).createRuntime()
    }
}
