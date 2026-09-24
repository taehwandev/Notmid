package app.thdev.glassnavlab.core.data.impl.notmid

import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteExecutor
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteRequest
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteResult
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthMode
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SingleFlightNotmidProtectedWriteExecutorTest {
    private val auth = NotmidAuthState(NotmidAuthMode.Fake, null, emptyList())
    private val request = NotmidProtectedWriteRequest.SaveClip("clip")

    @Test
    fun concurrentCallerIsRejectedAndCancellationReleasesAdmission() = runTest {
        val pending = CompletableDeferred<NotmidProtectedWriteResult>()
        var calls = 0
        val delegate = object : NotmidProtectedWriteExecutor {
            override suspend fun execute(authState: NotmidAuthState, request: NotmidProtectedWriteRequest): NotmidProtectedWriteResult {
                calls++
                return if (calls == 1) pending.await() else NotmidProtectedWriteResult.Completed
            }
        }
        val executor = SingleFlightNotmidProtectedWriteExecutor(delegate, StandardTestDispatcher(testScheduler))
        val first = async { executor.execute(auth, request) }
        runCurrent()
        assertEquals(NotmidProtectedWriteResult.Busy, executor.execute(auth, request))
        assertEquals(1, calls)
        first.cancelAndJoin()
        assertEquals(NotmidProtectedWriteResult.Completed, executor.execute(auth, request))
        assertEquals(2, calls)
    }

    @Test
    fun failureIsPropagatedAndReleasesAdmission() = runTest {
        val failure = IllegalStateException("failure")
        var calls = 0
        val delegate = object : NotmidProtectedWriteExecutor {
            override suspend fun execute(authState: NotmidAuthState, request: NotmidProtectedWriteRequest): NotmidProtectedWriteResult {
                if (calls++ == 0) throw failure
                return NotmidProtectedWriteResult.Completed
            }
        }
        val executor = SingleFlightNotmidProtectedWriteExecutor(delegate, StandardTestDispatcher(testScheduler))
        try {
            executor.execute(auth, request)
            fail("Expected the original failure")
        } catch (actual: IllegalStateException) {
            assertEquals(failure.message, actual.message)
        }
        assertEquals(NotmidProtectedWriteResult.Completed, executor.execute(auth, request))
        assertEquals(2, calls)
    }
}
