package app.thdev.glassnavlab

import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthGateway
import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthResult
import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthSignInRequest
import app.thdev.glassnavlab.core.data.notmid.NotmidContentSource
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentRepository
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteAction
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteRepository
import app.thdev.glassnavlab.core.feedback.api.effect.FeedbackEffect
import app.thdev.glassnavlab.core.feedback.api.effect.FeedbackEffectDelegate
import app.thdev.glassnavlab.core.feedback.api.effect.MutableFeedbackEffectDelegate
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthMode
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthProvider
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthSession
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthUser
import app.thdev.glassnavlab.core.model.notmid.NotmidCaptureModerationStatus
import app.thdev.glassnavlab.core.model.notmid.NotmidCapturePublishReceipt
import app.thdev.glassnavlab.core.model.notmid.NotmidCapturePublishRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidCaptureVisibility
import app.thdev.glassnavlab.core.model.notmid.ChannelNotmidActionDelegate
import app.thdev.glassnavlab.core.model.notmid.NotmidActionDelegate
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class NotmidAppViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initLoadsContentIntoState() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = newViewModel(
            contentRepository = FakeContentRepository(listOf(testDestination)),
        )

        advanceUntilIdle()

        assertEquals(
            NotmidContentUiState.Ready(
                source = NotmidContentSource.Static,
                destinations = listOf(testDestination),
            ),
            viewModel.state.value.content,
        )
    }

    @Test
    fun protectedWriteUpdatesInlineFeedbackAndEmitsUiEffect() = runTest(
        mainDispatcherRule.dispatcher,
    ) {
        val viewModel = newViewModel()
        val effects = mutableListOf<FeedbackEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.take(1).toList(effects)
        }

        viewModel.onAction(NotmidAppAction.SaveClip("clip-1"))
        advanceUntilIdle()

        assertEquals(
            "Clip saved.",
            viewModel.state.value.messageFor(NotmidProtectedWriteAction.ClipSave),
        )
        val effect = effects.single() as FeedbackEffect.ShowFeedback
        assertEquals("Clip saved.", effect.feedback.message)
    }

    @Test
    fun protectedWriteEmitsThroughInjectedUiEffectDelegate() = runTest(
        mainDispatcherRule.dispatcher,
    ) {
        val uiEffects = MutableFeedbackEffectDelegate()
        val viewModel = newViewModel(uiEffects = uiEffects)
        val effects = mutableListOf<FeedbackEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            uiEffects.effects.take(1).toList(effects)
        }

        viewModel.onAction(NotmidAppAction.SaveClip("clip-1"))
        advanceUntilIdle()

        val effect = effects.single() as FeedbackEffect.ShowFeedback
        assertEquals("Clip saved.", effect.feedback.message)
    }

    @Test
    fun actionsAreProcessedThroughInjectedActionDelegate() = runTest(
        mainDispatcherRule.dispatcher,
    ) {
        val actionDelegate = ChannelNotmidActionDelegate<NotmidAppAction>()
        val viewModel = newViewModel(actionDelegate = actionDelegate)

        actionDelegate.dispatch(NotmidAppAction.SaveClip("clip-1"))
        advanceUntilIdle()

        assertEquals(
            "Clip saved.",
            viewModel.state.value.messageFor(NotmidProtectedWriteAction.ClipSave),
        )
    }

    @Test
    fun sendThreadMessageAppendsReceiptToLoadedContent() = runTest(
        mainDispatcherRule.dispatcher,
    ) {
        val viewModel = newViewModel(
            contentRepository = FakeContentRepository(listOf(testInboxDestination)),
        )
        advanceUntilIdle()

        viewModel.onAction(
            NotmidAppAction.SendThreadMessage(
                threadId = "thread-1",
                request = NotmidSendThreadMessageRequest(body = "meet at 8"),
            ),
        )
        advanceUntilIdle()

        val content = viewModel.state.value.content as NotmidContentUiState.Ready
        val thread = content.destinations.single().threads.single()
        val message = content.destinations.single().threadMessages.single()
        assertEquals("message-1", message.id)
        assertEquals("thread-1", message.threadId)
        assertEquals("meet at 8", message.body)
        assertEquals("meet at 8", thread.preview)
        assertEquals("Now", thread.updatedAtLabel)
        assertEquals(
            "Message sent.",
            viewModel.state.value.messageFor(NotmidProtectedWriteAction.ChatMessage),
        )
    }

    @Test
    fun startThreadAddsReceiptThreadAndMessageToLoadedContent() = runTest(
        mainDispatcherRule.dispatcher,
    ) {
        val viewModel = newViewModel(
            contentRepository = FakeContentRepository(listOf(testDestination, testInboxDestination)),
        )
        val effects = mutableListOf<FeedbackEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.take(2).toList(effects)
        }
        advanceUntilIdle()

        viewModel.onAction(
            NotmidAppAction.StartThread(
                NotmidStartThreadRequest(
                    participantHandle = "min.zip",
                    body = "Can we chat about Clip?",
                    attachedClipId = "clip-1",
                ),
            ),
        )
        advanceUntilIdle()

        val content = viewModel.state.value.content as NotmidContentUiState.Ready
        val feedThread = content.destinations.first().threads.single()
        val inboxThread = content.destinations.last().threads.first()
        assertEquals("thread-start", feedThread.id)
        assertEquals(feedThread.id, inboxThread.id)
        assertEquals("message-start", content.destinations.first().threadMessages.single().id)
        assertEquals(
            "Chat started.",
            viewModel.state.value.messageFor(NotmidProtectedWriteAction.ChatStart),
        )
        assertEquals("Chat started.", (effects[0] as FeedbackEffect.ShowFeedback).feedback.message)
        assertEquals(
            "https://thdev.app/notmid/inbox/chats/thread-start",
            (effects[1] as FeedbackEffect.NavigateDeepLink).deepLink,
        )
    }

    @Test
    fun respondThreadInviteUpdatesLoadedContentThread() = runTest(
        mainDispatcherRule.dispatcher,
    ) {
        val viewModel = newViewModel(
            contentRepository = FakeContentRepository(listOf(testInboxDestination)),
        )
        advanceUntilIdle()

        viewModel.onAction(
            NotmidAppAction.RespondThreadInvite(
                threadId = "thread-1",
                decision = NotmidChatInviteDecision.Accept,
            ),
        )
        advanceUntilIdle()

        val content = viewModel.state.value.content as NotmidContentUiState.Ready
        val thread = content.destinations.single().threads.single()
        assertEquals("Chat request accepted. You can message now.", thread.preview)
        assertEquals(NotmidChatInviteStatus.Accepted, thread.chatAccess.inviteStatus)
        assertEquals(
            "Chat request updated.",
            viewModel.state.value.messageFor(NotmidProtectedWriteAction.ChatInviteResponse),
        )
    }

    @Test
    fun uiEffectsDoNotReplayWhenNoCollectorWasStarted() = runTest(
        mainDispatcherRule.dispatcher,
    ) {
        val viewModel = newViewModel()

        viewModel.onAction(NotmidAppAction.SaveClip("clip-1"))
        advanceUntilIdle()

        val replayedEffect = withTimeoutOrNull(100) {
            viewModel.effects.first()
        }

        assertNull(replayedEffect)
    }

    @Test
    fun rejectedAuthUpdatesAuthErrorState() = runTest(mainDispatcherRule.dispatcher) {
        val rejectedState = signedOutAuthState
        val viewModel = newViewModel(
            authGateway = FakeAuthGateway(
                initialState = signedOutAuthState,
                signInResult = NotmidAuthResult.Rejected(
                    code = "auth_disabled",
                    message = "Authentication is disabled for this runtime.",
                    state = rejectedState,
                ),
            ),
        )

        viewModel.onAction(NotmidAppAction.ContinueAuth(NotmidAuthProvider.Fake))
        advanceUntilIdle()

        assertEquals(
            "Authentication is disabled for this runtime.",
            viewModel.state.value.authErrorMessage,
        )
        assertFalse(viewModel.state.value.isAuthenticating)
    }

    private fun newViewModel(
        contentRepository: NotmidContentRepository = FakeContentRepository(listOf(testDestination)),
        protectedWriteRepository: NotmidProtectedWriteRepository = FakeProtectedWriteRepository(),
        authGateway: NotmidAuthGateway = FakeAuthGateway(signedInAuthState),
        actionDelegate: NotmidActionDelegate<NotmidAppAction> = ChannelNotmidActionDelegate(),
        uiEffects: FeedbackEffectDelegate = MutableFeedbackEffectDelegate(),
    ): NotmidAppViewModel {
        return NotmidAppViewModel(
            contentSource = NotmidContentSource.Static,
            contentRepository = contentRepository,
            protectedWriteRepository = protectedWriteRepository,
            authGateway = authGateway,
            actionDelegate = actionDelegate,
            uiEffects = uiEffects,
            ioDispatcher = mainDispatcherRule.dispatcher,
        )
    }
}

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

