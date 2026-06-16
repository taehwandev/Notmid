package app.thdev.glassnavlab.feature.webview.api.deeplink

import app.thdev.glassnavlab.core.router.deeplink.DeepLinkRequest
import app.thdev.glassnavlab.core.router.deeplink.DeepLinkSpec
import app.thdev.glassnavlab.core.router.runtime.RoutePlan
import app.thdev.glassnavlab.feature.webview.api.route.WebViewMode
import app.thdev.glassnavlab.feature.webview.api.route.WebViewRoute
import java.net.URI

object WebViewDeepLinkSpec : DeepLinkSpec {
    override val priority: Int = 10

    override fun match(request: DeepLinkRequest): RoutePlan? {
        if (request.pathSegments != listOf(WEB_PATH)) return null

        val url = request.queryParameters["url"]?.firstOrNull()
            ?.takeIf(::isAllowedUrl)
            ?: return null
        val title = request.queryParameters["title"]?.firstOrNull()
        val mode = request.queryParameters["mode"]?.firstOrNull()
            ?.let(::modeFor)
            ?: WebViewMode.Generic
        val javaScriptEnabled = request.queryParameters["js"]?.firstOrNull()
            ?.toBooleanStrictOrNull()
            ?: true

        return RoutePlan.activity(
            WebViewRoute(
                url = url,
                title = title,
                mode = mode,
                javaScriptEnabled = javaScriptEnabled,
            ),
        )
    }

    private fun modeFor(value: String): WebViewMode? {
        return WebViewMode.entries.firstOrNull { mode ->
            mode.name.equals(value, ignoreCase = true)
        }
    }

    private fun isAllowedUrl(value: String): Boolean {
        val uri = runCatching { URI(value) }.getOrNull() ?: return false
        return uri.scheme == "https" || uri.scheme == "http"
    }
}

private const val WEB_PATH = "web"
