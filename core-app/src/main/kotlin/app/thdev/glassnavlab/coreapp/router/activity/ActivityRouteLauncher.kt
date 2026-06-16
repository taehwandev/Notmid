package app.thdev.glassnavlab.coreapp.router.activity

import app.thdev.glassnavlab.core.router.route.ActivityRoute

fun interface ActivityRouteLauncher {
    fun launch(route: ActivityRoute): Boolean
}
