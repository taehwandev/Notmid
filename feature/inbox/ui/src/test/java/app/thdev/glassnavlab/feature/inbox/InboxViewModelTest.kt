package app.thdev.glassnavlab.feature.inbox

import androidx.lifecycle.SavedStateHandle
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentRepository
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentSnapshot
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentUpdates
import app.thdev.glassnavlab.core.model.notmid.NotmidDestination
import app.thdev.glassnavlab.core.model.notmid.NotmidNavigationIcon
import app.thdev.glassnavlab.core.model.notmid.NotmidThread
import app.thdev.glassnavlab.core.navigation.runtime.RouteEvent
import app.thdev.glassnavlab.core.navigation.runtime.RouteEventSink
import app.thdev.glassnavlab.feature.inbox.api.event.InboxRouteEvent
import kotlinx.coroutines.CompletableDeferred
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
class InboxViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun restoredFilterAndSharedUpdatesDoNotTriggerReads() = runTest(dispatcher) {
        val source = Source(NotmidContentSnapshot.Ready(listOf(destination)))
        val handle = SavedStateHandle(mapOf("filter" to "Unread"))
        val vm = InboxViewModel(handle, source, source, RouteEventSink {}, dispatcher)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        runCurrent()
        assertEquals(listOf("unread"), (vm.state.value as InboxUiState.Ready).visibleThreads.map { it.id })
        vm.onAction(InboxAction.FilterSelected("All"))
        runCurrent()
        assertEquals(2, (vm.state.value as InboxUiState.Ready).visibleThreads.size)
        assertEquals("All", handle.get<String>("filter"))
        source.snapshot.value = NotmidContentSnapshot.Ready(listOf(destination.copy(threads = listOf(threads[1]))))
        runCurrent()
        assertEquals(listOf("read"), (vm.state.value as InboxUiState.Ready).visibleThreads.map { it.id })
        assertEquals(0, source.reads)
    }

    @Test
    fun clickRoutesThroughViewModelPort() = runTest(dispatcher) {
        val source = Source(NotmidContentSnapshot.Loading)
        val events = mutableListOf<RouteEvent>()
        val vm = InboxViewModel(SavedStateHandle(), source, source, RouteEventSink { events.add(it) }, dispatcher)
        vm.onAction(InboxAction.ThreadClicked("thread-1"))
        assertEquals(listOf(InboxRouteEvent.ChatThreadRequested("thread-1")), events)
        assertEquals(0, source.reads)
    }

    @Test
    fun retryIsSingleFlightAndPublishesRecovery() = runTest(dispatcher) {
        val source = Source(NotmidContentSnapshot.Unavailable)
        source.release = CompletableDeferred()
        val vm = InboxViewModel(SavedStateHandle(), source, source, RouteEventSink {}, dispatcher)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        runCurrent()
        assertEquals(InboxUiState.Unavailable, vm.state.value)
        vm.onAction(InboxAction.Retry)
        vm.onAction(InboxAction.Retry)
        runCurrent()
        assertEquals(1, source.reads)
        source.release?.complete(Unit)
        runCurrent()
        assertEquals(2, (vm.state.value as InboxUiState.Ready).threads.size)
    }

    @Test
    fun unknownFilterDefaultsToAllAndEmptyContentStaysRenderable() = runTest(dispatcher) {
        val source = Source(NotmidContentSnapshot.Ready(emptyList()))
        val vm = InboxViewModel(SavedStateHandle(mapOf("filter" to "unknown")), source, source, RouteEventSink {}, dispatcher)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        runCurrent()
        val state = vm.state.value as InboxUiState.Ready
        assertEquals("All", state.selectedFilter)
        assertEquals(emptyList<InboxThreadUi>(), state.visibleThreads)
    }

    private class Source(initial: NotmidContentSnapshot) : NotmidContentUpdates, NotmidContentRepository {
        override val snapshot = MutableStateFlow(initial)
        var reads = 0
        var release: CompletableDeferred<Unit>? = null
        override suspend fun destinations(): List<NotmidDestination> {
            reads++
            release?.await()
            return listOf(destination).also { snapshot.value = NotmidContentSnapshot.Ready(it) }
        }
    }

    companion object {
        private val threads = listOf(
            NotmidThread("unread", "Unread", "First", "now", listOf("you"), unreadCount = 1),
            NotmidThread("read", "Read", "Second", "now", listOf("you")),
        )
        private val destination = NotmidDestination("inbox", "Inbox", "Chats", NotmidNavigationIcon.Inbox, emptyList(), emptyList(), threads = threads)
    }
}
