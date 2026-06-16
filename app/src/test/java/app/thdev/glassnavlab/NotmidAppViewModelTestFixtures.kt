package app.thdev.glassnavlab

import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthGateway
import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthResult
import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthSignInRequest
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentRepository
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteRepository
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthMode
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthProvider
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthSession
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthUser
import app.thdev.glassnavlab.core.model.notmid.NotmidCaptureModerationStatus
import app.thdev.glassnavlab.core.model.notmid.NotmidCapturePublishReceipt
import app.thdev.glassnavlab.core.model.notmid.NotmidCapturePublishRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidCaptureVisibility
import app.thdev.glassnavlab.core.model.notmid.NotmidChatAccess
import app.thdev.glassnavlab.core.model.notmid.NotmidChatInviteDecision
import app.thdev.glassnavlab.core.model.notmid.NotmidChatInviteResponseReceipt
import app.thdev.glassnavlab.core.model.notmid.NotmidChatInviteStatus
import app.thdev.glassnavlab.core.model.notmid.NotmidChatRelationship
import app.thdev.glassnavlab.core.model.notmid.NotmidClip
import app.thdev.glassnavlab.core.model.notmid.NotmidClipSaveReceipt
import app.thdev.glassnavlab.core.model.notmid.NotmidDestination
import app.thdev.glassnavlab.core.model.notmid.NotmidNavigationIcon
import app.thdev.glassnavlab.core.model.notmid.NotmidProfilePrivacySettings
import app.thdev.glassnavlab.core.model.notmid.NotmidProfileSettings
import app.thdev.glassnavlab.core.model.notmid.NotmidProfileSettingsUpdateReceipt
import app.thdev.glassnavlab.core.model.notmid.NotmidProfileSettingsUpdateRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidSendThreadMessageReceipt
import app.thdev.glassnavlab.core.model.notmid.NotmidSendThreadMessageRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidStartThreadReceipt
import app.thdev.glassnavlab.core.model.notmid.NotmidStartThreadRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidThread
import app.thdev.glassnavlab.core.model.notmid.NotmidThreadMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        kotlinx.coroutines.Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        kotlinx.coroutines.Dispatchers.resetMain()
    }
}

internal class FakeContentRepository(
    private val destinations: List<NotmidDestination>,
) : NotmidContentRepository {
    override suspend fun destinations(): List<NotmidDestination> = destinations
}

internal class FakeAuthGateway(
    initialState: NotmidAuthState,
    private val signInResult: NotmidAuthResult = NotmidAuthResult.Success(
        state = signedInAuthState,
        nextPath = "/notmid",
    ),
) : NotmidAuthGateway {
    private var state = initialState

    override fun currentState(): NotmidAuthState = state

    override suspend fun signIn(request: NotmidAuthSignInRequest): NotmidAuthResult {
        return signInResult.also { result ->
            when (result) {
                is NotmidAuthResult.Success -> state = result.state
                is NotmidAuthResult.Rejected -> state = result.state
            }
        }
    }

    override fun signOut(): NotmidAuthState {
        state = signedOutAuthState
        return state
    }
}

internal class FakeProtectedWriteRepository : NotmidProtectedWriteRepository {
    override suspend fun publishCapture(
        authState: NotmidAuthState,
        request: NotmidCapturePublishRequest,
    ): NotmidCapturePublishReceipt {
        return NotmidCapturePublishReceipt(
            clip = testClip,
            moderationStatus = NotmidCaptureModerationStatus.Queued,
        )
    }

    override suspend fun saveClip(
        authState: NotmidAuthState,
        clipId: String,
    ): NotmidClipSaveReceipt {
        return NotmidClipSaveReceipt(
            clip = testClip,
            saved = true,
        )
    }

    override suspend fun sendThreadMessage(
        authState: NotmidAuthState,
        threadId: String,
        request: NotmidSendThreadMessageRequest,
    ): NotmidSendThreadMessageReceipt {
        return NotmidSendThreadMessageReceipt(
            message = NotmidThreadMessage(
                id = "message-1",
                threadId = threadId,
                senderHandle = "you.local",
                body = request.body,
                createdAtLabel = "Now",
                mine = true,
            ),
        )
    }

