package app.thdev.glassnavlab.shell

import app.thdev.glassnavlab.core.navigation.notmid.NotmidRoute

internal data class NotmidShellUiState(
    val navigationStack: List<NotmidRoute>,
    val activeRoute: NotmidRoute,
    val selectedDestinationId: String,
    val shouldShowLogin: Boolean,
)
