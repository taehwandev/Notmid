package app.thdev.glassnavlab.feature.notmid.router

import app.thdev.glassnavlab.core.router.runtime.RouteEventHandler
import app.thdev.glassnavlab.core.runtime.router.runtime.AppRouterRuntime
import javax.inject.Inject

class NotmidAppRouterFactory @Inject constructor(
    private val routeGraph: NotmidRouteGraph,
    private val routeEventHandlers: Set<@JvmSuppressWildcards RouteEventHandler>,
) {
    fun createRuntime(): AppRouterRuntime {
        return routeGraph.routerBundle(routeEventHandlers).createRuntime()
    }
}
