package app.thdev.glassnavlab

import app.thdev.glassnavlab.core.data.notmid.NotmidContentSource
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState

internal data class NotmidAppUiState(
    val contentSource: NotmidContentSource,
    val content: NotmidContentUiState = NotmidContentUiState.Loading,
    val authState: NotmidAuthState,
    val authErrorMessage: String? = null,
    val isAuthenticating: Boolean = false,
)
