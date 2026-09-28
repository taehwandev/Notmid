package app.thdev.glassnavlab.feature.map.components.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.thdev.glassnavlab.core.designsystem.component.NotmidGlassSurface
import app.thdev.glassnavlab.core.designsystem.component.NotmidText
import app.thdev.glassnavlab.core.designsystem.component.NotmidTextVariant
import app.thdev.glassnavlab.core.designsystem.theme.NotmidColorTokens
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme

import app.thdev.glassnavlab.feature.map.MapPinUi
import app.thdev.glassnavlab.feature.map.R

@Composable
internal fun MapPin(
    pin: MapPinUi,
    selected: Boolean,
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
            .border(
                width = if (selected) 3.dp else 2.dp,
                color = if (selected) NotmidColorTokens.SignalGreen else Color.White,
                shape = NotmidTheme.shapes.pill,
            )
            .clickable(
                role = Role.Button,
                onClickLabel = stringResource(R.string.map_select_place),
                onClick = onClick,
            )
            .semantics {
                contentDescription = "${pin.place.title}, ${pin.category}"
                this.selected = selected
            }
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
            contentPadding = PaddingValues(
                horizontal = if (selected) 14.dp else 10.dp,
                vertical = if (selected) 10.dp else 8.dp,
            ),
        ) {
            NotmidText(
                text = pin.category.take(1).uppercase(),
                color = if (selected) NotmidColorTokens.Ink else Color.White,
                variant = if (selected) NotmidTextVariant.Headline else NotmidTextVariant.Label,
                maxLines = 1,
            )
        }
    }
}
