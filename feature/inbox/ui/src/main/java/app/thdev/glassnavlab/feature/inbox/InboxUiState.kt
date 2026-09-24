package app.thdev.glassnavlab.feature.inbox

import androidx.compose.runtime.Immutable

internal val InboxFilters = listOf("All", "Unread", "Clips", "Places")

@Immutable
internal sealed interface InboxUiState {
    data object Loading : InboxUiState
    data object Unavailable : InboxUiState
    data class Ready(
        val destinationId: String,
        val title: String,
        val threads: List<InboxThreadUi>,
        val selectedFilter: String,
        val visibleThreads: List<InboxThreadUi>,
    ) : InboxUiState
}
