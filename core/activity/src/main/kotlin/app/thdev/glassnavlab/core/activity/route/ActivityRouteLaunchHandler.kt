package app.thdev.glassnavlab.core.activity.route

import android.content.Context
import app.thdev.glassnavlab.core.navigation.route.ActivityRoute

fun interface ActivityRouteLaunchHandler {
    fun launch(
        context: Context,
        route: ActivityRoute,
    ): Boolean
}
