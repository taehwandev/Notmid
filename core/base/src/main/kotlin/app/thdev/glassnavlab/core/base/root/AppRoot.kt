package app.thdev.glassnavlab.core.base.root

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.thdev.glassnavlab.core.notice.api.effect.NoticeEffect
import app.thdev.glassnavlab.core.runtime.notice.host.NoticeHost
import app.thdev.glassnavlab.core.runtime.router.activity.ActivityRouteLauncher
import app.thdev.glassnavlab.core.runtime.router.activity.ActivityRouteLauncherEffect
import app.thdev.glassnavlab.core.runtime.router.runtime.AppRouterRuntime
import kotlinx.coroutines.flow.Flow

@Composable
fun AppRoot(
    router: AppRouterRuntime,
    activityRouteLauncher: ActivityRouteLauncher,
    noticeEffects: Flow<NoticeEffect>,
    modifier: Modifier = Modifier,
    onNoticeActionDeepLink: (String) -> Unit = {},
    theme: @Composable (@Composable () -> Unit) -> Unit = { content -> content() },
    content: @Composable () -> Unit,
) {
    ActivityRouteLauncherEffect(
        router = router,
        launcher = activityRouteLauncher,
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
