package app.thdev.glassnavlab.feature.inbox

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentRepository
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentSnapshot
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentUpdates
import app.thdev.glassnavlab.core.navigation.runtime.RouteEventSink
import app.thdev.glassnavlab.feature.inbox.api.event.InboxRouteEvent
import app.thdev.glassnavlab.feature.inbox.api.route.InboxRoute
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
internal class InboxViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    updates: NotmidContentUpdates,
    private val repository: NotmidContentRepository,
    private val routeEvents: RouteEventSink,
    @param:InboxIoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private var retryJob: Job? = null
    val state = combine(updates.snapshot, savedState.getStateFlow("filter", "All")) { snapshot, filter ->
        when (snapshot) {
            NotmidContentSnapshot.Loading -> InboxUiState.Loading
            NotmidContentSnapshot.Unavailable -> InboxUiState.Unavailable
            is NotmidContentSnapshot.Ready -> {
                val destinations = snapshot.destinations.toNotmidDestinations()
                val destination = destinations.firstOrNull { it.id == InboxRoute.selectedDestinationId }
                    ?: destinations.firstOrNull()
                val threads = destination?.toInboxThreads().orEmpty()
                val selectedFilter = filter.takeIf { it in InboxFilters } ?: "All"
                InboxUiState.Ready(
                    destination?.id ?: InboxRoute.selectedDestinationId,
                    destination?.title ?: "Inbox",
                    threads,
                    selectedFilter,
                    threads.filterFor(selectedFilter),
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InboxUiState.Loading)

    fun onAction(action: InboxAction) {
        when (action) {
            is InboxAction.FilterSelected -> if (action.filter in InboxFilters) savedState["filter"] = action.filter
            is InboxAction.ThreadClicked -> routeEvents.onRouteEvent(InboxRouteEvent.ChatThreadRequested(action.threadId))
            InboxAction.Retry -> retry()
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
                // The content owner publishes Unavailable; raw transport messages stay out of UI.
            }
        }
    }
}
