package app.thdev.glassnavlab

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.thdev.glassnavlab.core.activity.BaseActivity
import app.thdev.glassnavlab.core.activity.deeplink.PendingDeepLinkEffect
import app.thdev.glassnavlab.core.activity.root.AppRoot
import app.thdev.glassnavlab.core.activity.route.ActivityRouteLauncher
import app.thdev.glassnavlab.core.designsystem.theme.notmidTheme
import app.thdev.glassnavlab.feature.bootstrap.NotmidBootstrapScreen
import app.thdev.glassnavlab.shell.NotmidShellAction
import app.thdev.glassnavlab.shell.NotmidShellScreen
import app.thdev.glassnavlab.shell.NotmidShellViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseActivity() {
    @Inject
    lateinit var activityRouteLauncher: ActivityRouteLauncher

    @Composable
    override fun Content() {
        val shellViewModel: NotmidShellViewModel = viewModel()
        val shellState by shellViewModel.state.collectAsStateWithLifecycle()
        val deepLink = pendingDeepLink

        PendingDeepLinkEffect(
            deepLinkKey = deepLink?.id,
            uri = deepLink?.uri,
            onDeepLink = { uri -> shellViewModel.onAction(NotmidShellAction.DeepLinkRequested(uri)) },
        )

        AppRoot(
            activityRouteRequest = shellState.activityRouteRequest,
            activityRouteLauncher = activityRouteLauncher,
            onActivityRouteLaunched = { id ->
                shellViewModel.onAction(NotmidShellAction.ActivityRouteLaunched(id))
            },
            noticeEffects = shellViewModel.effects,
            modifier = Modifier.fillMaxSize(),
            onNoticeActionDeepLink = { uri ->
                shellViewModel.onAction(NotmidShellAction.DeepLinkRequested(uri))
            },
            theme = { content ->
                notmidTheme(darkTheme = false) {
                    content()
                }
            },
        ) {
            NotmidBootstrapScreen { destinations ->
                NotmidShellScreen(destinations = destinations)
            }
        }
    }
}
