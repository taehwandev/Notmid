package app.thdev.glassnavlab.feature.map

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.graphics.Color
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentRepository
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentSnapshot
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentUpdates
import app.thdev.glassnavlab.core.navigation.runtime.RouteEventSink
import app.thdev.glassnavlab.feature.map.api.event.MapRouteEvent
import app.thdev.glassnavlab.feature.map.api.route.MapRoute
import app.thdev.glassnavlab.feature.map.di.MapIoDispatcher
import app.thdev.glassnavlab.feature.notmid.common.model.toNotmidDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
internal class MapViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val updates: NotmidContentUpdates,
    private val repository: NotmidContentRepository,
    private val routeEvents: RouteEventSink,
    @param:MapIoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private var retryJob: Job? = null
    private val category = savedState.getStateFlow("category", "All")
    private val placeId = savedState.getStateFlow("placeId", "")

    val state = combine(updates.snapshot, category, placeId) { snapshot, category, placeId ->
        when (snapshot) {
            NotmidContentSnapshot.Loading -> MapUiState.Loading
            NotmidContentSnapshot.Unavailable -> MapUiState.Unavailable
            is NotmidContentSnapshot.Ready -> {
                val destinations = snapshot.destinations.toNotmidDestinations()
                val destination = destinations.firstOrNull { it.id == MapRoute.selectedDestinationId }
                    ?: destinations.firstOrNull()
                val pins = destination?.places.orEmpty().mapIndexed { index, place -> place.toMapPin(index) }
                val categories = listOf("All") + pins.map { it.category }.filter(String::isNotBlank)
                    .distinct().ifEmpty { DefaultMapCategories.drop(1) }
                val visible = if (category == "All") pins else pins.filter { it.category == category }
                MapUiState.Ready(
                    destinationId = destination?.id ?: MapRoute.selectedDestinationId,
                    title = destination?.title.orEmpty(),
                    categories = categories,
                    selectedCategory = category,
                    visiblePins = visible,
                    selectedPin = visible.firstOrNull { it.place.id == placeId } ?: visible.firstOrNull() ?: pins.firstOrNull(),
                    backdropPalettes = listOf(emptyList<Color>()) +
                        destination?.clips.orEmpty().map { it.palette } + destination?.places.orEmpty().map { it.palette },
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapUiState.Loading)

    fun onAction(action: MapAction) {
        when (action) {
            is MapAction.CategorySelected -> {
                savedState["placeId"] = ""
                savedState["category"] = action.category
            }
            is MapAction.PinSelected -> savedState["placeId"] = action.placeId
            MapAction.OpenSelectedPlace -> (state.value as? MapUiState.Ready)?.selectedPin?.place?.id?.let {
                routeEvents.onRouteEvent(MapRouteEvent.PlaceRequested(it))
            }
            MapAction.Retry -> retry()
        }
    }

    private fun retry() {
        if (retryJob?.isActive == true) return
        retryJob = viewModelScope.launch {
            try {
                withContext(ioDispatcher) { repository.destinations() }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // The repository publishes the safe Unavailable snapshot.
            }
        }
    }
}
