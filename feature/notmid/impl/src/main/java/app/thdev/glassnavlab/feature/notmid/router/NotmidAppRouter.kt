package app.thdev.glassnavlab.feature.notmid.router

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.thdev.glassnavlab.coreapp.router.runtime.AppRouterRuntime
import app.thdev.glassnavlab.coreapp.router.config.AppRouterBundle
import app.thdev.glassnavlab.feature.notmid.api.route.NotmidRoute

@Composable
fun rememberNotmidAppRouter(): AppRouterRuntime {
    val bundle = remember { NotmidRouteGraph.routerBundle }
    return remember(bundle) { bundle.createRuntime() }
}

internal fun createNotmidAppRouter(
    bundle: AppRouterBundle = NotmidRouteGraph.routerBundle,
): AppRouterRuntime {
    return bundle.createRuntime()
}

fun AppRouterRuntime.notmidRouteStack(): List<NotmidRoute> {
    return backStack.entries.filterIsInstance<NotmidRoute>()
}
