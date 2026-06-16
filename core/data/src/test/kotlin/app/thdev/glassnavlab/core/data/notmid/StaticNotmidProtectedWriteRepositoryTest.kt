package app.thdev.glassnavlab.core.data.notmid

import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteFailure
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteException
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthMode
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthProvider
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthSession
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthUser
import app.thdev.glassnavlab.core.model.notmid.NotmidChatInviteDecision
import app.thdev.glassnavlab.core.model.notmid.NotmidChatInviteStatus
import app.thdev.glassnavlab.core.model.notmid.NotmidChatRelationship
import app.thdev.glassnavlab.core.model.notmid.NotmidProfileSettingsUpdateRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidSendThreadMessageRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidStartThreadRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class StaticNotmidProtectedWriteRepositoryTest {
    @Test
    fun missingAuthBlocksStaticWritesBeforeFakeMutation() {
        val repository = StaticNotmidProtectedWriteRepository()

        val exception = assertThrows(NotmidProtectedWriteException::class.java) {
            runSuspend {
                repository.sendThreadMessage(
                    authState = signedInState.copy(session = null),
                    threadId = "thread-1",
                    request = NotmidSendThreadMessageRequest(body = "hello"),
                )
            }
        }

        assertTrue(exception.failure is NotmidProtectedWriteFailure.MissingAuth)
    }

    @Test
    fun chatMessageUsesSignedInUserAndStableId() {
        val repository = StaticNotmidProtectedWriteRepository()

        val receipt = runSuspend {
            repository.sendThreadMessage(
                authState = signedInState,
                threadId = "thread-1",
                request = NotmidSendThreadMessageRequest(body = "hello"),
            )
        }

        assertEquals("thread-1", receipt.message.threadId)
        assertEquals("you.local", receipt.message.senderHandle)
        assertTrue(receipt.message.id.startsWith("msg-thread-1-"))
    }

    @Test
    fun chatMessageForPendingThreadRequiresInviteResponse() {
        val repository = StaticNotmidProtectedWriteRepository()

        val exception = assertThrows(NotmidProtectedWriteException::class.java) {
            runSuspend {
                repository.sendThreadMessage(
                    authState = signedInState,
                    threadId = "rain-route",
                    request = NotmidSendThreadMessageRequest(body = "hello"),
                )
            }
        }

        val failure = exception.failure as NotmidProtectedWriteFailure.InvalidRequest
        assertEquals("chat_invite_required", failure.code)
    }

    @Test
    fun startThreadForFriendReturnsAcceptedThreadAndStartMessage() {
        val repository = StaticNotmidProtectedWriteRepository()

        val receipt = runSuspend {
            repository.startThread(
                authState = signedInState,
                request = NotmidStartThreadRequest(
                    participantHandle = "min.zip",
                    body = "Can we chat about this clip?",
                    attachedClipId = "clip-thread",
                    attachedPlaceId = "no-dead-end-share",
                ),
            )
        }

        assertEquals("thread-min-zip-you-local-clip-thread", receipt.thread.id)
        assertEquals(NotmidChatRelationship.Friend, receipt.thread.chatAccess.relationship)
        assertEquals(NotmidChatInviteStatus.Accepted, receipt.thread.chatAccess.inviteStatus)
        assertTrue(receipt.thread.chatAccess.canSendMessage)
        assertEquals(receipt.thread.id, receipt.message?.threadId)
    }

    @Test
    fun startThreadCanUseFeedClipContext() {
        val repository = StaticNotmidProtectedWriteRepository()

        val receipt = runSuspend {
            repository.startThread(
                authState = signedInState,
                request = NotmidStartThreadRequest(
                    participantHandle = "min.zip",
                    body = "Can we chat about the queue?",
                    attachedClipId = "cafe-queue-check",
                    attachedPlaceId = "millo-roasters",
                ),
            )
        }

        assertEquals("cafe-queue-check", receipt.thread.attachedClipId)
        assertEquals("millo-roasters", receipt.thread.attachedPlaceId)
        assertEquals(NotmidChatInviteStatus.Accepted, receipt.thread.chatAccess.inviteStatus)
        assertTrue(receipt.thread.chatAccess.canSendMessage)
    }

    @Test
    fun startThreadForNonFriendRequiresOutboundInviteAcceptance() {
        val repository = StaticNotmidProtectedWriteRepository()

        val receipt = runSuspend {
            repository.startThread(
                authState = signedInState,
                request = NotmidStartThreadRequest(
                    participantHandle = "receipt.han",
                    body = "Can we chat about the reply?",
                    attachedClipId = "creator-reply",
                    attachedPlaceId = "moderation-queue",
                ),
            )
        }

        assertEquals(NotmidChatRelationship.NonFriend, receipt.thread.chatAccess.relationship)
        assertEquals(NotmidChatInviteStatus.PendingOutbound, receipt.thread.chatAccess.inviteStatus)
        assertTrue(!receipt.thread.chatAccess.canSendMessage)
    }

    @Test
    fun acceptThreadInviteReturnsAcceptedThread() {
        val repository = StaticNotmidProtectedWriteRepository()

        val receipt = runSuspend {
            repository.respondThreadInvite(
                authState = signedInState,
                threadId = "rain-route",
                decision = NotmidChatInviteDecision.Accept,
            )
        }

        assertEquals("rain-route", receipt.thread.id)
        assertEquals(NotmidChatInviteStatus.Accepted, receipt.thread.chatAccess.inviteStatus)
        assertTrue(receipt.thread.chatAccess.canSendMessage)
    }

    @Test
    fun profileSettingsUpdateReturnsUpdatedUser() {
        val repository = StaticNotmidProtectedWriteRepository()

        val receipt = runSuspend {
            repository.updateProfileSettings(
                authState = signedInState,
                request = NotmidProfileSettingsUpdateRequest(
                    displayName = "Local You Updated",
                    homeNeighborhood = "Seongsu",
                ),
            )
        }

        assertTrue(receipt.updated)
        assertEquals("Local You Updated", receipt.settings.user.displayName)
        assertEquals("Seongsu", receipt.settings.user.homeNeighborhood)
    }
}

private val signedInState = NotmidAuthState(
    mode = NotmidAuthMode.Fake,
    session = NotmidAuthSession(
        accessToken = "notmid-fake-local-dev-token",
        provider = NotmidAuthProvider.Fake,
        expiresAt = "2026-05-30T01:00:00.000Z",
        user = NotmidAuthUser(
            id = "local-you",
            handle = "you.local",
            displayName = "Local You",
            homeNeighborhood = "Hapjeong",
            avatarImageUrl = "",
            roles = listOf("creator"),
        ),
    ),
    requiredActions = emptyList(),
)
