package app.thdev.glassnavlab.feature.inbox

import androidx.lifecycle.SavedStateHandle
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentRepository
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentSnapshot
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentUpdates
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteRequest
import app.thdev.glassnavlab.core.model.notmid.ChannelNotmidActionDelegate
import app.thdev.glassnavlab.core.model.notmid.NotmidDestination
import app.thdev.glassnavlab.core.model.notmid.NotmidNavigationIcon
import app.thdev.glassnavlab.core.model.notmid.NotmidSendThreadMessageRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidThread
import app.thdev.glassnavlab.core.navigation.runtime.RouteEventSink
import kotlinx.coroutines.Dispatchers
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
        val writes = ChannelNotmidActionDelegate<NotmidProtectedWriteRequest>()
        val requests = mutableListOf<NotmidProtectedWriteRequest>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { writes.actions.collect { requests.add(it) } }
        val saved = SavedStateHandle(mapOf("threadId" to "thread-1", "draft" to "  Hello  "))
        val vm = ChatThreadViewModel(saved, source, source, writes, RouteEventSink {}, dispatcher)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        runCurrent()
        assertEquals("  Hello  ", (vm.state.value as ChatThreadUiState.Ready).draft)
        vm.onAction(ChatThreadAction.Send)
        vm.onAction(ChatThreadAction.Send)
        runCurrent()
        assertEquals(listOf(NotmidProtectedWriteRequest.SendThreadMessage("thread-1", NotmidSendThreadMessageRequest("Hello"))), requests)
        assertEquals("", saved.get<String>("draft"))
        assertEquals(0, source.reads)
        writes.close()
    }

    @Test
    fun latestPermissionsBlockSendEvenBeforeUiStateRecollects() = runTest(dispatcher) {
        val source = Source()
        val writes = ChannelNotmidActionDelegate<NotmidProtectedWriteRequest>()
        val requests = mutableListOf<NotmidProtectedWriteRequest>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { writes.actions.collect { requests.add(it) } }
        val saved = SavedStateHandle(mapOf("threadId" to "thread-1", "draft" to "Keep me"))
        val vm = ChatThreadViewModel(saved, source, source, writes, RouteEventSink {}, dispatcher)
        source.snapshot.value = NotmidContentSnapshot.Ready(listOf(destination.copy(threads = listOf(thread.copy(chatAccess = thread.chatAccess.copy(canSendMessage = false))))))
        vm.onAction(ChatThreadAction.Send)
        vm.onAction(ChatThreadAction.AcceptInvite)
        vm.onAction(ChatThreadAction.RejectInvite)
        runCurrent()
        assertEquals(emptyList<NotmidProtectedWriteRequest>(), requests)
        assertEquals("Keep me", saved.get<String>("draft"))
        writes.close()
    }

    @Test
    fun blankDraftAndUnavailableContentCannotWrite() = runTest(dispatcher) {
        val source = Source()
        val writes = ChannelNotmidActionDelegate<NotmidProtectedWriteRequest>()
        val requests = mutableListOf<NotmidProtectedWriteRequest>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { writes.actions.collect { requests.add(it) } }
        val saved = SavedStateHandle(mapOf("threadId" to "thread-1"))
        val vm = ChatThreadViewModel(saved, source, source, writes, RouteEventSink {}, dispatcher)
        vm.onAction(ChatThreadAction.DraftChanged("  "))
        vm.onAction(ChatThreadAction.Send)
        source.snapshot.value = NotmidContentSnapshot.Unavailable
        vm.onAction(ChatThreadAction.DraftChanged("Preserve"))
        vm.onAction(ChatThreadAction.Send)
        runCurrent()
        assertEquals(emptyList<NotmidProtectedWriteRequest>(), requests)
        assertEquals("Preserve", saved.get<String>("draft"))
        writes.close()
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
