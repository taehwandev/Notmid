package app.thdev.glassnavlab.core.base.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.thdev.glassnavlab.core.base.deeplink.PendingDeepLink
import app.thdev.glassnavlab.core.base.deeplink.PendingDeepLinkEffect
import app.thdev.glassnavlab.core.base.root.AppRoot
import app.thdev.glassnavlab.core.notice.api.effect.NoticeEffect
import app.thdev.glassnavlab.core.runtime.router.activity.ActivityRouteLauncher
import app.thdev.glassnavlab.core.runtime.router.runtime.AppRouterRuntime
import kotlinx.coroutines.flow.Flow

abstract class BaseActivity : ComponentActivity() {
    private var currentPendingDeepLink by mutableStateOf<PendingDeepLink?>(null)
    private var nextPendingDeepLinkId = 0L

    protected open val edgeToEdgeConfig: EdgeToEdgeConfig
        get() = EdgeToEdgeDefaults.lightTransparent()

    protected val pendingDeepLink: PendingDeepLink?
        get() = currentPendingDeepLink

    final override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyEdgeToEdge(edgeToEdgeConfig)
        updatePendingDeepLink(intent)
        onBeforeSetComposeContent(savedInstanceState)
        setContent {
            Content()
        }
    }

    final override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        updatePendingDeepLink(intent)
        onNewIntentReceived(intent)
    }

    protected open fun onBeforeSetComposeContent(savedInstanceState: Bundle?) = Unit

    protected open fun onNewIntentReceived(intent: Intent) = Unit

    protected open fun deepLinkUriFrom(intent: Intent): String? {
        return intent.data?.toString()
    }

    @Composable
    protected abstract fun Content()

    protected fun applyEdgeToEdge(config: EdgeToEdgeConfig) {
        enableEdgeToEdge(
            statusBarStyle = config.statusBarStyle,
            navigationBarStyle = config.navigationBarStyle,
        )
    }

    @Composable
    protected fun BaseAppRoot(
        router: AppRouterRuntime,
        activityRouteLauncher: ActivityRouteLauncher,
        noticeEffects: Flow<NoticeEffect>,
        modifier: Modifier = Modifier.fillMaxSize(),
        onNoticeActionDeepLink: (String) -> Unit = router::navigateDeepLink,
        theme: @Composable (@Composable () -> Unit) -> Unit = { content -> content() },
        content: @Composable () -> Unit,
    ) {
        BasePendingDeepLinkEffect(router)
        AppRoot(
            router = router,
            activityRouteLauncher = activityRouteLauncher,
            noticeEffects = noticeEffects,
            modifier = modifier,
            onNoticeActionDeepLink = onNoticeActionDeepLink,
            theme = theme,
            content = content,
        )
    }

    @Composable
    protected fun BasePendingDeepLinkEffect(
        router: AppRouterRuntime,
    ) {
        val pendingDeepLink = pendingDeepLink
        PendingDeepLinkEffect(
            deepLinkKey = pendingDeepLink?.id,
            uri = pendingDeepLink?.uri,
            onDeepLink = router::navigateDeepLink,
        )
    }

    private fun updatePendingDeepLink(intent: Intent?) {
        val uri = intent
            ?.let(::deepLinkUriFrom)
            ?.takeIf(String::isNotBlank)
            ?: return
        currentPendingDeepLink = PendingDeepLink(
            uri = uri,
            id = ++nextPendingDeepLinkId,
        )
    }
}
