package app.thdev.glassnavlab.core.activity.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

@Composable
fun ActivityRouteLauncherEffect(
    request: PendingActivityRouteRequest?,
    launcher: ActivityRouteLauncher,
    onLaunched: (Long) -> Unit,
) {
    val currentOnLaunched by rememberUpdatedState(onLaunched)

    LaunchedEffect(request) {
        val pendingRequest = request ?: return@LaunchedEffect
        if (launcher.launch(pendingRequest.route)) {
            currentOnLaunched(pendingRequest.id)
        }
    }
}
