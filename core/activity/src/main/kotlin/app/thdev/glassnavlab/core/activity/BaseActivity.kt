package app.thdev.glassnavlab.core.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.thdev.glassnavlab.core.activity.deeplink.PendingDeepLink

abstract class BaseActivity : ComponentActivity() {
    private var currentPendingDeepLink by mutableStateOf<PendingDeepLink?>(null)
    private var nextPendingDeepLinkId = 0L

    protected open val edgeToEdgeConfig: EdgeToEdgeConfig
        get() = EdgeToEdgeDefaults.lightTransparent()

    protected val pendingDeepLink: PendingDeepLink?
        get() = currentPendingDeepLink

    override fun onCreate(savedInstanceState: Bundle?) {
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
