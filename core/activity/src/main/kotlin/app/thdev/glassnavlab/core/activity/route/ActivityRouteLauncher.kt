package app.thdev.glassnavlab.core.activity.route

import app.thdev.glassnavlab.core.navigation.route.ActivityRoute

fun interface ActivityRouteLauncher {
    fun launch(route: ActivityRoute): Boolean
}
