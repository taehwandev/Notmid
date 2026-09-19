package app.thdev.glassnavlab.core.domain.notmid

import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState

/** Executes one write in the caller's coroutine; failures and cancellation propagate. */
fun interface NotmidProtectedWriteExecutor {
    suspend fun execute(
        authState: NotmidAuthState,
        request: NotmidProtectedWriteRequest,
    ): NotmidProtectedWriteResult
}
