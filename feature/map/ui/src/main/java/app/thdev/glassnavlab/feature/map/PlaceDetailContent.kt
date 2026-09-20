package app.thdev.glassnavlab.feature.map

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.thdev.glassnavlab.core.designsystem.theme.notmidTheme
import app.thdev.glassnavlab.feature.notmid.common.components.NotmidRouteDetailContent

@Composable
internal fun PlaceDetailContent(
    state: PlaceDetailUiState,
    listState: LazyListState,
    onAction: (PlaceDetailAction) -> Unit,
) {
    when (state) {
        PlaceDetailUiState.Loading -> MapLoadStatus(isLoading = true, onRetry = { onAction(PlaceDetailAction.Retry) })
        PlaceDetailUiState.Unavailable -> MapLoadStatus(isLoading = false, onRetry = { onAction(PlaceDetailAction.Retry) })
        is PlaceDetailUiState.Ready -> NotmidRouteDetailContent(
            routeTitle = state.place.title,
            routeSubtitle = state.place.description,
            routeMeta = "places/${state.place.id}",
            primaryClip = state.clip,
            primaryPlace = state.place,
            listState = listState,
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PlaceDetailMissingPreview() {
    notmidTheme {
        PlaceDetailContent(null.toPlaceDetailUiState("missing"), rememberLazyListState(), {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PlaceDetailUnavailablePreview() {
    notmidTheme {
        PlaceDetailContent(PlaceDetailUiState.Unavailable, rememberLazyListState(), {})
    }
}
