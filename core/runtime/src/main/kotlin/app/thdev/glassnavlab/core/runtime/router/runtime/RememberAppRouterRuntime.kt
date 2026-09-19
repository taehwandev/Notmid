package app.thdev.glassnavlab.core.runtime.router.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.thdev.glassnavlab.core.navigation.runtime.RouteStack
import app.thdev.glassnavlab.core.runtime.router.planner.AppRoutePlanner

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
