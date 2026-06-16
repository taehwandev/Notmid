package app.thdev.glassnavlab.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import app.thdev.glassnavlab.core.router.route.ActivityRoute
import app.thdev.glassnavlab.feature.webview.NotmidWebViewActivity
import app.thdev.glassnavlab.feature.webview.api.activity.WebViewActivityKeys
import app.thdev.glassnavlab.feature.webview.api.route.WebViewRoute

internal object AppActivityRouteLauncher {
    fun launch(
        context: Context,
        route: ActivityRoute,
    ): Boolean {
        return when (route.activityKey) {
            WebViewActivityKeys.DEFAULT -> {
                val webViewRoute = route as? WebViewRoute ?: return false
                val intent = NotmidWebViewActivity.createIntent(
                    context = context,
                    route = webViewRoute,
                )
                if (context !is Activity) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            }

            else -> false
        }
    }
}
