package app.thdev.glassnavlab.core.auth.notmid

import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState

sealed interface NotmidAuthResult {
    data class Success(
        val state: NotmidAuthState,
        val nextPath: String,
    ) : NotmidAuthResult

    data class Rejected(
        val code: String,
        val message: String,
        val state: NotmidAuthState,
    ) : NotmidAuthResult
}
