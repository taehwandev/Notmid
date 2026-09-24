package app.thdev.glassnavlab.feature.inbox

import androidx.lifecycle.SavedStateHandle
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentRepository
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentSnapshot
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentUpdates
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteRequest
import app.thdev.glassnavlab.core.auth.notmid.*
import app.thdev.glassnavlab.core.model.notmid.*
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteExecutor
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteResult
import app.thdev.glassnavlab.core.notice.api.effect.MutableNoticeEffectDelegate
import app.thdev.glassnavlab.core.model.notmid.NotmidDestination
import app.thdev.glassnavlab.core.model.notmid.NotmidNavigationIcon
import app.thdev.glassnavlab.core.model.notmid.NotmidSendThreadMessageRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidThread
import app.thdev.glassnavlab.core.navigation.runtime.RouteEventSink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import app.thdev.glassnavlab.core.notice.api.effect.NoticeEffect
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteAction
import org.junit.Assert.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatThreadViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun restoredDraftSendsToRouteThreadAndClearsOnlyOnce() = runTest(dispatcher) {
        val source = Source()
        val writes = Executor()
        val requests = writes.requests
        val saved = SavedStateHandle(mapOf("threadId" to "thread-1", "draft" to "  Hello  "))
        val vm = ChatThreadViewModel(saved, source, source, writes, RouteEventSink {}, dispatcher, Auth(), MutableNoticeEffectDelegate())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        runCurrent()
        assertEquals("  Hello  ", (vm.state.value as ChatThreadUiState.Ready).draft)
        vm.onAction(ChatThreadAction.Send)
        vm.onAction(ChatThreadAction.Send)
        runCurrent()
        assertEquals(listOf(NotmidProtectedWriteRequest.SendThreadMessage("thread-1", NotmidSendThreadMessageRequest("Hello"))), requests)
        assertEquals("", saved.get<String>("draft"))
        assertEquals(0, source.reads)
    }

    @Test
    fun latestPermissionsBlockSendEvenBeforeUiStateRecollects() = runTest(dispatcher) {
        val source = Source()
        val writes = Executor()
        val requests = writes.requests
        val saved = SavedStateHandle(mapOf("threadId" to "thread-1", "draft" to "Keep me"))
        val vm = ChatThreadViewModel(saved, source, source, writes, RouteEventSink {}, dispatcher, Auth(), MutableNoticeEffectDelegate())
        source.snapshot.value = NotmidContentSnapshot.Ready(listOf(destination.copy(threads = listOf(thread.copy(chatAccess = thread.chatAccess.copy(canSendMessage = false))))))
        vm.onAction(ChatThreadAction.Send)
        vm.onAction(ChatThreadAction.AcceptInvite)
        vm.onAction(ChatThreadAction.RejectInvite)
        runCurrent()
        assertEquals(emptyList<NotmidProtectedWriteRequest>(), requests)
        assertEquals("Keep me", saved.get<String>("draft"))
    }

    @Test
    fun blankDraftAndUnavailableContentCannotWrite() = runTest(dispatcher) {
        val source = Source()
        val writes = Executor()
        val requests = writes.requests
        val saved = SavedStateHandle(mapOf("threadId" to "thread-1"))
        val vm = ChatThreadViewModel(saved, source, source, writes, RouteEventSink {}, dispatcher, Auth(), MutableNoticeEffectDelegate())
        vm.onAction(ChatThreadAction.DraftChanged("  "))
        vm.onAction(ChatThreadAction.Send)
        source.snapshot.value = NotmidContentSnapshot.Unavailable
        vm.onAction(ChatThreadAction.DraftChanged("Preserve"))
        vm.onAction(ChatThreadAction.Send)
        runCurrent()
        assertEquals(emptyList<NotmidProtectedWriteRequest>(), requests)
        assertEquals("Preserve", saved.get<String>("draft"))
    }

    @Test
    fun sendReceiptDoesNotClearNewDraftAndEmitsSuccess() = runTest(dispatcher) {
        val pending = CompletableDeferred<NotmidProtectedWriteResult>()
        val writes = Executor { pending.await() }
        val notices = MutableNoticeEffectDelegate()
        val effects = mutableListOf<NoticeEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { notices.effects.collect { effects.add(it) } }
        val source = Source()
        val saved = SavedStateHandle(mapOf("threadId" to "thread-1", "draft" to "First"))
        val vm = ChatThreadViewModel(saved, source, source, writes, RouteEventSink {}, dispatcher, Auth(), notices)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        vm.onAction(ChatThreadAction.Send)
        vm.onAction(ChatThreadAction.Send)
        runCurrent()
        assertEquals(1, writes.requests.size)
        assertEquals(NotmidProtectedWriteAction.ChatMessage, (vm.state.value as ChatThreadUiState.Ready).write.inFlight)
        vm.onAction(ChatThreadAction.DraftChanged("Next message"))
        pending.complete(NotmidProtectedWriteResult.MessageSent(NotmidThreadMessage("message", "thread-1", "user", "First", "now", true)))
        runCurrent()
        assertEquals("Next message", saved.get<String>("draft"))
        assertNull((vm.state.value as ChatThreadUiState.Ready).write.inFlight)
        assertEquals("Message sent.", (effects.single() as NoticeEffect.ShowNotice).notice.message)
    }

    @Test
    fun busyCancelFailureAndLogoutKeepDraftWithoutFalseSuccess() = runTest(dispatcher) {
        var attempt = 0
        val auth = Auth()
        val writes = Executor {
            when (attempt++) {
                0 -> NotmidProtectedWriteResult.Busy
                1 -> throw CancellationException("cancel")
                2 -> error("private transport detail")
                else -> {
                    auth.signOut()
                    NotmidProtectedWriteResult.MessageSent(NotmidThreadMessage("message", "thread-1", "user", "Keep", "now", true))
                }
            }
        }
        val notices = MutableNoticeEffectDelegate()
        val effects = mutableListOf<NoticeEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { notices.effects.collect { effects.add(it) } }
        val source = Source()
        val saved = SavedStateHandle(mapOf("threadId" to "thread-1", "draft" to "Keep"))
        val vm = ChatThreadViewModel(saved, source, source, writes, RouteEventSink {}, dispatcher, auth, notices)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        repeat(4) { index ->
            vm.onAction(ChatThreadAction.Send)
            runCurrent()
            assertEquals("Keep", saved.get<String>("draft"))
            assertNull((vm.state.value as ChatThreadUiState.Ready).write.inFlight)
            assertEquals(if (index < 2) 0 else 1, effects.size)
        }
        assertEquals("This action failed. Try again.", (effects.single() as NoticeEffect.ShowNotice).notice.message)
    }

    @Test
    fun saveAndInviteActionsOwnRequestsAndNotices() = runTest(dispatcher) {
        val source = Source()
        source.snapshot.value = NotmidContentSnapshot.Ready(listOf(destination.copy(
            clips = listOf(NotmidClip(id = "clip", title = "Clip", description = "Receipt", badge = "live", palette = emptyList())),
            threads = listOf(thread.copy(attachedClipId = "clip", chatAccess = thread.chatAccess.copy(canAcceptInvite = true, canRejectInvite = true))),
        )))
        val writes = Executor()
        val notices = MutableNoticeEffectDelegate()
        val effects = mutableListOf<NoticeEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { notices.effects.collect { effects.add(it) } }
        val vm = ChatThreadViewModel(SavedStateHandle(mapOf("threadId" to "thread-1")), source, source, writes, RouteEventSink {}, dispatcher, Auth(), notices)
        for (action in listOf(ChatThreadAction.SaveClip, ChatThreadAction.AcceptInvite, ChatThreadAction.RejectInvite)) {
            vm.onAction(action)
            runCurrent()
        }
        assertEquals(listOf(
            NotmidProtectedWriteRequest.SaveClip("clip"),
            NotmidProtectedWriteRequest.RespondThreadInvite("thread-1", NotmidChatInviteDecision.Accept),
            NotmidProtectedWriteRequest.RespondThreadInvite("thread-1", NotmidChatInviteDecision.Reject),
        ), writes.requests)
        assertEquals(listOf("Clip saved.", "Chat request updated.", "Chat request updated."), effects.map { (it as NoticeEffect.ShowNotice).notice.message })
    }

    private class Executor(
        private val response: suspend (NotmidProtectedWriteRequest) -> NotmidProtectedWriteResult = { request ->
            when (request) {
                is NotmidProtectedWriteRequest.SendThreadMessage -> NotmidProtectedWriteResult.MessageSent(
                    NotmidThreadMessage("message", request.threadId, "user", request.request.body, "now", true))
                is NotmidProtectedWriteRequest.RespondThreadInvite -> NotmidProtectedWriteResult.ThreadUpdated(thread)
                else -> NotmidProtectedWriteResult.Completed
            }
        },
    ) : NotmidProtectedWriteExecutor {
        val requests = mutableListOf<NotmidProtectedWriteRequest>()
        override suspend fun execute(authState: NotmidAuthState, request: NotmidProtectedWriteRequest): NotmidProtectedWriteResult {
            requests.add(request)
            return response(request)
        }
    }

    private class Auth : NotmidAuthGateway {
        override val states = MutableStateFlow(NotmidAuthState(NotmidAuthMode.Fake,
            NotmidAuthSession("token", NotmidAuthProvider.Fake, "", NotmidAuthUser("user", "handle", "Name", "Seoul", "", emptyList())), emptyList()))
        override fun currentState() = states.value
        override fun signOut() = states.value.copy(session = null).also { states.value = it }
        override suspend fun signIn(request: NotmidAuthSignInRequest): NotmidAuthResult = error("No sign-in expected")
        override fun applyProfileUpdate(expectedSession: NotmidAuthSession, user: NotmidAuthUser): NotmidAuthState = error("No profile write expected")
    }

    private class Source : NotmidContentUpdates, NotmidContentRepository {
        override val snapshot = MutableStateFlow<NotmidContentSnapshot>(NotmidContentSnapshot.Ready(listOf(destination)))
        var reads = 0
        override suspend fun destinations(): List<NotmidDestination> { reads++; return listOf(destination) }
    }

    companion object {
        private val thread = NotmidThread("thread-1", "Chat", "Preview", "now", listOf("you"))
        private val destination = NotmidDestination("inbox", "Inbox", "Chats", NotmidNavigationIcon.Inbox, emptyList(), emptyList(), threads = listOf(thread))
    }
}
