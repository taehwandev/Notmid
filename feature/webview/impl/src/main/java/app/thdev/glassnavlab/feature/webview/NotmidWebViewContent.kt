package app.thdev.glassnavlab.feature.webview

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import app.thdev.glassnavlab.feature.webview.api.route.WebViewMode

@Composable
fun NotmidWebViewContent(
    url: String,
    modifier: Modifier = Modifier,
    mode: WebViewMode = WebViewMode.Generic,
    javaScriptEnabled: Boolean = true,
    handleBack: Boolean = true,
    onCanGoBackChanged: (Boolean) -> Unit = {},
) {
    val controller = remember { NotmidWebViewController() }
    val currentOnCanGoBackChanged by rememberUpdatedState(onCanGoBackChanged)
    var canGoBack by remember { mutableStateOf(false) }

    fun notifyCanGoBack(value: Boolean) {
        canGoBack = value
        currentOnCanGoBackChanged(value)
    }

    val backDispatcherOwner = LocalOnBackPressedDispatcherOwner.current
    if (handleBack && backDispatcherOwner != null) {
        BackHandler(enabled = canGoBack) {
            controller.goBack()
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            controller.create(
                context = context,
                url = url,
                mode = mode,
                javaScriptEnabled = javaScriptEnabled,
                onCanGoBackChanged = ::notifyCanGoBack,
            )
        },
        update = { webView ->
            controller.update(
                webView = webView,
                url = url,
                mode = mode,
                javaScriptEnabled = javaScriptEnabled,
                onCanGoBackChanged = ::notifyCanGoBack,
            )
        },
    )

    DisposableEffect(Unit) {
        onDispose {
            controller.destroy()
            currentOnCanGoBackChanged(false)
        }
    }
}
