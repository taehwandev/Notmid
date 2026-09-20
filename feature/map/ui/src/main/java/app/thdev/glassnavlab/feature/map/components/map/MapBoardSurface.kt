package app.thdev.glassnavlab.feature.map.components.map

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.thdev.glassnavlab.core.designsystem.component.NotmidGlassSurface
import app.thdev.glassnavlab.core.designsystem.component.NotmidText
import app.thdev.glassnavlab.core.designsystem.component.NotmidTextVariant
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme

import app.thdev.glassnavlab.feature.map.MapPinUi

@Composable
internal fun MapBoardSurface(
    pins: List<MapPinUi>,
    selectedPlaceId: String?,
    onPinSelected: (MapPinUi) -> Unit,
) {
    NotmidGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .height(520.dp),
        shape = NotmidTheme.shapes.sheet,
        backgroundColor = NotmidTheme.colors.glassLightStrong,
        contentPadding = PaddingValues(),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            FakeMapCanvas(
                modifier = Modifier.fillMaxSize(),
            )
            NotmidText(
                text = "${pins.size} live pins",
                color = NotmidTheme.colors.content.copy(alpha = 0.22f),
                variant = NotmidTextVariant.Title,
                modifier = Modifier.align(Alignment.Center),
            )
            pins.forEachIndexed { index, pin ->
                val selected = pin.place.id == selectedPlaceId
                MapPin(
                    pin = pin,
                    selected = selected,
                    index = index,
                    modifier = Modifier.offset(
                        x = pinOffset(maxWidth, pin.xFraction),
                        y = pinOffset(maxHeight, pin.yFraction),
                    ),
                    onClick = { onPinSelected(pin) },
                )
            }

            MapLegend(
                pinCount = pins.size,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(NotmidTheme.spacing.lg),
            )
        }
    }
}


private fun pinOffset(axis: Dp, fraction: Float): Dp = (axis * fraction) - 31.dp
