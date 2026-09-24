package app.thdev.glassnavlab

import app.thdev.glassnavlab.core.model.notmid.NotmidAuthProvider
import app.thdev.glassnavlab.core.navigation.runtime.RouteEvent

internal sealed interface NotmidAppAction {
    data object ReloadContent : NotmidAppAction
    data object ContinuePrimaryAuth : NotmidAppAction
    data class RouteRequested(val event: RouteEvent) : NotmidAppAction

    data class ContinueAuth(
        val provider: NotmidAuthProvider,
    ) : NotmidAppAction

    data object BrowseSignedOut : NotmidAppAction

}
