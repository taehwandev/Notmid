package app.thdev.glassnavlab.core.data.impl.notmid

import app.thdev.glassnavlab.core.domain.notmid.NotmidContentRepository
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentSnapshot
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteResult
import app.thdev.glassnavlab.core.model.notmid.NotmidNavigationIcon
import app.thdev.glassnavlab.core.model.notmid.NotmidThread
import app.thdev.glassnavlab.core.model.notmid.NotmidThreadMessage
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
        assertEquals(previous, repository.snapshot.value)
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

    @Test
    fun receiptDuringReadSurvivesStaleResponseAndIsAppliedOnlyOnce() = runTest {
        val release = CompletableDeferred<Unit>()
        val repository = ObservableNotmidContentRepository(object : NotmidContentRepository {
            override suspend fun destinations(): List<NotmidDestination> {
                release.await()
                return listOf(inbox)
            }
        })
        val read = async { repository.destinations() }
        runCurrent()
        repository.applyWriteResult(NotmidProtectedWriteResult.ThreadStarted(thread, message))
        repository.applyWriteResult(NotmidProtectedWriteResult.MessageSent(message))
        release.complete(Unit)
        val destinations = read.await()
        assertEquals(listOf(message), destinations.single().threadMessages)
        assertEquals(message.body, destinations.single().threads.single().preview)
        assertEquals(NotmidContentSnapshot.Ready(destinations), repository.snapshot.value)
    }

    @Test
    fun receiptDuringCancelledRefreshIsMergedIntoRestoredContent() = runTest {
        var refresh = false
        val waiting = CompletableDeferred<Unit>()
        val repository = ObservableNotmidContentRepository(object : NotmidContentRepository {
            override suspend fun destinations(): List<NotmidDestination> {
                if (refresh) waiting.await()
                return listOf(inbox)
            }
        })
        repository.destinations()
        refresh = true
        val read = async { repository.destinations() }
        runCurrent()
        repository.applyWriteResult(NotmidProtectedWriteResult.ThreadStarted(thread, message))
        read.cancel()
        read.join()
        val snapshot = repository.snapshot.value as NotmidContentSnapshot.Ready
        assertEquals(listOf(message), snapshot.destinations.single().threadMessages)
    }

    @Test
    fun failedRefreshKeepsReceiptForSuccessfulRetry() = runTest {
        var fails = true
        val repository = ObservableNotmidContentRepository(object : NotmidContentRepository {
            override suspend fun destinations(): List<NotmidDestination> {
                if (fails) throw IllegalStateException("unavailable")
                return listOf(inbox)
            }
        })
        repository.applyWriteResult(NotmidProtectedWriteResult.ThreadStarted(thread, message))
        assertThrows(IllegalStateException::class.java) { runSuspend { repository.destinations() } }
        assertSame(NotmidContentSnapshot.Unavailable, repository.snapshot.value)
        fails = false
        assertEquals(listOf(message), repository.destinations().single().threadMessages)
    }

    private val inbox = NotmidDestination("inbox", "Inbox", "Chats", NotmidNavigationIcon.Inbox, emptyList(), emptyList())
    private val thread = NotmidThread("thread", "Chat", "Before", "now", listOf("you"))
    private val message = NotmidThreadMessage("message", "thread", "you", "After", "now", true)
}
