package app.thdev.glassnavlab.feature.map

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.thdev.glassnavlab.core.designsystem.theme.NotmidColorTokens
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidBadge
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidClip
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidDestination
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidPlace

@Immutable
internal sealed interface PlaceDetailUiState {
    data object Loading : PlaceDetailUiState
    data object Unavailable : PlaceDetailUiState

    @Immutable
    data class Ready(
        val place: NotmidPlace,
        val clip: NotmidClip,
        val backdropPalettes: List<List<Color>>,
    ) : PlaceDetailUiState
}

internal fun NotmidDestination?.toPlaceDetailUiState(placeId: String): PlaceDetailUiState.Ready {
    val place = this?.places?.firstOrNull { it.id == placeId } ?: NotmidPlace(
        id = placeId,
        title = "Place",
        description = "This place route is valid, but the loaded content has no matching item.",
        metric = "missing",
        palette = listOf(NotmidColorTokens.Ink, NotmidColorTokens.Subtle, NotmidColorTokens.Mist),
        height = 176.dp,
        contentColor = NotmidColorTokens.Cloud,
    )
    val clip = this?.clips?.firstOrNull { it.placeId == placeId }
        ?: this?.clips?.firstOrNull()
        ?: NotmidClip(
            id = "place-$placeId-clip",
            title = "Recent proof",
            description = "The loaded content has no proof clip attached to this place.",
            badge = NotmidBadge.Label("place"),
            palette = place.palette,
        )
    return PlaceDetailUiState.Ready(
        place = place,
        clip = clip,
        // Preserve the shell's destination-based backdrop sampling.
        backdropPalettes = listOf(emptyList<Color>()) +
            this?.clips.orEmpty().map { it.palette } + this?.places.orEmpty().map { it.palette },
    )
}
