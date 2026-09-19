package app.thdev.glassnavlab.feature.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
) : ViewModel() {
    private var retryJob: Job? = null

    val state = contentUpdates.snapshot.map { snapshot ->
        when (snapshot) {
            NotmidContentSnapshot.Loading -> FeedLoadState.Loading
            NotmidContentSnapshot.Unavailable -> FeedLoadState.Unavailable
            is NotmidContentSnapshot.Ready -> {
                val destinations = snapshot.destinations.toNotmidDestinations()
                val destination = destinations.firstOrNull { it.id == FeedRoute.selectedDestinationId }
                    ?: destinations.firstOrNull()
                destination?.let { FeedLoadState.Ready(it.toFeedUiState()) }
                    ?: FeedLoadState.Ready(FeedUiState("Feed", "", null, emptyList(), emptyList()))
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FeedLoadState.Loading)

    fun retry() {
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
