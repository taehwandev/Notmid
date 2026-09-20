package app.thdev.glassnavlab.feature.map.components.places

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.glassnavlab.core.designsystem.component.NotmidButton
import app.thdev.glassnavlab.core.designsystem.component.NotmidButtonVariant
import app.thdev.glassnavlab.core.designsystem.component.NotmidGlassSurface
import app.thdev.glassnavlab.core.designsystem.component.NotmidMetricTile
import app.thdev.glassnavlab.core.designsystem.component.NotmidOutlinedButton
import app.thdev.glassnavlab.core.designsystem.component.NotmidText
import app.thdev.glassnavlab.core.designsystem.component.NotmidTextVariant
import app.thdev.glassnavlab.core.designsystem.theme.NotmidColorTokens
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme

import app.thdev.glassnavlab.feature.map.MapPinUi

@Composable
internal fun PlacePreviewSheet(
    pin: MapPinUi?,
    visiblePinCount: Int,
    onOpenPlace: () -> Unit,
) {
    NotmidGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = NotmidTheme.shapes.sheet,
        backgroundColor = NotmidTheme.colors.glassLightStrong,
        contentPadding = PaddingValues(NotmidTheme.spacing.lg),
    ) {
        if (pin == null) {
            NotmidText(
                text = "No pins match this filter.",
                color = NotmidTheme.colors.contentMuted,
            )
            return@NotmidGlassSurface
        }

        Column(verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .background(
                            brush = Brush.linearGradient(
                                pin.place.palette.ifEmpty {
                                    listOf(NotmidColorTokens.Ink, NotmidColorTokens.RouteBlue)
                                },
                            ),
                            shape = NotmidTheme.shapes.card,
                        ),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.xxs),
                ) {
                    NotmidText(
                        text = pin.place.title,
                        variant = NotmidTextVariant.Headline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    NotmidText(
                        text = pin.place.description,
                        color = NotmidTheme.colors.contentMuted,
                        variant = NotmidTextVariant.BodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.sm)) {
                NotmidMetricTile(
                    label = "score",
                    value = pin.place.metric,
                    modifier = Modifier.weight(1f),
                )
                NotmidMetricTile(
                    label = "category",
                    value = pin.category,
                    modifier = Modifier.weight(1f),
                )
                NotmidMetricTile(
                    label = "nearby",
                    value = "$visiblePinCount",
                    modifier = Modifier.weight(1f),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.sm)) {
                NotmidOutlinedButton(
                    text = "Save later",
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
                NotmidButton(
                    text = "Open place",
                    onClick = onOpenPlace,
                    modifier = Modifier.weight(1f),
                    variant = NotmidButtonVariant.Secondary,
                )
            }
        }
    }
}
