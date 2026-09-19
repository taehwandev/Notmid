package app.thdev.glassnavlab.core.runtime.router.activity

import android.content.Context
import app.thdev.glassnavlab.core.navigation.route.ActivityRoute

fun interface ActivityRouteLaunchHandler {
    fun launch(
        context: Context,
        route: ActivityRoute,
    ): Boolean
}
