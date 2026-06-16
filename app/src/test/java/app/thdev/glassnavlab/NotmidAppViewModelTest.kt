package app.thdev.glassnavlab

import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthGateway
import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthResult
import app.thdev.glassnavlab.core.data.notmid.NotmidContentSource
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentRepository
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteAction
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteRepository
import app.thdev.glassnavlab.core.notice.api.effect.NoticeEffect
import app.thdev.glassnavlab.core.notice.api.effect.NoticeEffectDelegate
import app.thdev.glassnavlab.core.notice.api.effect.MutableNoticeEffectDelegate
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthProvider
import app.thdev.glassnavlab.core.model.notmid.ChannelNotmidActionDelegate
import app.thdev.glassnavlab.core.model.notmid.NotmidActionDelegate
import app.thdev.glassnavlab.core.model.notmid.NotmidChatInviteDecision
import app.thdev.glassnavlab.core.model.notmid.NotmidChatInviteStatus
import app.thdev.glassnavlab.core.model.notmid.NotmidSendThreadMessageRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidStartThreadRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotmidAppViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initLoadsContentIntoState() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = newViewModel(
            contentRepository = FakeContentRepository(listOf(viewModelTestDestination)),
        )

        advanceUntilIdle()

        assertEquals(
            NotmidContentUiState.Ready(
                source = NotmidContentSource.Static,
                destinations = listOf(viewModelTestDestination),
            ),
            viewModel.state.value.content,
        )
    }

    @Test
    fun protectedWriteUpdatesInlineNoticeAndEmitsNoticeEffect() = runTest(
        mainDispatcherRule.dispatcher,
    ) {
        val viewModel = newViewModel()
        val effects = mutableListOf<NoticeEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.take(1).toList(effects)
        }

        viewModel.onAction(NotmidAppAction.SaveClip("clip-1"))
        advanceUntilIdle()

        assertEquals(
            "Clip saved.",
            viewModel.state.value.messageFor(NotmidProtectedWriteAction.ClipSave),
        )
        val effect = effects.single() as NoticeEffect.ShowNotice
        assertEquals("Clip saved.", effect.notice.message)
    }

    @Test
    fun protectedWriteEmitsThroughInjectedUiEffectDelegate() = runTest(
        mainDispatcherRule.dispatcher,
    ) {
        val uiEffects = MutableNoticeEffectDelegate()
        val viewModel = newViewModel(uiEffects = uiEffects)
        val effects = mutableListOf<NoticeEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            uiEffects.effects.take(1).toList(effects)
        }

        viewModel.onAction(NotmidAppAction.SaveClip("clip-1"))
        advanceUntilIdle()

        val effect = effects.single() as NoticeEffect.ShowNotice
        assertEquals("Clip saved.", effect.notice.message)
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
            contentRepository = FakeContentRepository(listOf(viewModelTestDestination, testInboxDestination)),
        )
        val effects = mutableListOf<NoticeEffect>()
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
        assertEquals("Chat started.", (effects[0] as NoticeEffect.ShowNotice).notice.message)
        assertEquals(
            "https://thdev.app/notmid/inbox/chats/thread-start",
            (effects[1] as NoticeEffect.NavigateDeepLink).deepLink,
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
        contentRepository: NotmidContentRepository = FakeContentRepository(listOf(viewModelTestDestination)),
        protectedWriteRepository: NotmidProtectedWriteRepository = FakeProtectedWriteRepository(),
        authGateway: NotmidAuthGateway = FakeAuthGateway(signedInAuthState),
        actionDelegate: NotmidActionDelegate<NotmidAppAction> = ChannelNotmidActionDelegate(),
        uiEffects: NoticeEffectDelegate = MutableNoticeEffectDelegate(),
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
