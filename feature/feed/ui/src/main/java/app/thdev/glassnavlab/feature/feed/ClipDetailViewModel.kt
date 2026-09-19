package app.thdev.glassnavlab.feature.feed

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.thdev.glassnavlab.core.model.notmid.NotmidActionDelegate
import app.thdev.glassnavlab.core.model.notmid.NotmidStartThreadRequest
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteRequest
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentRepository
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentSnapshot
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentUpdates
import app.thdev.glassnavlab.feature.feed.api.route.ClipDetailRoute
import app.thdev.glassnavlab.feature.feed.di.FeedIoDispatcher
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
internal class ClipDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    contentUpdates: NotmidContentUpdates,
    private val repository: NotmidContentRepository,
    @param:FeedIoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val protectedWriteActions: NotmidActionDelegate<NotmidProtectedWriteRequest>,
) : ViewModel() {
    private val route = ClipDetailRoute(checkNotNull(savedStateHandle.get<String>(CLIP_ID)))
    private var retryJob: Job? = null

    val state = contentUpdates.snapshot.map { snapshot ->
        when (snapshot) {
            NotmidContentSnapshot.Loading -> ClipDetailUiState.Loading
            NotmidContentSnapshot.Unavailable -> ClipDetailUiState.Unavailable
            is NotmidContentSnapshot.Ready -> {
                val destinations = snapshot.destinations.toNotmidDestinations()
                val destination = destinations.firstOrNull { it.id == route.selectedDestinationId }
                    ?: destinations.firstOrNull()
                destination.toClipDetailUiState(route.clipId)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClipDetailUiState.Loading)

    fun onAction(action: ClipDetailAction) {
        when (action) {
            ClipDetailAction.Retry -> retry()
            ClipDetailAction.ChatClicked -> {
                val clip = (state.value as? ClipDetailUiState.Ready)?.clip ?: return
                if (clip.creatorHandle.isBlank()) return
                val request = NotmidProtectedWriteRequest.StartThread(
                    NotmidStartThreadRequest(
                        participantHandle = clip.creatorHandle,
                        body = "Can we chat about ${clip.title}?",
                        attachedClipId = clip.id,
                        attachedPlaceId = clip.placeId,
                    ),
                )
                viewModelScope.launch { protectedWriteActions.dispatch(request) }
            }
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
        const val CLIP_ID = "clipId"
    }
}
