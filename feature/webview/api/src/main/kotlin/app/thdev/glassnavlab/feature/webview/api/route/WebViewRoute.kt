package app.thdev.glassnavlab.feature.webview.api.route

import app.thdev.glassnavlab.core.router.route.ActivityRoute
import app.thdev.glassnavlab.feature.webview.api.activity.WebViewActivityKeys

data class WebViewRoute(
    val url: String,
    val title: String? = null,
    val mode: WebViewMode = WebViewMode.Generic,
    val javaScriptEnabled: Boolean = true,
) : ActivityRoute {
    override val route: String = "notmid/web"
    override val activityKey: String = WebViewActivityKeys.DEFAULT
}