    override suspend fun startThread(
        authState: NotmidAuthState,
        request: NotmidStartThreadRequest,
    ): NotmidStartThreadReceipt {
        return NotmidStartThreadReceipt(
            thread = NotmidThread(
                id = "thread-start",
                title = "chat with ${request.participantHandle}",
                preview = request.body,
                updatedAtLabel = "now",
                participantHandles = listOf("you.local", request.participantHandle),
                attachedClipId = request.attachedClipId,
            ),
            message = NotmidThreadMessage(
                id = "message-start",
                threadId = "thread-start",
                senderHandle = "you.local",
                body = request.body,
                createdAtLabel = "Now",
                mine = true,
            ),
        )
    }

    override suspend fun respondThreadInvite(
        authState: NotmidAuthState,
        threadId: String,
        decision: NotmidChatInviteDecision,
    ): NotmidChatInviteResponseReceipt {
        return NotmidChatInviteResponseReceipt(
            thread = testInboxDestination.threads.single().copy(
                id = threadId,
                preview = when (decision) {
                    NotmidChatInviteDecision.Accept -> "Chat request accepted. You can message now."
                    NotmidChatInviteDecision.Reject -> "Chat request rejected."
                },
                chatAccess = NotmidChatAccess(
                    relationship = NotmidChatRelationship.NonFriend,
                    inviteStatus = when (decision) {
                        NotmidChatInviteDecision.Accept -> NotmidChatInviteStatus.Accepted
                        NotmidChatInviteDecision.Reject -> NotmidChatInviteStatus.Rejected
                    },
                    canSendMessage = decision == NotmidChatInviteDecision.Accept,
                    canAcceptInvite = false,
                    canRejectInvite = false,
                    reasonLabel = when (decision) {
                        NotmidChatInviteDecision.Accept -> "Chat request accepted."
                        NotmidChatInviteDecision.Reject -> "This chat request was rejected."
                    },
                ),
            ),
        )
    }

    override suspend fun updateProfileSettings(
        authState: NotmidAuthState,
        request: NotmidProfileSettingsUpdateRequest,
    ): NotmidProfileSettingsUpdateReceipt {
        return NotmidProfileSettingsUpdateReceipt(
            settings = NotmidProfileSettings(
                user = testUser.copy(
                    displayName = request.displayName,
                    homeNeighborhood = request.homeNeighborhood,
                ),
                privacy = NotmidProfilePrivacySettings(
                    savedPlacesVisibility = "friends",
                    chatInvites = "friends",
                    defaultReceiptVisibility = NotmidCaptureVisibility.Friends,
                ),
            ),
            updated = true,
        )
    }
}

internal val testUser = NotmidAuthUser(
    id = "user-1",
    handle = "you.local",
    displayName = "Local You",
    homeNeighborhood = "Seongsu",
    avatarImageUrl = "local-fake-avatar",
    roles = listOf("creator"),
)

internal val signedInAuthState = NotmidAuthState(
    mode = NotmidAuthMode.Fake,
    session = NotmidAuthSession(
        accessToken = "test-token",
        provider = NotmidAuthProvider.Fake,
        expiresAt = "2026-05-24T00:00:00.000Z",
        user = testUser,
    ),
    requiredActions = emptyList(),
)

internal val signedOutAuthState = NotmidAuthState(
    mode = NotmidAuthMode.Fake,
    session = null,
    requiredActions = emptyList(),
)

internal val testClip = NotmidClip(
    id = "clip-1",
    title = "Clip",
    description = "A local clip.",
    badge = "Local",
    palette = emptyList(),
)

internal val viewModelTestDestination = NotmidDestination(
    id = "feed",
    title = "Feed",
    subtitle = "Short video receipts.",
    icon = NotmidNavigationIcon.Feed,
    clips = listOf(testClip),
    places = emptyList(),
)

internal val testInboxDestination = NotmidDestination(
    id = "inbox",
    title = "Inbox",
    subtitle = "Receipt chats.",
    icon = NotmidNavigationIcon.Inbox,
    clips = emptyList(),
    places = emptyList(),
    threads = listOf(
        NotmidThread(
            id = "thread-1",
            title = "Thread",
            preview = "Preview",
            updatedAtLabel = "now",
            participantHandles = listOf("you.local"),
        ),
    ),
)
