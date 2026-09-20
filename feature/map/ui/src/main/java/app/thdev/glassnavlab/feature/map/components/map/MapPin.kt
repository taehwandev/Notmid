package app.thdev.glassnavlab.feature.map.components.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.thdev.glassnavlab.core.designsystem.component.NotmidGlassSurface
import app.thdev.glassnavlab.core.designsystem.component.NotmidText
import app.thdev.glassnavlab.core.designsystem.component.NotmidTextVariant
import app.thdev.glassnavlab.core.designsystem.theme.NotmidColorTokens
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme

import app.thdev.glassnavlab.feature.map.MapPinUi

@Composable
internal fun MapPin(
    pin: MapPinUi,
    selected: Boolean,
    index: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val pinSize = if (selected) 76.dp else 62.dp
    val palette = pin.place.palette.ifEmpty {
        listOf(NotmidColorTokens.Ink, NotmidColorTokens.RouteBlue)
    }
    Box(
        modifier = modifier
            .size(pinSize)
            .clip(NotmidTheme.shapes.pill)
            .background(
                brush = Brush.linearGradient(palette),
                shape = NotmidTheme.shapes.pill,
            )
            .clickable(onClick = onClick)
            .padding(5.dp),
        contentAlignment = Alignment.Center,
    ) {
        NotmidGlassSurface(
            shape = NotmidTheme.shapes.pill,
            backgroundColor = if (selected) {
                Color.White.copy(alpha = 0.44f)
            } else {
                Color.Black.copy(alpha = 0.22f)
            },
            borderColor = Color.White.copy(alpha = 0.34f),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 7.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                NotmidText(
                    text = "${index + 1}",
                    color = Color.White,
                    variant = NotmidTextVariant.Label,
                    maxLines = 1,
                )
                if (selected) {
                    NotmidText(
                        text = pin.category,
                        color = Color.White.copy(alpha = 0.86f),
                        variant = NotmidTextVariant.Caption,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
