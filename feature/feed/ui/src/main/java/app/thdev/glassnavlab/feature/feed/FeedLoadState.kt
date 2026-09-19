package app.thdev.glassnavlab.feature.feed

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface FeedLoadState {
    data object Loading : FeedLoadState
    data class Ready(val content: FeedUiState) : FeedLoadState
    data object Unavailable : FeedLoadState
}
