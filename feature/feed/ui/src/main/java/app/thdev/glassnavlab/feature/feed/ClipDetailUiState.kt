package app.thdev.glassnavlab.feature.feed

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.thdev.glassnavlab.core.designsystem.theme.NotmidColorTokens
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidBadge
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidClip
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidDestination
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidPlace

@Immutable
internal sealed interface ClipDetailUiState {
    data object Loading : ClipDetailUiState
    data object Unavailable : ClipDetailUiState

    @Immutable
    data class Ready(
        val clip: NotmidClip,
        val place: NotmidPlace,
        val backdropPalettes: List<List<Color>>,
        val isStartingChat: Boolean = false,
    ) : ClipDetailUiState
}

internal fun NotmidDestination?.toClipDetailUiState(clipId: String): ClipDetailUiState.Ready {
    val clip = this?.clips?.firstOrNull { it.id == clipId } ?: NotmidClip(
        id = clipId,
        title = "Clip",
        description = "This clip route is valid, but the loaded content has no matching item.",
        badge = NotmidBadge.Label("missing"),
        palette = listOf(NotmidColorTokens.Ink, NotmidColorTokens.Subtle, NotmidColorTokens.Mist),
    )
    val place = clip.placeId?.let { placeId -> this?.places?.firstOrNull { it.id == placeId } }
        ?: this?.places?.firstOrNull()
        ?: NotmidPlace(
            id = "clip-$clipId-place",
            title = "Linked place",
            description = "The loaded content has no place attached to this clip.",
            metric = "clip",
            palette = clip.palette,
            height = 176.dp,
            contentColor = NotmidColorTokens.Cloud,
        )
    return ClipDetailUiState.Ready(
        clip = clip,
        place = place,
        // Preserve the shell's existing destination-based backdrop sampling.
        backdropPalettes = listOf(emptyList<Color>()) +
            this?.clips.orEmpty().map { it.palette } + this?.places.orEmpty().map { it.palette },
    )
}
