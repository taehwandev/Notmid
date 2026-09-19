package app.thdev.glassnavlab.core.data.notmid

import app.thdev.glassnavlab.core.domain.notmid.NotmidContentRepository
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentSnapshot
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentUpdates
import app.thdev.glassnavlab.core.model.notmid.NotmidDestination
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Publishes the existing read result; callers still own IO dispatch and cancellation. */
class ObservableNotmidContentRepository(
    private val repository: NotmidContentRepository,
) : NotmidContentRepository, NotmidContentUpdates {
    private val reads = Mutex()
    private val mutableSnapshot = MutableStateFlow<NotmidContentSnapshot>(NotmidContentSnapshot.Loading)
    override val snapshot = mutableSnapshot.asStateFlow()

    override suspend fun destinations(): List<NotmidDestination> = reads.withLock {
        val previous = mutableSnapshot.value
        mutableSnapshot.value = NotmidContentSnapshot.Loading
        try {
            repository.destinations().also { destinations ->
                mutableSnapshot.value = NotmidContentSnapshot.Ready(destinations)
            }
        } catch (cancelled: CancellationException) {
            mutableSnapshot.value = previous
            throw cancelled
        } catch (failure: Exception) {
            mutableSnapshot.value = NotmidContentSnapshot.Unavailable
            throw failure
        }
    }
}
