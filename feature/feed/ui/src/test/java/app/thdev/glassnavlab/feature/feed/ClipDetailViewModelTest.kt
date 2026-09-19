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
import app.thdev.glassnavlab.core.model.notmid.ChannelNotmidActionDelegate
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
    private val writeActions = ChannelNotmidActionDelegate<NotmidProtectedWriteRequest>()
    private val repository = object : NotmidContentRepository {
        override suspend fun destinations(): List<NotmidDestination> {
            reads++
            return listOf(feed).also { updates.snapshot.value = NotmidContentSnapshot.Ready(it) }
        }
    }

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() {
        store.clear()
        writeActions.close()
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
        val submitted = mutableListOf<NotmidProtectedWriteRequest>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { writeActions.actions.collect { submitted.add(it) } }
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
        val vm = ClipDetailViewModel(SavedStateHandle(mapOf(ClipDetailViewModel.CLIP_ID to "clip-1")), updates, pendingRepository, dispatcher, writeActions)
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
        SavedStateHandle(mapOf(ClipDetailViewModel.CLIP_ID to clipId)), updates, repository, dispatcher, writeActions,
    ).also { store.put(clipId, it) }

    private val feed = NotmidDestination(
        id = "feed", title = "Feed", subtitle = "Receipts", icon = NotmidNavigationIcon.Feed,
        clips = listOf(
            NotmidClip(id = "clip-1", title = "First", description = "Description", badge = "fresh", palette = emptyList()),
            NotmidClip(id = "clip-2", title = "Second", description = "Description", badge = "fresh", palette = emptyList(), creatorHandle = "@second"),
        ),
        places = emptyList(),
    )
}
