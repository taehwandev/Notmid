package app.thdev.glassnavlab.core.data.impl.notmid

import app.thdev.glassnavlab.core.domain.notmid.NotmidContentRepository
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentSnapshot
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentUpdates
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteResult
import app.thdev.glassnavlab.core.model.notmid.NotmidDestination
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Publishes content reads; callers own IO dispatch and cancellation. */
class ObservableNotmidContentRepository(
    private val repository: NotmidContentRepository,
) : NotmidContentRepository, NotmidContentUpdates {
    private val reads = Mutex()
    private val stateLock = Any()
    private val pendingWrites = mutableListOf<NotmidProtectedWriteResult>()
    private val mutableSnapshot = MutableStateFlow<NotmidContentSnapshot>(NotmidContentSnapshot.Loading)
    override val snapshot = mutableSnapshot.asStateFlow()

    override suspend fun destinations(): List<NotmidDestination> = reads.withLock {
        val previous = synchronized(stateLock) {
            mutableSnapshot.value.also { mutableSnapshot.value = NotmidContentSnapshot.Loading }
        }
        try {
            val destinations = repository.destinations()
            synchronized(stateLock) {
                reconcilePending(destinations).also { mutableSnapshot.value = NotmidContentSnapshot.Ready(it) }
            }
        } catch (cancelled: CancellationException) {
            synchronized(stateLock) {
                mutableSnapshot.value = if (previous is NotmidContentSnapshot.Ready) {
                    NotmidContentSnapshot.Ready(reconcilePending(previous.destinations))
                } else previous
            }
            throw cancelled
        } catch (failure: Exception) {
            synchronized(stateLock) { mutableSnapshot.value = NotmidContentSnapshot.Unavailable }
            throw failure
        }
    }

    /** Records a receipt before the executor returns; an active read merges it before publishing. */
    internal fun applyWriteResult(result: NotmidProtectedWriteResult) {
        if (result is NotmidProtectedWriteResult.ProfileUpdated || result == NotmidProtectedWriteResult.Completed || result == NotmidProtectedWriteResult.Busy) return
        synchronized(stateLock) {
            val current = mutableSnapshot.value
            if (current is NotmidContentSnapshot.Ready) {
                mutableSnapshot.value = NotmidContentSnapshot.Ready(current.destinations.withWriteResult(result))
            } else {
                pendingWrites.add(result)
            }
        }
    }

    private fun reconcilePending(destinations: List<NotmidDestination>): List<NotmidDestination> {
        val updated = pendingWrites.fold(destinations) { current, result -> current.withWriteResult(result) }
        pendingWrites.clear()
        return updated
    }
}
