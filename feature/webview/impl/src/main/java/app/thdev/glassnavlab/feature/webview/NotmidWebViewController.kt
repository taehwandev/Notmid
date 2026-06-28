package app.thdev.glassnavlab.feature.webview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import app.thdev.glassnavlab.feature.webview.api.route.WebViewMode

internal class NotmidWebViewController {
    private var webView: WebView? = null
    private var loadedRouteUrl: String? = null

    fun create(
        context: Context,
        url: String,
        mode: WebViewMode,
        javaScriptEnabled: Boolean,
        onCanGoBackChanged: (Boolean) -> Unit,
    ): WebView {
        return WebView(context).apply {
            webView = this
            configureFor(
                mode = mode,
                javaScriptEnabled = javaScriptEnabled,
                onCanGoBackChanged = onCanGoBackChanged,
            )
            loadRouteUrl(
                url = url,
                onCanGoBackChanged = onCanGoBackChanged,
            )
        }
    }

    fun update(
        webView: WebView,
        url: String,
        mode: WebViewMode,
        javaScriptEnabled: Boolean,
        onCanGoBackChanged: (Boolean) -> Unit,
    ) {
        webView.applyRouteSettings(
            mode = mode,
            javaScriptEnabled = javaScriptEnabled,
        )
        webView.loadRouteUrl(
            url = url,
            onCanGoBackChanged = onCanGoBackChanged,
        )
    }

    fun goBack() {
        webView?.goBack()
    }

    fun destroy() {
        webView?.run {
            stopLoading()
            webChromeClient = null
            webViewClient = WebViewClient()
            destroy()
        }
        webView = null
        loadedRouteUrl = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun WebView.configureFor(
        mode: WebViewMode,
        javaScriptEnabled: Boolean,
        onCanGoBackChanged: (Boolean) -> Unit,
    ) {
        setBackgroundColor(Color.WHITE)
        applyRouteSettings(
            mode = mode,
            javaScriptEnabled = javaScriptEnabled,
        )
        webViewClient = Client(onCanGoBackChanged)
        webChromeClient = WebChromeClient()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun WebView.applyRouteSettings(
        mode: WebViewMode,
        javaScriptEnabled: Boolean,
    ) {
        settings.javaScriptEnabled = javaScriptEnabled
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = mode != WebViewMode.Auth
    }

    private fun WebView.loadRouteUrl(
        url: String,
        onCanGoBackChanged: (Boolean) -> Unit,
    ) {
        if (loadedRouteUrl == url) return
        loadedRouteUrl = url
        loadUrl(url)
        onCanGoBackChanged(canGoBack())
    }

    private class Client(
        private val onCanGoBackChanged: (Boolean) -> Unit,
    ) : WebViewClient() {
        override fun doUpdateVisitedHistory(
            view: WebView,
            url: String?,
            isReload: Boolean,
        ) {
            onCanGoBackChanged(view.canGoBack())
        }

        override fun onPageFinished(
            view: WebView,
            url: String?,
        ) {
            onCanGoBackChanged(view.canGoBack())
        }
    }
}
