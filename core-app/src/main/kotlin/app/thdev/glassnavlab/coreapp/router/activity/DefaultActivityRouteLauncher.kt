package app.thdev.glassnavlab.coreapp.router.activity

import android.content.Context
import app.thdev.glassnavlab.core.router.route.ActivityRoute

class DefaultActivityRouteLauncher(
    private val context: Context,
    private val handlers: List<ActivityRouteLaunchHandler>,
) : ActivityRouteLauncher {
    init {
        require(handlers.isNotEmpty()) { "At least one activity route launch handler is required." }
    }

    override fun launch(route: ActivityRoute): Boolean {
        return handlers.any { handler ->
            handler.launch(context, route)
        }
    }
}