private class FakeContentRepository(
    private val destinations: List<NotmidDestination>,
) : NotmidContentRepository {
    override suspend fun destinations(): List<NotmidDestination> = destinations
}

private class FakeAuthGateway(
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

private class FakeProtectedWriteRepository : NotmidProtectedWriteRepository {
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

private val testUser = NotmidAuthUser(
    id = "user-1",
    handle = "you.local",
    displayName = "Local You",
    homeNeighborhood = "Seongsu",
    avatarImageUrl = "local-fake-avatar",
    roles = listOf("creator"),
)

private val signedInAuthState = NotmidAuthState(
    mode = NotmidAuthMode.Fake,
    session = NotmidAuthSession(
        accessToken = "test-token",
        provider = NotmidAuthProvider.Fake,
        expiresAt = "2026-05-24T00:00:00.000Z",
        user = testUser,
    ),
    requiredActions = emptyList(),
)

private val signedOutAuthState = NotmidAuthState(
    mode = NotmidAuthMode.Fake,
    session = null,
    requiredActions = emptyList(),
)

private val testClip = NotmidClip(
    id = "clip-1",
    title = "Clip",
    description = "A local clip.",
    badge = "Local",
    palette = emptyList(),
)

private val testDestination = NotmidDestination(
    id = "feed",
    title = "Feed",
    subtitle = "Short video receipts.",
    icon = NotmidNavigationIcon.Feed,
    clips = listOf(testClip),
    places = emptyList(),
)

private val testInboxDestination = NotmidDestination(
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
