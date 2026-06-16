package app.thdev.glassnavlab.feature.feed

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.thdev.glassnavlab.feature.feed.api.event.FeedRouteEvent
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidDestination

@Composable
fun FeedScreen(
    destination: NotmidDestination,
    listState: LazyListState,
    onRouteEvent: (FeedRouteEvent) -> Unit = {},
) {
    val state = remember(destination.id, destination.clips, destination.places) {
        destination.toFeedUiState()
    }

    FeedContent(
        state = state,
        listState = listState,
        onClipSelected = { clipId ->
            onRouteEvent(FeedRouteEvent.ClipRequested(clipId))
        },
    )
}
