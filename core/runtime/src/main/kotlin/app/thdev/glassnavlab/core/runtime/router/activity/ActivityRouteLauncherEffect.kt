package app.thdev.glassnavlab.core.runtime.router.activity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import app.thdev.glassnavlab.core.runtime.router.runtime.AppRouterRuntime

@Composable
fun ActivityRouteLauncherEffect(
    router: AppRouterRuntime,
    launcher: ActivityRouteLauncher,
) {
    val request = router.pendingActivityRouteRequest

    LaunchedEffect(request) {
        val pendingRequest = request ?: return@LaunchedEffect
        if (launcher.launch(pendingRequest.route)) {
            router.consumeActivityRouteRequest(pendingRequest.id)
        }
    }
}
