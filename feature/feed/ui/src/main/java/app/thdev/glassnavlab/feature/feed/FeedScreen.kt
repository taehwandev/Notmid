package app.thdev.glassnavlab.feature.feed

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.thdev.glassnavlab.feature.feed.api.event.FeedRouteEvent
import app.thdev.glassnavlab.core.designsystem.component.backdrop.rememberListBackdropColor

@Composable
fun FeedScreen(
    onRouteEvent: (FeedRouteEvent) -> Unit = {},
    onBackdropColorChanged: (Color) -> Unit = {},
) {
    val viewModel: FeedViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val palettes = remember(state) {
        val content = (state as? FeedLoadState.Ready)?.content
        if (content == null) emptyList() else {
            listOf(emptyList<Color>()) + listOfNotNull(content.heroClip).map { it.palette } +
                content.queue.map { it.palette } + content.places.map { it.palette }
        }
    }
    val backdropColor by rememberListBackdropColor(listState, palettes)
    SideEffect { onBackdropColorChanged(backdropColor) }

    FeedLoadContent(
        state = state,
        listState = listState,
        onRetry = viewModel::retry,
        onClipSelected = { clipId ->
            onRouteEvent(FeedRouteEvent.ClipRequested(clipId))
        },
    )
}
