package app.thdev.glassnavlab.feature.feed

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentRepository
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentSnapshot
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentUpdates
import app.thdev.glassnavlab.core.model.notmid.NotmidClip
import app.thdev.glassnavlab.core.model.notmid.NotmidDestination
import app.thdev.glassnavlab.core.model.notmid.NotmidNavigationIcon
import app.thdev.glassnavlab.core.model.notmid.NotmidPlace
import app.thdev.glassnavlab.core.auth.notmid.*
import app.thdev.glassnavlab.core.model.notmid.*
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteExecutor
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteResult
import app.thdev.glassnavlab.core.notice.api.effect.MutableNoticeEffectDelegate
import app.thdev.glassnavlab.core.notice.api.effect.NoticeEffect
import app.thdev.glassnavlab.core.navigation.runtime.RouteEvent
import app.thdev.glassnavlab.core.navigation.runtime.RouteEventSink
import app.thdev.glassnavlab.feature.inbox.api.event.InboxRouteEvent
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import kotlinx.coroutines.CancellationException
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteRequest
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
class ClipDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val updates = object : NotmidContentUpdates {
        override val snapshot = MutableStateFlow<NotmidContentSnapshot>(NotmidContentSnapshot.Loading)
    }
    private var reads = 0
    private val submitted = mutableListOf<NotmidProtectedWriteRequest>()
    private val thread = NotmidThread("created/thread", "Chat", "", "", listOf("@second"))
    private var response: suspend () -> NotmidProtectedWriteResult = { NotmidProtectedWriteResult.ThreadStarted(thread, null) }
    private val writeActions = object : NotmidProtectedWriteExecutor {
        override suspend fun execute(authState: NotmidAuthState, request: NotmidProtectedWriteRequest): NotmidProtectedWriteResult {
            submitted.add(request)
            return response()
        }
    }
    private val auth = object : NotmidAuthGateway {
        override val states = MutableStateFlow(NotmidAuthState(NotmidAuthMode.Fake,
            NotmidAuthSession("token", NotmidAuthProvider.Fake, "", NotmidAuthUser("user", "handle", "Name", "Seoul", "", emptyList())), emptyList()))
        override fun currentState() = states.value
        override fun signOut() = states.value.copy(session = null).also { states.value = it }
        override suspend fun signIn(request: NotmidAuthSignInRequest): NotmidAuthResult = error("No sign-in expected")
        override fun applyProfileUpdate(expectedSession: NotmidAuthSession, user: NotmidAuthUser): NotmidAuthState = error("No profile write expected")
    }
    private val notices = MutableNoticeEffectDelegate()
    private val routeEvents = mutableListOf<RouteEvent>()
    private val routes = RouteEventSink { routeEvents.add(it) }
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
    fun restoredClipIdSelectsFeedAndDoesNotReadAgain() = runTest(dispatcher) {
        val vm = model("clip-2")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        updates.snapshot.value = NotmidContentSnapshot.Ready(listOf(feed.copy(id = "map", clips = emptyList()), feed))
        advanceUntilIdle()
        val state = vm.state.value as ClipDetailUiState.Ready
        assertEquals("Second", state.clip.title)
        assertEquals("@second", state.clip.creatorHandle)
        assertEquals(0, reads)
    }

    @Test
    fun separateClipModelsKeepTheirIdentityAcrossSharedRefresh() = runTest(dispatcher) {
        val first = model("clip-1")
        val second = model("clip-2")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { first.state.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { second.state.collect {} }
        updates.snapshot.value = NotmidContentSnapshot.Ready(listOf(feed))
        advanceUntilIdle()
        assertEquals("First", (first.state.value as ClipDetailUiState.Ready).clip.title)
        assertEquals("Second", (second.state.value as ClipDetailUiState.Ready).clip.title)
        updates.snapshot.value = NotmidContentSnapshot.Ready(listOf(feed.copy(clips = feed.clips.map { it.copy(title = it.title + " refreshed") })))
        advanceUntilIdle()
        assertEquals("First refreshed", (first.state.value as ClipDetailUiState.Ready).clip.title)
        assertEquals("Second refreshed", (second.state.value as ClipDetailUiState.Ready).clip.title)
        assertEquals(0, reads)
    }

    @Test
    fun missingClipAndEmptyDataPreservePlaceholderAndDisableChat() = runTest(dispatcher) {
        val vm = model("missing-id")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        updates.snapshot.value = NotmidContentSnapshot.Ready(emptyList())
        advanceUntilIdle()
        val state = vm.state.value as ClipDetailUiState.Ready
        assertEquals("missing-id", state.clip.id)
        assertEquals("Clip", state.clip.title)
        assertEquals("", state.clip.creatorHandle)
        assertEquals("clip-missing-id-place", state.place.id)
    }

    @Test
    fun missingFeedUsesFirstDestinationAndRetryRecoversUnavailable() = runTest(dispatcher) {
        val vm = model("clip-1")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        updates.snapshot.value = NotmidContentSnapshot.Ready(listOf(feed.copy(id = "other")))
        advanceUntilIdle()
        assertEquals("First", (vm.state.value as ClipDetailUiState.Ready).clip.title)
        updates.snapshot.value = NotmidContentSnapshot.Unavailable
        advanceUntilIdle()
        assertSame(ClipDetailUiState.Unavailable, vm.state.value)
        vm.onAction(ClipDetailAction.Retry)
        advanceUntilIdle()
        assertEquals(1, reads)
        assertEquals("First", (vm.state.value as ClipDetailUiState.Ready).clip.title)
    }

    @Test
    fun chatActionBuildsRequestInViewModelAndMissingClipDoesNotSubmit() = runTest(dispatcher) {
        val vm = model("clip-2")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        vm.onAction(ClipDetailAction.ChatClicked)
        advanceUntilIdle()
        assertEquals(0, submitted.size)
        updates.snapshot.value = NotmidContentSnapshot.Ready(listOf(feed))
        advanceUntilIdle()
        vm.onAction(ClipDetailAction.ChatClicked)
        advanceUntilIdle()
        val request = (submitted.single() as NotmidProtectedWriteRequest.StartThread).request
        assertEquals("@second", request.participantHandle)
        assertEquals("clip-2", request.attachedClipId)
        assertEquals("Can we chat about Second?", request.body)
        updates.snapshot.value = NotmidContentSnapshot.Ready(emptyList())
        advanceUntilIdle()
        vm.onAction(ClipDetailAction.ChatClicked)
        advanceUntilIdle()
        assertEquals(1, submitted.size)
    }

    @Test
    fun linkedPlaceWinsOverFirstPlaceAndMissingLinkFallsBack() = runTest(dispatcher) {
        val firstPlace = NotmidPlace(id = "first", title = "First place", description = "", metric = "", palette = emptyList(), heightDp = 176)
        val linkedPlace = firstPlace.copy(id = "linked", title = "Linked place")
        val vm = model("clip-1")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        val linkedFeed = feed.copy(
            clips = listOf(feed.clips.first().copy(placeId = "linked")),
            places = listOf(firstPlace, linkedPlace),
        )
        updates.snapshot.value = NotmidContentSnapshot.Ready(listOf(linkedFeed))
        advanceUntilIdle()
        assertEquals("linked", (vm.state.value as ClipDetailUiState.Ready).place.id)
        updates.snapshot.value = NotmidContentSnapshot.Ready(listOf(linkedFeed.copy(places = listOf(firstPlace))))
        advanceUntilIdle()
        assertEquals("first", (vm.state.value as ClipDetailUiState.Ready).place.id)
    }

    @Test
    fun retryIsSingleFlightAndClearingOwnerCancelsPendingRead() = runTest(dispatcher) {
        val started = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        val pendingRepository = object : NotmidContentRepository {
            override suspend fun destinations(): List<NotmidDestination> {
                reads++
                started.complete(Unit)
                try {
                    CompletableDeferred<Unit>().await()
                } finally {
                    cancelled.complete(Unit)
                }
                return emptyList()
            }
        }
        val vm = ClipDetailViewModel(SavedStateHandle(mapOf(ClipDetailViewModel.CLIP_ID to "clip-1")), updates, pendingRepository, dispatcher, writeActions, auth, notices, routes)
            .also { store.put("pending", it) }
        vm.onAction(ClipDetailAction.Retry)
        vm.onAction(ClipDetailAction.Retry)
        advanceUntilIdle()
        assertEquals(true, started.isCompleted)
        assertEquals(1, reads)
        store.clear()
        advanceUntilIdle()
        assertEquals(true, cancelled.isCompleted)
    }

    private fun model(clipId: String) = ClipDetailViewModel(
        SavedStateHandle(mapOf(ClipDetailViewModel.CLIP_ID to clipId)), updates, repository, dispatcher, writeActions, auth, notices, routes,
    ).also { store.put(clipId, it) }

    @Test
    fun successfulCreationOwnsBusyStateNoticeAndTypedRoute() = runTest(dispatcher) {
        val pending = CompletableDeferred<NotmidProtectedWriteResult>()
        response = { pending.await() }
        val effects = mutableListOf<NoticeEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { notices.effects.collect { effects.add(it) } }
        val vm = model("clip-2")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        updates.snapshot.value = NotmidContentSnapshot.Ready(listOf(feed))
        // Actions read the latest source even before the state collector runs.
        vm.onAction(ClipDetailAction.ChatClicked)
        vm.onAction(ClipDetailAction.ChatClicked)
        runCurrent()
        assertEquals(1, submitted.size)
        assertTrue((vm.state.value as ClipDetailUiState.Ready).isStartingChat)
        assertTrue(routeEvents.isEmpty())
        pending.complete(NotmidProtectedWriteResult.ThreadStarted(thread, null))
        runCurrent()
        assertFalse((vm.state.value as ClipDetailUiState.Ready).isStartingChat)
        assertEquals("Chat started.", (effects.single() as NoticeEffect.ShowNotice).notice.message)
        assertEquals(listOf(InboxRouteEvent.ChatThreadRequested("created/thread")), routeEvents)
    }

    @Test
    fun busyCancellationAndFailureDoNotNavigateAndAllowRetry() = runTest(dispatcher) {
        var attempt = 0
        response = {
            when (attempt++) {
                0 -> NotmidProtectedWriteResult.Busy
                1 -> throw CancellationException("cancel")
                else -> error("private transport detail")
            }
        }
        val effects = mutableListOf<NoticeEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { notices.effects.collect { effects.add(it) } }
        val vm = model("clip-2")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        updates.snapshot.value = NotmidContentSnapshot.Ready(listOf(feed))
        repeat(2) {
            vm.onAction(ClipDetailAction.ChatClicked)
            runCurrent()
            assertFalse((vm.state.value as ClipDetailUiState.Ready).isStartingChat)
            assertTrue(effects.isEmpty())
            assertTrue(routeEvents.isEmpty())
        }
        vm.onAction(ClipDetailAction.ChatClicked)
        runCurrent()
        assertFalse((vm.state.value as ClipDetailUiState.Ready).isStartingChat)
        assertEquals("This action failed. Try again.", (effects.single() as NoticeEffect.ShowNotice).notice.message)
        assertTrue(routeEvents.isEmpty())
    }

    @Test
    fun logoutBeforeReceiptSuppressesNoticeAndNavigation() = runTest(dispatcher) {
        val pending = CompletableDeferred<NotmidProtectedWriteResult>()
        response = { pending.await() }
        val effects = mutableListOf<NoticeEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { notices.effects.collect { effects.add(it) } }
        val vm = model("clip-2")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        updates.snapshot.value = NotmidContentSnapshot.Ready(listOf(feed))
        vm.onAction(ClipDetailAction.ChatClicked)
        runCurrent()
        auth.signOut()
        pending.complete(NotmidProtectedWriteResult.ThreadStarted(thread, null))
        runCurrent()
        assertFalse((vm.state.value as ClipDetailUiState.Ready).isStartingChat)
        assertTrue(effects.isEmpty())
        assertTrue(routeEvents.isEmpty())
    }

    private val feed = NotmidDestination(
        id = "feed", title = "Feed", subtitle = "Receipts", icon = NotmidNavigationIcon.Feed,
        clips = listOf(
            NotmidClip(id = "clip-1", title = "First", description = "Description", badge = "fresh", palette = emptyList()),
            NotmidClip(id = "clip-2", title = "Second", description = "Description", badge = "fresh", palette = emptyList(), creatorHandle = "@second"),
        ),
        places = emptyList(),
    )
}
