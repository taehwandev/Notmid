package app.thdev.glassnavlab.feature.webview.ui

sealed interface WebViewEffect {
    data object GoBack : WebViewEffect
}
