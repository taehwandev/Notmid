package app.thdev.glassnavlab.coreapp.router.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.thdev.glassnavlab.core.router.runtime.RouteStack
import app.thdev.glassnavlab.coreapp.router.planner.AppRoutePlanner

@Composable
fun rememberAppRouterRuntime(
    initialStack: RouteStack,
    routePlanner: AppRoutePlanner,
): AppRouterRuntime {
    return remember(initialStack, routePlanner) {
        DefaultAppRouterRuntime(
            initialStack = initialStack,
            routePlanner = routePlanner,
        )
    }
}
