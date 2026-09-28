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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.glassnavlab.core.designsystem.component.NotmidButton
import app.thdev.glassnavlab.core.designsystem.component.NotmidGlassSurface
import app.thdev.glassnavlab.core.designsystem.component.NotmidText
import app.thdev.glassnavlab.core.designsystem.component.NotmidTextVariant
import app.thdev.glassnavlab.core.designsystem.theme.NotmidColorTokens
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme
import app.thdev.glassnavlab.feature.map.MapPinUi
import app.thdev.glassnavlab.feature.map.R

@Composable
internal fun PlacePreviewSheet(
    pin: MapPinUi?,
    modifier: Modifier = Modifier,
    onOpenPlace: () -> Unit,
) {
    NotmidGlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = NotmidTheme.shapes.sheet,
        backgroundColor = NotmidTheme.colors.glassLightStrong,
        contentPadding = PaddingValues(NotmidTheme.spacing.md),
    ) {
        if (pin == null) {
            NotmidText(
                text = stringResource(R.string.map_empty_filter),
                color = NotmidTheme.colors.contentMuted,
            )
            return@NotmidGlassSurface
        }

        val description = if (pin.place.description.isBlank()) {
            stringResource(R.string.map_preview_description_fallback)
        } else {
            pin.place.description
        }

        Column(verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
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
                        text = description,
                        color = NotmidTheme.colors.contentMuted,
                        variant = NotmidTextVariant.BodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            NotmidText(
                text = listOfNotNull(
                    pin.category,
                    pin.place.metric.takeIf(String::isNotBlank),
                ).joinToString(" · "),
                color = NotmidTheme.colors.contentMuted,
                variant = NotmidTextVariant.Caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            NotmidButton(
                text = stringResource(R.string.map_open_place),
                onClick = onOpenPlace,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
