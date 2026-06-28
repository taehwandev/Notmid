package app.thdev.glassnavlab.feature.webview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.thdev.glassnavlab.feature.webview.api.route.WebViewRoute

@Composable
fun NotmidWebViewRouteContent(
    route: WebViewRoute,
    modifier: Modifier = Modifier,
    handleBack: Boolean = true,
    onCanGoBackChanged: (Boolean) -> Unit = {},
) {
    NotmidWebViewContent(
        url = route.url,
        mode = route.mode,
        javaScriptEnabled = route.javaScriptEnabled,
        modifier = modifier,
        handleBack = handleBack,
        onCanGoBackChanged = onCanGoBackChanged,
    )
}
