package app.thdev.glassnavlab.feature.webview

import android.app.Activity
import android.content.Context
import android.content.Intent
import app.thdev.glassnavlab.core.router.route.ActivityRoute
import app.thdev.glassnavlab.coreapp.router.activity.ActivityRouteLaunchHandler
import app.thdev.glassnavlab.feature.webview.api.activity.WebViewActivityKeys
import app.thdev.glassnavlab.feature.webview.api.route.WebViewRoute

class WebViewActivityRouteLaunchHandler : ActivityRouteLaunchHandler {
    override fun launch(
        context: Context,
        route: ActivityRoute,
    ): Boolean {
        if (route.activityKey != WebViewActivityKeys.DEFAULT) return false
        val webViewRoute = route as? WebViewRoute ?: return false
        val intent = NotmidWebViewActivity.createIntent(
            context = context,
            route = webViewRoute,
        )
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return true
    }
}
