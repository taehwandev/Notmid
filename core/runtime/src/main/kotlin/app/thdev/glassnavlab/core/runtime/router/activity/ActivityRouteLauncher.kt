package app.thdev.glassnavlab.core.runtime.router.activity

import app.thdev.glassnavlab.core.router.route.ActivityRoute

fun interface ActivityRouteLauncher {
    fun launch(route: ActivityRoute): Boolean
}
