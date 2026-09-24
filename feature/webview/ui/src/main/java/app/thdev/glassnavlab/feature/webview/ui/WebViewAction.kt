package app.thdev.glassnavlab.feature.webview.ui

sealed interface WebViewAction {
    data class HistoryChanged(val canGoBack: Boolean) : WebViewAction
    data object BackRequested : WebViewAction
}
