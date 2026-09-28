package app.thdev.glassnavlab.feature.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.thdev.glassnavlab.core.designsystem.theme.NotmidColorTokens

@Composable
fun MapScreen(onBackdropColorChanged: (Color) -> Unit = {}) {
    val viewModel: MapViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    SideEffect { onBackdropColorChanged(NotmidColorTokens.WarmMist) }
    when (val current = state) {
        MapUiState.Loading -> MapLoadStatus(true) { viewModel.onAction(MapAction.Retry) }
        MapUiState.Unavailable -> MapLoadStatus(false) { viewModel.onAction(MapAction.Retry) }
        is MapUiState.Ready -> MapContent(current, viewModel::onAction)
    }
}
