package app.thdev.glassnavlab.core.designsystem.component.backdrop

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme
import app.thdev.glassnavlab.core.designsystem.theme.NotmidColorTokens

@Composable
fun rememberListBackdropColor(
    listState: LazyListState,
    palettes: List<List<Color>>,
): State<Color> {
    val density = LocalDensity.current
    val sampleOffsetFromBottom = with(density) {
        NotmidTheme.spacing.bottomNavigationSampleOffset.roundToPx()
    }

    return remember(listState, palettes, sampleOffsetFromBottom) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val sampleY = layoutInfo.viewportEndOffset - sampleOffsetFromBottom
            val visibleCard = layoutInfo.visibleItemsInfo.firstOrNull { item ->
                sampleY >= item.offset && sampleY <= item.offset + item.size
            } ?: return@derivedStateOf NotmidColorTokens.WarmMist
            val palette = palettes.getOrNull(visibleCard.index)
                ?: return@derivedStateOf NotmidColorTokens.WarmMist
            val localFraction = ((sampleY - visibleCard.offset).toFloat() / visibleCard.size)
                .coerceIn(0f, 1f)

            samplePalette(
                palette = palette,
                fraction = localFraction,
            )
        }
    }
}

private fun samplePalette(palette: List<Color>, fraction: Float): Color {
    if (palette.isEmpty()) return NotmidColorTokens.WarmMist
    if (palette.size == 1) return palette.first()
    val scaled = fraction.coerceIn(0f, 1f) * palette.lastIndex
    val start = scaled.toInt().coerceIn(0, palette.lastIndex - 1)
    return lerp(palette[start], palette[start + 1], scaled - start)
}
