package app.thdev.glassnavlab.feature.map.components.map

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme
import app.thdev.glassnavlab.feature.map.MapPinUi

@Composable
internal fun MapBoardSurface(
    pins: List<MapPinUi>,
    selectedPlaceId: String?,
    modifier: Modifier = Modifier,
    onPinSelected: (MapPinUi) -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        pins.forEach { pin ->
            val selected = pin.place.id == selectedPlaceId
            MapPin(
                pin = pin,
                selected = selected,
                modifier = Modifier
                    .zIndex(if (selected) 1f else 0f)
                    .offset(
                        x = pinOffset(maxWidth, pin.xFraction, selected),
                        y = pinOffset(maxHeight, pin.yFraction, selected),
                    ),
                onClick = { onPinSelected(pin) },
            )
        }

        if (maxHeight >= 160.dp) {
            MapLegend(
                hasPins = pins.isNotEmpty(),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = NotmidTheme.spacing.screenHorizontal),
            )
        }
    }
}

private fun pinOffset(axis: Dp, fraction: Float, selected: Boolean): Dp =
    (axis * fraction) - if (selected) 38.dp else 31.dp
