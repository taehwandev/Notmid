package app.thdev.glassnavlab.feature.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.thdev.glassnavlab.core.navigation.runtime.RouteEventSink
import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthGateway
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteAction
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteExecutor
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteRequest
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteResult
import app.thdev.glassnavlab.core.notice.api.effect.NoticeEffectDelegate
import app.thdev.glassnavlab.feature.feed.api.event.FeedRouteEvent
import app.thdev.glassnavlab.feature.map.api.event.MapRouteEvent
import app.thdev.glassnavlab.feature.notmid.notice.toProtectedActionNotice
import app.thdev.glassnavlab.feature.notmid.notice.toSuccessNotice
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentSnapshot
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentUpdates
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentRepository
import app.thdev.glassnavlab.feature.feed.di.FeedIoDispatcher
import app.thdev.glassnavlab.feature.feed.api.route.FeedRoute
import app.thdev.glassnavlab.feature.notmid.common.model.toNotmidDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
internal class FeedViewModel @Inject constructor(
    contentUpdates: NotmidContentUpdates,
    private val repository: NotmidContentRepository,
    @param:FeedIoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val routeEvents: RouteEventSink,
    private val writes: NotmidProtectedWriteExecutor,
    private val auth: NotmidAuthGateway,
    private val notices: NoticeEffectDelegate,
) : ViewModel() {
    private var retryJob: Job? = null
    private var saveJob: Job? = null

    val state = contentUpdates.snapshot.map { snapshot ->
        when (snapshot) {
            NotmidContentSnapshot.Loading -> FeedLoadState.Loading
            NotmidContentSnapshot.Unavailable -> FeedLoadState.Unavailable
            is NotmidContentSnapshot.Ready -> {
                val destinations = snapshot.destinations.toNotmidDestinations()
                val destination = destinations.firstOrNull { it.id == FeedRoute.selectedDestinationId }
                destination?.let { FeedLoadState.Ready(it.toFeedUiState()) }
                    ?: FeedLoadState.Ready(FeedUiState("Feed", "", null, emptyList(), emptyList()))
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FeedLoadState.Loading)

    fun onAction(action: FeedAction) {
        when (action) {
            is FeedAction.ClipClicked -> routeEvents.onRouteEvent(FeedRouteEvent.ClipRequested(action.clipId))
            is FeedAction.PlaceClicked -> routeEvents.onRouteEvent(MapRouteEvent.PlaceRequested(action.placeId))
            is FeedAction.SaveClicked -> save(action.clipId)
            FeedAction.Retry -> retry()
        }
    }

    private fun save(clipId: String) {
        if (saveJob?.isActive == true) return
        val requestAuth = auth.currentState()
        saveJob = viewModelScope.launch {
            try {
                val result = writes.execute(requestAuth, NotmidProtectedWriteRequest.SaveClip(clipId))
                if (result == NotmidProtectedWriteResult.Busy || auth.currentState().session !== requestAuth.session) return@launch
                check(result == NotmidProtectedWriteResult.Completed)
                notices.emit(NotmidProtectedWriteAction.ClipSave.toSuccessNotice().effect)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (auth.currentState().session === requestAuth.session) {
                    notices.emit(failure.toProtectedActionNotice(NotmidProtectedWriteAction.ClipSave).effect)
                }
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
                // The shared repository publishes Unavailable; never expose transport text to UI.
            }
        }
    }
}
