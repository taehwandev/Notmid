package app.thdev.glassnavlab.feature.feed

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthGateway
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteExecutor
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteResult
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteAction
import app.thdev.glassnavlab.core.notice.api.effect.NoticeEffectDelegate
import app.thdev.glassnavlab.core.navigation.runtime.RouteEventSink
import app.thdev.glassnavlab.feature.inbox.api.event.InboxRouteEvent
import app.thdev.glassnavlab.feature.notmid.notice.toSuccessNotice
import app.thdev.glassnavlab.feature.notmid.notice.toProtectedActionNotice
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
internal class ClipDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contentUpdates: NotmidContentUpdates,
    private val repository: NotmidContentRepository,
    @param:FeedIoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val writes: NotmidProtectedWriteExecutor,
    private val auth: NotmidAuthGateway,
    private val notices: NoticeEffectDelegate,
    private val routes: RouteEventSink,
) : ViewModel() {
    private val route = ClipDetailRoute(checkNotNull(savedStateHandle.get<String>(CLIP_ID)))
    private var retryJob: Job? = null
    private val startingChat = MutableStateFlow(false)

    val state = combine(contentUpdates.snapshot, startingChat) { snapshot, starting ->
        val content = resolve(snapshot)
        if (content is ClipDetailUiState.Ready) content.copy(isStartingChat = starting) else content
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClipDetailUiState.Loading)

    private fun resolve(snapshot: NotmidContentSnapshot): ClipDetailUiState = when (snapshot) {
        NotmidContentSnapshot.Loading -> ClipDetailUiState.Loading
        NotmidContentSnapshot.Unavailable -> ClipDetailUiState.Unavailable
        is NotmidContentSnapshot.Ready -> {
            val destinations = snapshot.destinations.toNotmidDestinations()
            val destination = destinations.firstOrNull { it.id == route.selectedDestinationId }
                ?: destinations.firstOrNull()
            destination.toClipDetailUiState(route.clipId)
        }
    }

    fun onAction(action: ClipDetailAction) {
        when (action) {
            ClipDetailAction.Retry -> retry()
            ClipDetailAction.ChatClicked -> {
                if (startingChat.value) return
                val clip = (resolve(contentUpdates.snapshot.value) as? ClipDetailUiState.Ready)?.clip ?: return
                if (clip.creatorHandle.isBlank()) return
                val request = NotmidProtectedWriteRequest.StartThread(
                    NotmidStartThreadRequest(
                        participantHandle = clip.creatorHandle,
                        body = "Can we chat about ${clip.title}?",
                        attachedClipId = clip.id,
                        attachedPlaceId = clip.placeId,
                    ),
                )
                startChat(request)
            }
        }
    }

    private fun startChat(request: NotmidProtectedWriteRequest.StartThread) {
        val requestAuth = auth.currentState()
        startingChat.value = true
        viewModelScope.launch {
            try {
                val result = writes.execute(requestAuth, request)
                if (result == NotmidProtectedWriteResult.Busy || auth.currentState().session !== requestAuth.session) return@launch
                check(result is NotmidProtectedWriteResult.ThreadStarted)
                notices.emit(NotmidProtectedWriteAction.ChatStart.toSuccessNotice().effect)
                routes.onRouteEvent(InboxRouteEvent.ChatThreadRequested(result.thread.id))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (auth.currentState().session === requestAuth.session) {
                    notices.emit(failure.toProtectedActionNotice(NotmidProtectedWriteAction.ChatStart).effect)
                }
            } finally {
                startingChat.value = false
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
