package app.thdev.glassnavlab.core.runtime.router.activity

import android.content.Context
import app.thdev.glassnavlab.core.router.route.ActivityRoute
import dagger.hilt.android.qualifiers.ActivityContext
import javax.inject.Inject

class DefaultActivityRouteLauncher @Inject constructor(
    @param:ActivityContext private val context: Context,
    private val handlers: Set<@JvmSuppressWildcards ActivityRouteLaunchHandler>,
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
