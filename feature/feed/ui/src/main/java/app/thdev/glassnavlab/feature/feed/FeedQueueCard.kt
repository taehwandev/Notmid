package app.thdev.glassnavlab.feature.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.glassnavlab.core.designsystem.component.NotmidGlassSurface
import app.thdev.glassnavlab.core.designsystem.component.NotmidMetricTile
import app.thdev.glassnavlab.core.designsystem.component.NotmidPillButton
import app.thdev.glassnavlab.core.designsystem.component.NotmidText
import app.thdev.glassnavlab.core.designsystem.component.NotmidTextVariant
import app.thdev.glassnavlab.core.designsystem.theme.NotmidColorTokens
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme

@Composable
internal fun FeedQueueCard(
    clip: FeedClipUi,
    place: FeedPlaceUi?,
    onClipSelected: (String) -> Unit,
) {
    NotmidGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClipSelected(clip.id) },
        shape = NotmidTheme.shapes.cardLarge,
        backgroundColor = NotmidTheme.colors.glassLight,
        contentPadding = PaddingValues(NotmidTheme.spacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FeedQueueThumbnail(clip = clip)
            FeedQueueCopy(
                clip = clip,
                place = place,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun FeedQueueThumbnail(
    clip: FeedClipUi,
) {
    val colors = clip.palette.takeIf(List<Color>::isNotEmpty)
        ?: listOf(NotmidColorTokens.WarmClip, NotmidColorTokens.RouteBlue)
    Box(
        modifier = Modifier
            .width(96.dp)
            .height(132.dp)
            .clip(NotmidTheme.shapes.card)
            .background(Brush.linearGradient(colors)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(clip.progress.coerceIn(0f, 1f))
                .height(4.dp)
                .align(Alignment.BottomStart)
                .background(Color.White.copy(alpha = 0.82f)),
        )
        NotmidText(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(NotmidTheme.spacing.sm),
            text = clip.qualityLabel,
            color = Color.White,
            variant = NotmidTextVariant.Caption,
        )
    }
}

@Composable
private fun FeedQueueCopy(
    clip: FeedClipUi,
    place: FeedPlaceUi?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.sm),
    ) {
        NotmidText(
            text = clip.title,
            variant = NotmidTextVariant.Headline,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        NotmidText(
            text = clip.caption,
            color = NotmidTheme.colors.contentMuted,
            variant = NotmidTextVariant.BodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.xs),
        ) {
            NotmidPillButton(
                label = clip.badgeLabel,
                selected = true,
                onClick = {},
            )
            NotmidPillButton(
                label = clip.capturedAtLabel,
                selected = false,
                onClick = {},
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.sm)) {
            NotmidMetricTile(label = "place", value = place?.metric ?: "clip")
            NotmidMetricTile(label = "saves", value = clip.saveCountLabel)
        }
    }
}
