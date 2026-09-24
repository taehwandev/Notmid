package app.thdev.glassnavlab.core.activity.root

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.thdev.glassnavlab.core.activity.route.ActivityRouteLauncher
import app.thdev.glassnavlab.core.activity.route.ActivityRouteLauncherEffect
import app.thdev.glassnavlab.core.activity.route.PendingActivityRouteRequest
import app.thdev.glassnavlab.core.notice.api.effect.NoticeEffect
import app.thdev.glassnavlab.core.notice.ui.host.NoticeHost
import kotlinx.coroutines.flow.Flow

@Composable
fun AppRoot(
    activityRouteRequest: PendingActivityRouteRequest?,
    activityRouteLauncher: ActivityRouteLauncher,
    onActivityRouteLaunched: (Long) -> Unit,
    noticeEffects: Flow<NoticeEffect>,
    onNoticeActionDeepLink: (String) -> Unit,
    modifier: Modifier = Modifier,
    theme: @Composable (@Composable () -> Unit) -> Unit = { content -> content() },
    content: @Composable () -> Unit,
) {
    ActivityRouteLauncherEffect(
        request = activityRouteRequest,
        launcher = activityRouteLauncher,
        onLaunched = onActivityRouteLaunched,
    )

    theme {
        NoticeHost(
            effects = noticeEffects,
            modifier = modifier,
            onActionDeepLink = onNoticeActionDeepLink,
            content = content,
        )
    }
}
