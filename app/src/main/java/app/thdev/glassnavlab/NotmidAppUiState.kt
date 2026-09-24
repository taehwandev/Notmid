package app.thdev.glassnavlab

import app.thdev.glassnavlab.core.data.notmid.NotmidContentSource

internal data class NotmidAppUiState(
    val contentSource: NotmidContentSource,
    val content: NotmidContentUiState = NotmidContentUiState.Loading,
)
