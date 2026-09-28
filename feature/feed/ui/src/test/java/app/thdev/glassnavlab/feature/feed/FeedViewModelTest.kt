package app.thdev.glassnavlab.feature.feed

import androidx.lifecycle.ViewModelStore
import app.thdev.glassnavlab.core.navigation.runtime.RouteEvent
import app.thdev.glassnavlab.core.navigation.runtime.RouteEventSink
import app.thdev.glassnavlab.feature.feed.api.event.FeedRouteEvent
import app.thdev.glassnavlab.feature.map.api.event.MapRouteEvent
import app.thdev.glassnavlab.core.auth.notmid.*
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteExecutor
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteRequest
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteResult
import app.thdev.glassnavlab.core.notice.api.effect.MutableNoticeEffectDelegate
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthMode
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthProvider
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthSession
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthUser
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
    private val submitted = mutableListOf<NotmidProtectedWriteRequest>()
    private val writes = NotmidProtectedWriteExecutor { _, request ->
        submitted.add(request)
        NotmidProtectedWriteResult.Completed
    }
    private val auth = object : NotmidAuthGateway {
        override val states = MutableStateFlow(NotmidAuthState(NotmidAuthMode.Fake,
            NotmidAuthSession("token", NotmidAuthProvider.Fake, "", NotmidAuthUser("user", "handle", "Name", "Seoul", "", emptyList())), emptyList()))
        override fun currentState() = states.value
        override fun signOut() = states.value.copy(session = null).also { states.value = it }
        override suspend fun signIn(request: NotmidAuthSignInRequest): NotmidAuthResult = error("No sign-in expected")
        override fun applyProfileUpdate(expectedSession: NotmidAuthSession, user: NotmidAuthUser): NotmidAuthState = error("No profile update expected")
    }
    private val notices = MutableNoticeEffectDelegate()
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
    fun missingFeedDoesNotDisplayClipsFromAnotherDestination() = runTest(dispatcher) {
        val vm = model()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        updates.snapshot.value = NotmidContentSnapshot.Ready(listOf(feed.copy(id = "other", title = "Fallback")))
        advanceUntilIdle()
        assertEquals(true, (vm.state.value as FeedLoadState.Ready).content.isEmpty)
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
        val vm = FeedViewModel(updates, pendingRepository, dispatcher, routeSink, writes, auth, notices).also { store.put("feed", it) }
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

    @Test
    fun placeActionOpensPlaceAndSaveActionExecutesProtectedWrite() = runTest(dispatcher) {
        val vm = model()
        vm.onAction(FeedAction.PlaceClicked("place-2"))
        vm.onAction(FeedAction.SaveClicked("clip-2"))
        advanceUntilIdle()
        assertEquals(listOf(MapRouteEvent.PlaceRequested("place-2")), routeEvents)
        assertEquals(listOf(NotmidProtectedWriteRequest.SaveClip("clip-2")), submitted)
    }

    private fun model() = FeedViewModel(updates, repository, dispatcher, routeSink, writes, auth, notices).also { store.put("feed", it) }

    private val feed = NotmidDestination(
        id = "feed", title = "Feed", subtitle = "Receipts", icon = NotmidNavigationIcon.Feed,
        clips = listOf(NotmidClip(id = "clip-1", title = "Clip", description = "Description", badge = "fresh", palette = emptyList())),
        places = emptyList(),
    )
}
