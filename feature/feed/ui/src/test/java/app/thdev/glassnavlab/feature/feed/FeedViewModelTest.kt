package app.thdev.glassnavlab.feature.feed

import androidx.lifecycle.ViewModelStore
import app.thdev.glassnavlab.core.navigation.runtime.RouteEvent
import app.thdev.glassnavlab.core.navigation.runtime.RouteEventSink
import app.thdev.glassnavlab.feature.feed.api.event.FeedRouteEvent
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentRepository
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentSnapshot
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentUpdates
import app.thdev.glassnavlab.core.model.notmid.NotmidClip
import app.thdev.glassnavlab.core.model.notmid.NotmidDestination
import app.thdev.glassnavlab.core.model.notmid.NotmidNavigationIcon
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val updates = object : NotmidContentUpdates {
        override val snapshot = MutableStateFlow<NotmidContentSnapshot>(NotmidContentSnapshot.Loading)
    }
    private var reads = 0
    private val routeEvents = mutableListOf<RouteEvent>()
    private val routeSink = RouteEventSink { routeEvents.add(it) }
    private val repository = object : NotmidContentRepository {
        override suspend fun destinations(): List<NotmidDestination> {
            reads++
            return listOf(feed).also { updates.snapshot.value = NotmidContentSnapshot.Ready(it) }
        }
    }

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun subscribesToSharedResultWithoutAnotherReadAndSelectsFeedById() = runTest(dispatcher) {
        val vm = model()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        updates.snapshot.value = NotmidContentSnapshot.Ready(listOf(feed.copy(id = "map", title = "Map"), feed))
        advanceUntilIdle()
        val state = vm.state.value as FeedLoadState.Ready
        assertEquals("Feed", state.content.title)
        assertEquals("clip-1", state.content.heroClip?.id)
        assertEquals(0, reads)
    }

    @Test
    fun preservesMissingFeedFallbackAndRepresentsEmptyData() = runTest(dispatcher) {
        val vm = model()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        updates.snapshot.value = NotmidContentSnapshot.Ready(listOf(feed.copy(id = "other", title = "Fallback")))
        advanceUntilIdle()
        assertEquals("Fallback", (vm.state.value as FeedLoadState.Ready).content.title)
        updates.snapshot.value = NotmidContentSnapshot.Ready(emptyList())
        advanceUntilIdle()
        assertEquals(true, (vm.state.value as FeedLoadState.Ready).content.isEmpty)
    }

    @Test
    fun unavailableCanRetryAndReplaceTheSnapshot() = runTest(dispatcher) {
        val vm = model()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        updates.snapshot.value = NotmidContentSnapshot.Unavailable
        advanceUntilIdle()
        assertSame(FeedLoadState.Unavailable, vm.state.value)
        vm.onAction(FeedAction.Retry)
        advanceUntilIdle()
        assertEquals(1, reads)
        assertEquals("clip-1", (vm.state.value as FeedLoadState.Ready).content.heroClip?.id)
    }

    @Test
    fun repeatedRetryWhilePendingDoesNotStartAnotherRequest() = runTest(dispatcher) {
        val release = CompletableDeferred<Unit>()
        val pendingRepository = object : NotmidContentRepository {
            override suspend fun destinations(): List<NotmidDestination> {
                reads++
                release.await()
                return emptyList()
            }
        }
        val vm = FeedViewModel(updates, pendingRepository, dispatcher, routeSink).also { store.put("feed", it) }
        vm.onAction(FeedAction.Retry)
        vm.onAction(FeedAction.Retry)
        advanceUntilIdle()
        assertEquals(1, reads)
        release.complete(Unit)
    }

    @Test
    fun clipActionNavigatesThroughInjectedPortWithoutReadingContent() {
        model().onAction(FeedAction.ClipClicked("clip-2"))
        assertEquals(listOf(FeedRouteEvent.ClipRequested("clip-2")), routeEvents)
        assertEquals(0, reads)
    }

    private fun model() = FeedViewModel(updates, repository, dispatcher, routeSink).also { store.put("feed", it) }

    private val feed = NotmidDestination(
        id = "feed", title = "Feed", subtitle = "Receipts", icon = NotmidNavigationIcon.Feed,
        clips = listOf(NotmidClip(id = "clip-1", title = "Clip", description = "Description", badge = "fresh", palette = emptyList())),
        places = emptyList(),
    )
}
