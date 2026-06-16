package app.thdev.glassnavlab.feature.notmid

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidBackgroundColor
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidDestination
import app.thdev.glassnavlab.feature.notmid.common.model.backdropPaletteForItem
import app.thdev.glassnavlab.feature.notmid.common.model.notmidPalette

@Composable
internal fun rememberNavigationBackdropColor(
    listState: LazyListState,
    destination: NotmidDestination,
): State<Color> {
    val density = LocalDensity.current
    val sampleOffsetFromBottom = with(density) {
        NotmidTheme.spacing.bottomNavigationSampleOffset.roundToPx()
    }

    return remember(listState, destination.id, sampleOffsetFromBottom) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val sampleY = layoutInfo.viewportEndOffset - sampleOffsetFromBottom
            val visibleCard = layoutInfo.visibleItemsInfo.firstOrNull { item ->
                item.index > 0 && sampleY >= item.offset && sampleY <= item.offset + item.size
            } ?: return@derivedStateOf NotmidBackgroundColor
            val palette = backdropPaletteForItem(
                destination = destination,
                itemIndex = visibleCard.index,
            ) ?: return@derivedStateOf NotmidBackgroundColor
            val localFraction = ((sampleY - visibleCard.offset).toFloat() / visibleCard.size)
                .coerceIn(0f, 1f)

            notmidPalette(
                palette = palette,
                fraction = localFraction,
            )
        }
    }
}
