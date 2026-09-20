package app.thdev.glassnavlab.feature.map

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentRepository
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentSnapshot
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentUpdates
import app.thdev.glassnavlab.core.model.notmid.NotmidDestination
import app.thdev.glassnavlab.core.model.notmid.NotmidGeoPoint
import app.thdev.glassnavlab.core.model.notmid.NotmidNavigationIcon
import app.thdev.glassnavlab.core.model.notmid.NotmidPlace
import app.thdev.glassnavlab.core.navigation.runtime.RouteEvent
import app.thdev.glassnavlab.core.navigation.runtime.RouteEventSink
import app.thdev.glassnavlab.feature.map.api.event.MapRouteEvent
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val updates = object : NotmidContentUpdates {
        override val snapshot = MutableStateFlow<NotmidContentSnapshot>(NotmidContentSnapshot.Loading)
    }
    private val events = mutableListOf<RouteEvent>()
    private var reads = 0
    private val repository = object : NotmidContentRepository {
        override suspend fun destinations(): List<NotmidDestination> {
            reads++
            return listOf(destination).also { updates.snapshot.value = NotmidContentSnapshot.Ready(it) }
        }
    }
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() { store.clear(); Dispatchers.resetMain() }

    @Test fun observesMapWithoutReadAndRestoresFilterAndPlace() = runTest(dispatcher) {
        val vm = model(SavedStateHandle(mapOf("category" to "Work", "placeId" to "work")))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        updates.snapshot.value = NotmidContentSnapshot.Ready(listOf(destination.copy(id = "feed", places = emptyList()), destination))
        advanceUntilIdle()
        val ready = vm.state.value as MapUiState.Ready
        assertEquals("map", ready.destinationId)
        assertEquals(listOf("work"), ready.visiblePins.map { it.place.id })
        assertEquals("work", ready.selectedPin?.place?.id)
        assertEquals(0, reads)
    }

    @Test fun selectionAndOpenActionsUseViewModelRoutePort() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        val vm = model(handle)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        vm.onAction(MapAction.OpenSelectedPlace)
        assertEquals(0, events.size)
        updates.snapshot.value = NotmidContentSnapshot.Ready(listOf(destination))
        advanceUntilIdle()
        vm.onAction(MapAction.PinSelected("work"))
        advanceUntilIdle()
        vm.onAction(MapAction.OpenSelectedPlace)
        assertEquals(listOf(MapRouteEvent.PlaceRequested("work")), events)
        vm.onAction(MapAction.CategorySelected("Cafe"))
        advanceUntilIdle()
        assertEquals("Cafe", handle.get<String>("category"))
        assertEquals("cafe", (vm.state.value as MapUiState.Ready).selectedPin?.place?.id)
    }

    @Test fun refreshAndEmptyDataPreserveFallbackRules() = runTest(dispatcher) {
        val vm = model(SavedStateHandle(mapOf("category" to "Missing")))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        updates.snapshot.value = NotmidContentSnapshot.Ready(listOf(destination.copy(id = "fallback")))
        advanceUntilIdle()
        val ready = vm.state.value as MapUiState.Ready
        assertEquals("fallback", ready.destinationId)
        assertEquals(emptyList<MapPinUi>(), ready.visiblePins)
        assertEquals("cafe", ready.selectedPin?.place?.id)
        updates.snapshot.value = NotmidContentSnapshot.Ready(emptyList())
        advanceUntilIdle()
        assertNull((vm.state.value as MapUiState.Ready).selectedPin)
        vm.onAction(MapAction.OpenSelectedPlace)
        assertEquals(0, events.size)
    }

    @Test fun errorRetryRecoversAndDoesNotDuplicatePendingRead() = runTest(dispatcher) {
        val release = CompletableDeferred<Unit>()
        val pending = object : NotmidContentRepository {
            override suspend fun destinations(): List<NotmidDestination> {
                release.await()
                return repository.destinations()
            }
        }
        val vm = model(repository = pending)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        updates.snapshot.value = NotmidContentSnapshot.Unavailable
        advanceUntilIdle()
        assertSame(MapUiState.Unavailable, vm.state.value)
        vm.onAction(MapAction.Retry)
        vm.onAction(MapAction.Retry)
        advanceUntilIdle()
        release.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, reads)
        assertEquals("map", (vm.state.value as MapUiState.Ready).destinationId)
    }

    @Test fun coordinatesClampAndMissingCoordinatesKeepFixtureLayout() = runTest(dispatcher) {
        val vm = model()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        updates.snapshot.value = NotmidContentSnapshot.Ready(listOf(destination.copy(places = listOf(
            destination.places.first().copy(coordinate = NotmidGeoPoint(90.0, 180.0)), destination.places.last(),
        ))))
        advanceUntilIdle()
        val pins = (vm.state.value as MapUiState.Ready).visiblePins
        assertEquals(0.86f, pins[0].xFraction)
        assertEquals(0.12f, pins[0].yFraction)
        assertEquals(0.62f, pins[1].xFraction)
        assertEquals(0.34f, pins[1].yFraction)
    }

    private fun model(handle: SavedStateHandle = SavedStateHandle(), repository: NotmidContentRepository = this.repository) =
        MapViewModel(handle, updates, repository, RouteEventSink { events.add(it) }, dispatcher).also { store.put("map", it) }

    private val destination = NotmidDestination(
        id = "map", title = "Map", subtitle = "", icon = NotmidNavigationIcon.Map,
        clips = emptyList(), places = listOf(
            NotmidPlace(id = "cafe", title = "Cafe", description = "", metric = "", palette = emptyList(), heightDp = 176, category = "Cafe"),
            NotmidPlace(id = "work", title = "Work", description = "", metric = "", palette = emptyList(), heightDp = 176, category = "Work"),
        ),
    )
}
