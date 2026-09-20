package app.thdev.glassnavlab.feature.map

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentRepository
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentSnapshot
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentUpdates
import app.thdev.glassnavlab.core.model.notmid.NotmidClip
import app.thdev.glassnavlab.core.model.notmid.NotmidDestination
import app.thdev.glassnavlab.core.model.notmid.NotmidNavigationIcon
import app.thdev.glassnavlab.core.model.notmid.NotmidPlace
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
class PlaceDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val updates = object : NotmidContentUpdates {
        override val snapshot = MutableStateFlow<NotmidContentSnapshot>(NotmidContentSnapshot.Loading)
    }
    private var reads = 0
    private val repository = object : NotmidContentRepository {
        override suspend fun destinations(): List<NotmidDestination> {
            reads++
            return listOf(map).also { updates.snapshot.value = NotmidContentSnapshot.Ready(it) }
        }
    }

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun restoredPlaceIdSelectsMapAndLinkedClipWithoutReadingAgain() = runTest(dispatcher) {
        val vm = model("place-2")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        updates.snapshot.value = NotmidContentSnapshot.Ready(listOf(map.copy(id = "feed", places = emptyList()), map))
        advanceUntilIdle()
        val state = vm.state.value as PlaceDetailUiState.Ready
        assertEquals("Second place", state.place.title)
        assertEquals("clip-2", state.clip.id)
        assertEquals(5, state.backdropPalettes.size)
        assertEquals(0, reads)
    }

    @Test
    fun separatePlaceModelsKeepIdentityAcrossSharedRefresh() = runTest(dispatcher) {
        val first = model("place-1")
        val second = model("place-2")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { first.state.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { second.state.collect {} }
        updates.snapshot.value = NotmidContentSnapshot.Ready(listOf(map))
        advanceUntilIdle()
        assertEquals("First place", (first.state.value as PlaceDetailUiState.Ready).place.title)
        assertEquals("Second place", (second.state.value as PlaceDetailUiState.Ready).place.title)
        updates.snapshot.value = NotmidContentSnapshot.Ready(listOf(map.copy(places = map.places.map { it.copy(title = it.title + " refreshed") })))
        advanceUntilIdle()
        assertEquals("First place refreshed", (first.state.value as PlaceDetailUiState.Ready).place.title)
        assertEquals("Second place refreshed", (second.state.value as PlaceDetailUiState.Ready).place.title)
        assertEquals(0, reads)
    }

    @Test
    fun missingPlaceFallsBackToFirstClipAndEmptyDataPreservesPlaceholder() = runTest(dispatcher) {
        val vm = model("missing-id")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        updates.snapshot.value = NotmidContentSnapshot.Ready(listOf(map))
        advanceUntilIdle()
        assertEquals("clip-1", (vm.state.value as PlaceDetailUiState.Ready).clip.id)
        updates.snapshot.value = NotmidContentSnapshot.Ready(emptyList())
        advanceUntilIdle()
        val state = vm.state.value as PlaceDetailUiState.Ready
        assertEquals("missing-id", state.place.id)
        assertEquals("Place", state.place.title)
        assertEquals("place-missing-id-clip", state.clip.id)
        assertEquals(state.place.palette, state.clip.palette)
    }

    @Test
    fun missingMapUsesFirstDestinationAndRetryRecoversUnavailable() = runTest(dispatcher) {
        val vm = model("place-1")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        updates.snapshot.value = NotmidContentSnapshot.Ready(listOf(map.copy(id = "other")))
        advanceUntilIdle()
        assertEquals("First place", (vm.state.value as PlaceDetailUiState.Ready).place.title)
        updates.snapshot.value = NotmidContentSnapshot.Unavailable
        advanceUntilIdle()
        assertSame(PlaceDetailUiState.Unavailable, vm.state.value)
        vm.onAction(PlaceDetailAction.Retry)
        advanceUntilIdle()
        assertEquals(1, reads)
        assertEquals("First place", (vm.state.value as PlaceDetailUiState.Ready).place.title)
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
        val vm = PlaceDetailViewModel(SavedStateHandle(mapOf(PlaceDetailViewModel.PLACE_ID to "place-1")), updates, pendingRepository, dispatcher)
            .also { store.put("pending", it) }
        vm.onAction(PlaceDetailAction.Retry)
        vm.onAction(PlaceDetailAction.Retry)
        advanceUntilIdle()
        assertEquals(true, started.isCompleted)
        assertEquals(1, reads)
        store.clear()
        advanceUntilIdle()
        assertEquals(true, cancelled.isCompleted)
    }

    private fun model(placeId: String) = PlaceDetailViewModel(
        SavedStateHandle(mapOf(PlaceDetailViewModel.PLACE_ID to placeId)), updates, repository, dispatcher,
    ).also { store.put(placeId, it) }

    private val map = NotmidDestination(
        id = "map", title = "Map", subtitle = "Proof", icon = NotmidNavigationIcon.Map,
        clips = listOf(
            NotmidClip(id = "clip-1", title = "First clip", description = "", badge = "fresh", palette = emptyList(), placeId = "place-1"),
            NotmidClip(id = "clip-2", title = "Second clip", description = "", badge = "fresh", palette = emptyList(), placeId = "place-2"),
        ),
        places = listOf(
            NotmidPlace(id = "place-1", title = "First place", description = "", metric = "", palette = emptyList(), heightDp = 176),
            NotmidPlace(id = "place-2", title = "Second place", description = "", metric = "", palette = emptyList(), heightDp = 176),
        ),
    )
}
