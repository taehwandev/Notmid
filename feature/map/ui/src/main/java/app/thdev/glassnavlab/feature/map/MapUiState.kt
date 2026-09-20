package app.thdev.glassnavlab.feature.map

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
internal sealed interface MapUiState {
    data object Loading : MapUiState
    data object Unavailable : MapUiState
    @Immutable
    data class Ready(
        val destinationId: String,
        val title: String,
        val categories: List<String>,
        val selectedCategory: String,
        val visiblePins: List<MapPinUi>,
        val selectedPin: MapPinUi?,
        val backdropPalettes: List<List<Color>>,
    ) : MapUiState
}
