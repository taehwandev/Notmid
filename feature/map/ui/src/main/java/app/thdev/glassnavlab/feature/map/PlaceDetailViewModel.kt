package app.thdev.glassnavlab.feature.map

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentRepository
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentSnapshot
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentUpdates
import app.thdev.glassnavlab.feature.map.api.route.PlaceDetailRoute
import app.thdev.glassnavlab.feature.map.di.MapIoDispatcher
import app.thdev.glassnavlab.feature.notmid.common.model.toNotmidDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
internal class PlaceDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    contentUpdates: NotmidContentUpdates,
    private val repository: NotmidContentRepository,
    @param:MapIoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val route = PlaceDetailRoute(checkNotNull(savedStateHandle.get<String>(PLACE_ID)))
    private var retryJob: Job? = null

    val state = contentUpdates.snapshot.map { snapshot ->
        when (snapshot) {
            NotmidContentSnapshot.Loading -> PlaceDetailUiState.Loading
            NotmidContentSnapshot.Unavailable -> PlaceDetailUiState.Unavailable
            is NotmidContentSnapshot.Ready -> {
                val destinations = snapshot.destinations.toNotmidDestinations()
                val destination = destinations.firstOrNull { it.id == route.selectedDestinationId }
                    ?: destinations.firstOrNull()
                destination.toPlaceDetailUiState(route.placeId)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaceDetailUiState.Loading)

    fun onAction(action: PlaceDetailAction) {
        when (action) {
            PlaceDetailAction.Retry -> retry()
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
                // The shared repository publishes Unavailable without transport details.
            }
        }
    }

    companion object {
        const val PLACE_ID = "placeId"
    }
}
