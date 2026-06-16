package app.thdev.glassnavlab.core.runtime.router.activity

import app.thdev.glassnavlab.core.router.route.ActivityRoute

class RecordingActivityRouteLauncher(
    private val launchResult: Boolean = true,
) : ActivityRouteLauncher {
    val launchedRoutes: List<ActivityRoute>
        get() = mutableLaunchedRoutes.toList()

    private val mutableLaunchedRoutes = mutableListOf<ActivityRoute>()

    override fun launch(route: ActivityRoute): Boolean {
        mutableLaunchedRoutes += route
        return launchResult
    }
}
