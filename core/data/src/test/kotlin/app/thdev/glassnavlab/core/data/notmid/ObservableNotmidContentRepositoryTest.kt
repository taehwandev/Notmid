package app.thdev.glassnavlab.core.data.notmid

import app.thdev.glassnavlab.core.domain.notmid.NotmidContentRepository
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentSnapshot
import app.thdev.glassnavlab.core.model.notmid.NotmidDestination
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObservableNotmidContentRepositoryTest {
    @Test
    fun observationDoesNotLoadAndOneReadPublishesItsExactResult() = runTest {
        var reads = 0
        val result = emptyList<NotmidDestination>()
        val repository = ObservableNotmidContentRepository(object : NotmidContentRepository {
            override suspend fun destinations(): List<NotmidDestination> {
                reads++
                return result
            }
        })
        assertSame(NotmidContentSnapshot.Loading, repository.snapshot.value)
        assertEquals(0, reads)
        assertSame(result, repository.destinations())
        assertEquals(NotmidContentSnapshot.Ready(result), repository.snapshot.value)
        assertEquals(1, reads)
    }

    @Test
    fun failurePublishesUnavailableAndRetainsOriginalException() {
        val failure = IllegalStateException("private transport detail")
        val repository = ObservableNotmidContentRepository(object : NotmidContentRepository {
            override suspend fun destinations(): List<NotmidDestination> = throw failure
        })
        val actual = assertThrows(IllegalStateException::class.java) { runSuspend { repository.destinations() } }
        assertSame(failure, actual)
        assertSame(NotmidContentSnapshot.Unavailable, repository.snapshot.value)
    }

    @Test
    fun cancellationRestoresPreviousSnapshotAndIsNotPublishedAsFailure() = runTest {
        var cancel = false
        val repository = ObservableNotmidContentRepository(object : NotmidContentRepository {
            override suspend fun destinations(): List<NotmidDestination> {
                if (cancel) throw CancellationException("cancelled")
                return emptyList()
            }
        })
        repository.destinations()
        val previous = repository.snapshot.value
        cancel = true
        assertThrows(CancellationException::class.java) { runSuspend { repository.destinations() } }
        assertSame(previous, repository.snapshot.value)
    }

    @Test
    fun overlappingReadsPublishInOrderWithoutStaleCompletion() = runTest {
        val release = CompletableDeferred<Unit>()
        var reads = 0
        val repository = ObservableNotmidContentRepository(object : NotmidContentRepository {
            override suspend fun destinations(): List<NotmidDestination> {
                reads++
                if (reads == 1) release.await()
                return emptyList()
            }
        })
        val first = async { repository.destinations() }
        val second = async { repository.destinations() }
        runCurrent()
        assertEquals(1, reads)
        assertSame(NotmidContentSnapshot.Loading, repository.snapshot.value)
        release.complete(Unit)
        first.await()
        second.await()
        assertEquals(2, reads)
        assertEquals(NotmidContentSnapshot.Ready(emptyList()), repository.snapshot.value)
    }
}
