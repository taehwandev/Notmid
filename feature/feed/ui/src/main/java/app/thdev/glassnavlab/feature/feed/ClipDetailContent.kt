package app.thdev.glassnavlab.feature.feed

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.thdev.glassnavlab.core.designsystem.component.NotmidButton
import app.thdev.glassnavlab.core.designsystem.component.NotmidButtonVariant
import app.thdev.glassnavlab.core.model.notmid.NotmidNavigationIcon
import app.thdev.glassnavlab.core.designsystem.theme.notmidTheme
import app.thdev.glassnavlab.feature.notmid.common.components.NotmidGlassIcon
import app.thdev.glassnavlab.feature.notmid.common.components.NotmidRouteDetailContent

@Composable
internal fun ClipDetailContent(
    state: ClipDetailUiState,
    listState: LazyListState,
    isStartingChat: Boolean,
    onAction: (ClipDetailAction) -> Unit,
) {
    when (state) {
        ClipDetailUiState.Loading -> FeedLoadStatus(isLoading = true, onRetry = { onAction(ClipDetailAction.Retry) })
        ClipDetailUiState.Unavailable -> FeedLoadStatus(isLoading = false, onRetry = { onAction(ClipDetailAction.Retry) })
        is ClipDetailUiState.Ready -> NotmidRouteDetailContent(
            routeTitle = state.clip.title,
            routeSubtitle = state.clip.description,
            routeMeta = "clips/${state.clip.id}",
            primaryClip = state.clip,
            primaryPlace = state.place,
            listState = listState,
            actions = {
                NotmidButton(
                    text = if (isStartingChat) "Starting" else "Chat",
                    onClick = { onAction(ClipDetailAction.ChatClicked) },
                    enabled = state.clip.creatorHandle.isNotBlank() && !isStartingChat,
                    variant = NotmidButtonVariant.Secondary,
                    leadingIcon = { color -> NotmidGlassIcon(NotmidNavigationIcon.Inbox, color) },
                )
            },
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ClipDetailMissingPreview() {
    notmidTheme {
        ClipDetailContent(null.toClipDetailUiState("missing"), rememberLazyListState(), false, {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ClipDetailUnavailablePreview() {
    notmidTheme {
        ClipDetailContent(ClipDetailUiState.Unavailable, rememberLazyListState(), false, {})
    }
}
