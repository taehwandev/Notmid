package app.thdev.glassnavlab.core.data.impl.notmid

import app.thdev.glassnavlab.core.data.assertions.notmid.StaticNotmidContentRepository
import app.thdev.glassnavlab.core.data.assertions.notmid.StaticNotmidProtectedWriteRepository
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteException
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteFailure
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteRepository
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteRequest
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteResult
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthMode
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthProvider
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthSession
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthUser
import app.thdev.glassnavlab.core.model.notmid.NotmidCapturePublishRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidCaptureVisibility
import app.thdev.glassnavlab.core.model.notmid.NotmidChatInviteDecision
import app.thdev.glassnavlab.core.model.notmid.NotmidClipSaveReceipt
import app.thdev.glassnavlab.core.model.notmid.NotmidProfileSettingsUpdateRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidSendThreadMessageRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidStartThreadRequest
import java.util.concurrent.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryNotmidProtectedWriteExecutorTest {
    private val content = ObservableNotmidContentRepository(StaticNotmidContentRepository())
    private val executor = RepositoryNotmidProtectedWriteExecutor(StaticNotmidProtectedWriteRepository(), content)

    @Test
    fun everyCommandRetainsItsAuthenticationRequirement() {
        for (request in requests) {
            val exception = assertThrows(NotmidProtectedWriteException::class.java) {
                runSuspend { executor.execute(authState.copy(session = null), request) }
            }
            assertTrue(exception.failure is NotmidProtectedWriteFailure.MissingAuth)
            assertEquals(request.writeAction, exception.failure.action)
        }
    }

    @Test
    fun publishAndSaveCompleteWithoutIntroducingContentMutations() {
        requests.take(2).forEach { request ->
            assertSame(NotmidProtectedWriteResult.Completed, runSuspend { executor.execute(authState, request) })
        }
    }

    @Test
    fun messageResultRetainsThreadAndSignedInSender() {
        val result = runSuspend { executor.execute(authState, requests[2]) }
            as NotmidProtectedWriteResult.MessageSent
        assertEquals("thread-1", result.message.threadId)
        assertEquals("you.local", result.message.senderHandle)
        assertEquals("hello", result.message.body)
    }

    @Test
    fun startResultRetainsThreadAndOptionalInitialMessage() {
        runSuspend { content.destinations() }
        val result = runSuspend { executor.execute(authState, requests[3]) }
            as NotmidProtectedWriteResult.ThreadStarted
        assertTrue(result.thread.participantHandles.contains("min.zip"))
        assertEquals(result.thread.id, result.message?.threadId)
        assertEquals("hello", result.message?.body)
        val snapshot = content.snapshot.value as app.thdev.glassnavlab.core.domain.notmid.NotmidContentSnapshot.Ready
        val inbox = snapshot.destinations.first { it.icon == app.thdev.glassnavlab.core.model.notmid.NotmidNavigationIcon.Inbox }
        assertTrue(inbox.threads.any { it.id == result.thread.id })
        assertTrue(inbox.threadMessages.any { it.id == result.message?.id })
    }

    @Test
    fun invitationResultRetainsUpdatedPermissions() {
        val result = runSuspend { executor.execute(authState, requests[4]) }
            as NotmidProtectedWriteResult.ThreadUpdated
        assertEquals("rain-route", result.thread.id)
        assertTrue(result.thread.chatAccess.canSendMessage)
    }

    @Test
    fun profileResultRetainsUserIdentityAndEditedFields() {
        val result = runSuspend { executor.execute(authState, requests[5]) }
            as NotmidProtectedWriteResult.ProfileUpdated
        assertEquals("local-you", result.user.id)
        assertEquals("Updated", result.user.displayName)
        assertEquals("Seongsu", result.user.homeNeighborhood)
    }

    @Test
    fun cancellationAndFailuresEscapeWithoutWrappingOrRetry() {
        listOf(CancellationException("cancelled"), IllegalStateException("failed")).forEach { failure ->
            var calls = 0
            val repository = object : NotmidProtectedWriteRepository by StaticNotmidProtectedWriteRepository() {
                override suspend fun saveClip(authState: NotmidAuthState, clipId: String): NotmidClipSaveReceipt {
                    calls++
                    assertSame(this@RepositoryNotmidProtectedWriteExecutorTest.authState, authState)
                    assertEquals("cafe-queue-check", clipId)
                    throw failure
                }
            }
            val actual = assertThrows(failure.javaClass) {
                runSuspend { RepositoryNotmidProtectedWriteExecutor(repository, content).execute(authState, requests[1]) }
            }
            assertSame(failure, actual)
            assertEquals(1, calls)
        }
    }

    private val authState = NotmidAuthState(
        mode = NotmidAuthMode.Fake,
        session = NotmidAuthSession(
            accessToken = "test-token",
            provider = NotmidAuthProvider.Fake,
            expiresAt = "2026-05-30T01:00:00.000Z",
            user = NotmidAuthUser("local-you", "you.local", "Local You", "Hapjeong", "", emptyList()),
        ),
        requiredActions = emptyList(),
    )

    private val requests = listOf(
        NotmidProtectedWriteRequest.PublishCapture(
            NotmidCapturePublishRequest("draft-1", "A receipt", "millo-roasters", listOf("calm"), NotmidCaptureVisibility.Friends),
        ),
        NotmidProtectedWriteRequest.SaveClip("cafe-queue-check"),
        NotmidProtectedWriteRequest.SendThreadMessage("thread-1", NotmidSendThreadMessageRequest("hello")),
        NotmidProtectedWriteRequest.StartThread(NotmidStartThreadRequest(participantHandle = "min.zip", body = "hello")),
        NotmidProtectedWriteRequest.RespondThreadInvite("rain-route", NotmidChatInviteDecision.Accept),
        NotmidProtectedWriteRequest.UpdateProfileSettings(NotmidProfileSettingsUpdateRequest("Updated", "Seongsu")),
    )
}
