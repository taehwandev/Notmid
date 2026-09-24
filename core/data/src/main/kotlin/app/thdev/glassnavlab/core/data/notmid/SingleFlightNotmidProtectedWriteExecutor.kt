package app.thdev.glassnavlab.core.data.notmid

import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteExecutor
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteRequest
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteResult
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Shared by feature callers; the caller retains coroutine and cancellation ownership. */
class SingleFlightNotmidProtectedWriteExecutor(
    private val delegate: NotmidProtectedWriteExecutor,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : NotmidProtectedWriteExecutor {
    private val mutex = Mutex()

    override suspend fun execute(
        authState: NotmidAuthState,
        request: NotmidProtectedWriteRequest,
    ): NotmidProtectedWriteResult {
        if (!mutex.tryLock()) return NotmidProtectedWriteResult.Busy
        return try {
            withContext(dispatcher) { delegate.execute(authState, request) }
        } finally {
            mutex.unlock()
        }
    }
}
