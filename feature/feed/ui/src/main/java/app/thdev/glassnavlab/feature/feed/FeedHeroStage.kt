package app.thdev.glassnavlab.feature.feed

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.glassnavlab.core.designsystem.component.NotmidButton
import app.thdev.glassnavlab.core.designsystem.component.NotmidButtonVariant
import app.thdev.glassnavlab.core.designsystem.component.NotmidText
import app.thdev.glassnavlab.core.designsystem.component.NotmidTextVariant
import app.thdev.glassnavlab.core.designsystem.theme.NotmidColorTokens
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme

@Composable
internal fun FeedHeroStage(
    clip: FeedClipUi,
    place: FeedPlaceUi?,
    onAction: (FeedAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NotmidTheme.shapes.sheet)
            .background(NotmidTheme.colors.surface),
    ) {
        FeedMediaPlaceholder(clip.palette)
        Column(
            modifier = Modifier.padding(NotmidTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                NotmidText(
                    text = clip.creatorHandle.takeIf(String::isNotBlank)?.let { "@$it" } ?: "Unknown creator",
                    variant = NotmidTextVariant.Label,
                )
                NotmidText(
                    text = clip.capturedAtLabel,
                    color = NotmidTheme.colors.contentMuted,
                    variant = NotmidTextVariant.Caption,
                )
            }
            NotmidText(
                text = clip.title,
                variant = NotmidTextVariant.Title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            NotmidText(
                text = clip.caption,
                color = NotmidTheme.colors.contentMuted,
                variant = NotmidTextVariant.BodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (clip.moodTags.isNotEmpty()) {
                NotmidText(
                    text = clip.moodTags.take(3).joinToString("  ·  "),
                    color = NotmidTheme.colors.contentMuted,
                    variant = NotmidTextVariant.Caption,
                )
            }
            FeedPlaceStrip(
                place = place,
                onClick = { place?.id?.let { onAction(FeedAction.PlaceClicked(it)) } },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.sm)) {
                NotmidButton(
                    text = "Save post",
                    onClick = { onAction(FeedAction.SaveClicked(clip.id)) },
                )
                NotmidButton(
                    text = "Post details",
                    onClick = { onAction(FeedAction.ClipClicked(clip.id)) },
                    variant = NotmidButtonVariant.Secondary,
                )
            }
        }
    }
}

@Composable
private fun FeedMediaPlaceholder(palette: List<Color>) {
    val colors = palette.takeIf(List<Color>::isNotEmpty)
        ?: listOf(NotmidColorTokens.Ink, NotmidColorTokens.WarmClip)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRect(
                brush = Brush.linearGradient(
                    colors = colors,
                    start = Offset.Zero,
                    end = Offset(size.width, size.height),
                ),
            )
        }
        NotmidText(
            text = "Video unavailable",
            color = Color.White,
            variant = NotmidTextVariant.Label,
        )
    }
}
