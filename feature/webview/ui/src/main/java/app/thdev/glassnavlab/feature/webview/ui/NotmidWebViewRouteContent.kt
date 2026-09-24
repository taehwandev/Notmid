package app.thdev.glassnavlab.feature.webview.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.thdev.glassnavlab.feature.webview.api.route.WebViewRoute

@Composable
fun NotmidWebViewRouteContent(
    route: WebViewRoute,
    modifier: Modifier = Modifier,
    handleBack: Boolean = true,
    onCanGoBackChanged: (Boolean) -> Unit = {},
) {
    val viewModel: NotmidWebViewViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    NotmidWebViewContent(
        url = route.url,
        mode = route.mode,
        javaScriptEnabled = route.javaScriptEnabled,
        modifier = modifier,
        handleBack = handleBack,
        canGoBack = state.canGoBack,
        effects = viewModel.effects,
        onBackRequested = { viewModel.onAction(WebViewAction.BackRequested) },
        onCanGoBackChanged = { canGoBack ->
            viewModel.onAction(WebViewAction.HistoryChanged(canGoBack))
            onCanGoBackChanged(canGoBack)
        },
    )
}
