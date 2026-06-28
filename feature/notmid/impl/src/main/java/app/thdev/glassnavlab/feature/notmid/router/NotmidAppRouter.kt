package app.thdev.glassnavlab.feature.notmid.router

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.thdev.glassnavlab.core.router.runtime.RouteEventHandler
import app.thdev.glassnavlab.core.runtime.router.runtime.AppRouterRuntime
import app.thdev.glassnavlab.feature.notmid.api.route.NotmidRoute

@Composable
fun rememberNotmidAppRouter(
    factory: NotmidAppRouterFactory,
): AppRouterRuntime {
    return remember(factory) { factory.createRuntime() }
}

internal fun createNotmidAppRouter(
    routeGraph: NotmidRouteGraph,
    routeEventHandlers: Collection<RouteEventHandler>,
): AppRouterRuntime {
    return routeGraph.routerBundle(routeEventHandlers).createRuntime()
}

fun AppRouterRuntime.notmidRouteStack(): List<NotmidRoute> {
    return backStack.entries.filterIsInstance<NotmidRoute>()
}
