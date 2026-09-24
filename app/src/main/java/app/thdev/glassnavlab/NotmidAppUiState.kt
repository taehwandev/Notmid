package app.thdev.glassnavlab

import app.thdev.glassnavlab.core.data.api.notmid.NotmidContentSource

internal data class NotmidAppUiState(
    val contentSource: NotmidContentSource,
    val content: NotmidContentUiState = NotmidContentUiState.Loading,
)
