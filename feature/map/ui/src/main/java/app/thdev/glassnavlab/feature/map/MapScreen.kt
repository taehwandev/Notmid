package app.thdev.glassnavlab.feature.map

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.thdev.glassnavlab.core.designsystem.component.backdrop.rememberListBackdropColor

@Composable
fun MapScreen(onBackdropColorChanged: (Color) -> Unit = {}) {
    val viewModel: MapViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val palettes = (state as? MapUiState.Ready)?.backdropPalettes.orEmpty()
    val backdropColor by rememberListBackdropColor(listState, palettes)
    SideEffect { onBackdropColorChanged(backdropColor) }
    when (val current = state) {
        MapUiState.Loading -> MapLoadStatus(true) { viewModel.onAction(MapAction.Retry) }
        MapUiState.Unavailable -> MapLoadStatus(false) { viewModel.onAction(MapAction.Retry) }
        is MapUiState.Ready -> MapContent(current, listState, viewModel::onAction)
    }
}
