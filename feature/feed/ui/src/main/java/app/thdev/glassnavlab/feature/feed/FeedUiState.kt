package app.thdev.glassnavlab.feature.feed

import androidx.compose.runtime.Immutable

@Immutable
internal data class FeedUiState(
    val title: String,
    val subtitle: String,
    val heroClip: FeedClipUi?,
    val queue: List<FeedClipUi>,
    val places: List<FeedPlaceUi>,
) {
    val isEmpty: Boolean
        get() = heroClip == null && queue.isEmpty()
}
