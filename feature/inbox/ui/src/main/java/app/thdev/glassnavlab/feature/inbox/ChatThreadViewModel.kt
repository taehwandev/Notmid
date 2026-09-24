package app.thdev.glassnavlab.feature.inbox

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentRepository
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentSnapshot
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentUpdates
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteRequest
import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthGateway
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteExecutor
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteResult
import app.thdev.glassnavlab.core.notice.api.effect.NoticeEffectDelegate
import app.thdev.glassnavlab.feature.notmid.notice.toSuccessNotice
import app.thdev.glassnavlab.feature.notmid.notice.toProtectedActionNotice
import kotlinx.coroutines.flow.MutableStateFlow
import app.thdev.glassnavlab.core.model.notmid.NotmidChatInviteDecision
import app.thdev.glassnavlab.core.model.notmid.NotmidSendThreadMessageRequest
import app.thdev.glassnavlab.core.navigation.runtime.RouteEventSink
import app.thdev.glassnavlab.feature.inbox.api.event.InboxRouteEvent
import app.thdev.glassnavlab.feature.inbox.api.route.ChatThreadRoute
import app.thdev.glassnavlab.feature.inbox.di.InboxIoDispatcher
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
internal class ChatThreadViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val updates: NotmidContentUpdates,
    private val repository: NotmidContentRepository,
    private val writes: NotmidProtectedWriteExecutor,
    private val routeEvents: RouteEventSink,
    @param:InboxIoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val auth: NotmidAuthGateway,
    private val notices: NoticeEffectDelegate,
) : ViewModel() {
    private val route = ChatThreadRoute(checkNotNull(savedState.get<String>(THREAD_ID)))
    private var retryJob: Job? = null
    private val writeState = MutableStateFlow(ChatWriteUiState())
    val state = combine(updates.snapshot, savedState.getStateFlow("draft", ""), writeState) { snapshot, draft, write ->
        when (snapshot) {
            NotmidContentSnapshot.Loading -> ChatThreadUiState.Loading
            NotmidContentSnapshot.Unavailable -> ChatThreadUiState.Unavailable
            is NotmidContentSnapshot.Ready -> {
                val thread = selectedThread(snapshot)
                if (thread == null) ChatThreadUiState.Unavailable else {
                    ChatThreadUiState.Ready(route.threadId, thread, thread.toMessages(), draft, write)
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatThreadUiState.Loading)

    fun onAction(action: ChatThreadAction) {
        when (action) {
            is ChatThreadAction.DraftChanged -> savedState["draft"] = action.draft
            ChatThreadAction.Retry -> retry()
            else -> handleThreadAction(action)
        }
    }

    private fun handleThreadAction(action: ChatThreadAction) {
        val snapshot = updates.snapshot.value as? NotmidContentSnapshot.Ready ?: return
        val thread = selectedThread(snapshot) ?: return
        when (action) {
            ChatThreadAction.Send -> {
                val draft = savedState.get<String>("draft").orEmpty()
                if (!thread.chatAccess.canSendMessage || draft.isBlank()) return
                dispatch(NotmidProtectedWriteRequest.SendThreadMessage(thread.id, NotmidSendThreadMessageRequest(draft.trim())), draft)
            }
            ChatThreadAction.SaveClip -> thread.clip?.id?.let { dispatch(NotmidProtectedWriteRequest.SaveClip(it)) }
            ChatThreadAction.OpenPlace -> thread.place?.id?.let {
                routeEvents.onRouteEvent(InboxRouteEvent.AttachedPlaceRequested(it))
            }
            ChatThreadAction.AcceptInvite -> if (thread.chatAccess.canAcceptInvite) {
                dispatch(NotmidProtectedWriteRequest.RespondThreadInvite(thread.id, NotmidChatInviteDecision.Accept))
            }
            ChatThreadAction.RejectInvite -> if (thread.chatAccess.canRejectInvite) {
                dispatch(NotmidProtectedWriteRequest.RespondThreadInvite(thread.id, NotmidChatInviteDecision.Reject))
            }
            else -> Unit
        }
    }

    private fun selectedThread(snapshot: NotmidContentSnapshot.Ready): InboxThreadUi? {
        val destinations = snapshot.destinations.toNotmidDestinations()
        val destination = destinations.firstOrNull { it.id == route.selectedDestinationId } ?: destinations.firstOrNull()
        return destination?.let { it.toInboxThreads().findMatchingThread(route.threadId) ?: it.fallbackThread(route.threadId) }
    }

    private fun dispatch(request: NotmidProtectedWriteRequest, sentDraft: String? = null) {
        if (writeState.value.inFlight != null) return
        val requestAuth = auth.currentState()
        writeState.value = ChatWriteUiState(inFlight = request.writeAction)
        viewModelScope.launch {
            try {
                val result = writes.execute(requestAuth, request)
                if (result == NotmidProtectedWriteResult.Busy || auth.currentState().session !== requestAuth.session) return@launch
                when (request) {
                    is NotmidProtectedWriteRequest.SendThreadMessage -> {
                        check(result is NotmidProtectedWriteResult.MessageSent)
                        if (savedState.get<String>("draft") == sentDraft) savedState["draft"] = ""
                    }
                    is NotmidProtectedWriteRequest.SaveClip -> check(result == NotmidProtectedWriteResult.Completed)
                    is NotmidProtectedWriteRequest.RespondThreadInvite -> check(result is NotmidProtectedWriteResult.ThreadUpdated)
                    else -> error("Unsupported chat write")
                }
                val notice = request.writeAction.toSuccessNotice()
                writeState.value = writeState.value.copy(notice = notice)
                notices.emit(notice.effect)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (auth.currentState().session === requestAuth.session) {
                    val notice = failure.toProtectedActionNotice(request.writeAction)
                    writeState.value = writeState.value.copy(notice = notice)
                    notices.emit(notice.effect)
                }
            } finally {
                writeState.value = writeState.value.copy(inFlight = null)
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
                // Content owner publishes the failure; do not expose raw transport messages.
            }
        }
    }

    companion object { const val THREAD_ID = "threadId" }
}
