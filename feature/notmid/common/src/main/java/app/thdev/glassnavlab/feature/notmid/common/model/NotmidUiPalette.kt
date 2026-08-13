package app.thdev.glassnavlab.feature.notmid.common.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp as lerpColor
import app.thdev.glassnavlab.core.designsystem.theme.NotmidColorTokens

val NotmidBackgroundColor = NotmidColorTokens.WarmMist

fun backdropPaletteForItem(
    destination: NotmidDestination,
    itemIndex: Int,
): List<Color>? {
    val contentIndex = itemIndex - 1
    if (contentIndex < 0) return null

    return when {
        contentIndex < destination.clips.size -> destination.clips[contentIndex].palette
        else -> {
            val placeIndex = contentIndex - destination.clips.size
            destination.places.getOrNull(placeIndex)?.palette
        }
    }
}

fun notmidPalette(
    palette: List<Color>,
    fraction: Float,
): Color {
    if (palette.isEmpty()) return NotmidBackgroundColor
    if (palette.size == 1) return palette.first()

    val scaledFraction = fraction.coerceIn(0f, 1f) * palette.lastIndex
    val startIndex = scaledFraction.toInt().coerceIn(0, palette.lastIndex - 1)
    val endIndex = startIndex + 1
    return lerpColor(
        start = palette[startIndex],
        stop = palette[endIndex],
        fraction = scaledFraction - startIndex,
    )
}
