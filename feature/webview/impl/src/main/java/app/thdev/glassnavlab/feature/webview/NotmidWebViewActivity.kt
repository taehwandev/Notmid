package app.thdev.glassnavlab.feature.webview

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import app.thdev.glassnavlab.feature.webview.api.route.WebViewMode
import app.thdev.glassnavlab.feature.webview.api.route.WebViewRoute

class NotmidWebViewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val route = intent.toWebViewRoute() ?: run {
            finish()
            return
        }

        title = route.title.orEmpty()
        setContent {
            NotmidWebViewRouteContent(route = route)
        }
    }

    companion object {
        private const val EXTRA_URL = "app.thdev.glassnavlab.feature.webview.URL"
        private const val EXTRA_TITLE = "app.thdev.glassnavlab.feature.webview.TITLE"
        private const val EXTRA_MODE = "app.thdev.glassnavlab.feature.webview.MODE"
        private const val EXTRA_JAVA_SCRIPT_ENABLED =
            "app.thdev.glassnavlab.feature.webview.JAVA_SCRIPT_ENABLED"

        fun createIntent(
            context: Context,
            route: WebViewRoute,
        ): Intent {
            return Intent(context, NotmidWebViewActivity::class.java).apply {
                putExtra(EXTRA_URL, route.url)
                putExtra(EXTRA_TITLE, route.title)
                putExtra(EXTRA_MODE, route.mode.name)
                putExtra(EXTRA_JAVA_SCRIPT_ENABLED, route.javaScriptEnabled)
            }
        }

        private fun Intent.toWebViewRoute(): WebViewRoute? {
            val url = getStringExtra(EXTRA_URL)?.takeIf(String::isNotBlank) ?: return null
            val title = getStringExtra(EXTRA_TITLE)
            val mode = getStringExtra(EXTRA_MODE)
                ?.let { name ->
                    WebViewMode.entries.firstOrNull { mode ->
                        mode.name == name
                    }
                }
                ?: WebViewMode.Generic

            return WebViewRoute(
                url = url,
                title = title,
                mode = mode,
                javaScriptEnabled = getBooleanExtra(EXTRA_JAVA_SCRIPT_ENABLED, true),
            )
        }
    }
}
