package app.thdev.glassnavlab.coreapp.router.activity

import android.content.Context
import app.thdev.glassnavlab.core.router.route.ActivityRoute

fun interface ActivityRouteLaunchHandler {
    fun launch(
        context: Context,
        route: ActivityRoute,
    ): Boolean
}
