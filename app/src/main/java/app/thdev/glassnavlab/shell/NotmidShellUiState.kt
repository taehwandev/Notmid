package app.thdev.glassnavlab.shell

import app.thdev.glassnavlab.core.activity.route.PendingActivityRouteRequest
import app.thdev.glassnavlab.core.navigation.notmid.NotmidRoute

internal data class NotmidShellUiState(
    val activeRoute: NotmidRoute,
    val selectedDestinationId: String,
    val shouldShowLogin: Boolean,
    val settingsRouteLabel: String,
    val activityRouteRequest: PendingActivityRouteRequest?,
)
