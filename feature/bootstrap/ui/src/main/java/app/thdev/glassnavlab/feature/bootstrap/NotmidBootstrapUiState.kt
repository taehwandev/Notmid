package app.thdev.glassnavlab.feature.bootstrap

import app.thdev.glassnavlab.core.data.api.notmid.NotmidContentSource

internal data class NotmidBootstrapUiState(
    val contentSource: NotmidContentSource,
    val content: NotmidContentUiState = NotmidContentUiState.Loading,
)
