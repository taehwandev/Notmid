package app.thdev.glassnavlab.feature.feed

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.glassnavlab.core.designsystem.component.NotmidGlassSurface
import app.thdev.glassnavlab.core.designsystem.component.NotmidPillButton
import app.thdev.glassnavlab.core.designsystem.component.NotmidText
import app.thdev.glassnavlab.core.designsystem.component.NotmidTextVariant
import app.thdev.glassnavlab.core.designsystem.theme.NotmidColorTokens
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme

@Composable
internal fun FeedHeroStage(
    clip: FeedClipUi,
    place: FeedPlaceUi?,
    onClipSelected: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(620.dp)
            .clip(NotmidTheme.shapes.sheet),
    ) {
        FeedMediaBackdrop(clip = clip)
        FeedPlaybackProgress(
            progress = clip.progress,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = NotmidTheme.spacing.xl, vertical = NotmidTheme.spacing.lg),
        )
        FeedProofPills(
            clip = clip,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(NotmidTheme.spacing.lg),
        )
        FeedHeroCopy(
            clip = clip,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    start = NotmidTheme.spacing.xl,
                    end = 92.dp,
                    bottom = 112.dp,
                ),
        )
        FeedRail(
            clip = clip,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = NotmidTheme.spacing.md),
            onClipSelected = onClipSelected,
        )
        FeedPlaceStrip(
            place = place,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(NotmidTheme.spacing.xl),
            onClick = { onClipSelected(clip.id) },
        )
    }
}

@Composable
private fun BoxScope.FeedMediaBackdrop(
    clip: FeedClipUi,
) {
    val colors = clip.palette.takeIf(List<Color>::isNotEmpty)
        ?: listOf(NotmidColorTokens.Ink, NotmidColorTokens.WarmClip, NotmidColorTokens.RouteBlue)

    Canvas(modifier = Modifier.matchParentSize()) {
        drawRect(
            brush = Brush.linearGradient(
                colors = colors,
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            ),
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.18f),
            radius = size.minDimension * 0.42f,
            center = Offset(size.width * 0.78f, size.height * 0.18f),
        )
        drawLine(
            color = Color.White.copy(alpha = 0.36f),
            start = Offset(size.width * 0.12f, size.height * 0.74f),
            end = Offset(size.width * 0.82f, size.height * 0.58f),
            strokeWidth = 5.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.18f),
                    Color.Black.copy(alpha = 0.76f),
                ),
                startY = size.height * 0.20f,
                endY = size.height,
            ),
        )
    }
}

@Composable
private fun FeedPlaybackProgress(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    NotmidGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp),
        shape = NotmidTheme.shapes.pill,
        backgroundColor = Color.Black.copy(alpha = 0.16f),
        borderColor = Color.White.copy(alpha = 0.20f),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(10.dp)
                    .background(Color.White.copy(alpha = 0.74f)),
            )
        }
    }
}

@Composable
private fun FeedProofPills(
    clip: FeedClipUi,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.sm),
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
}

@Composable
private fun FeedHeroCopy(
    clip: FeedClipUi,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.sm),
    ) {
        NotmidText(
            text = "@${clip.creatorHandle}",
            color = Color.White.copy(alpha = 0.82f),
            variant = NotmidTextVariant.Label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        NotmidText(
            text = clip.title,
            color = Color.White,
            variant = NotmidTextVariant.Display,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        NotmidText(
            text = clip.caption,
            color = Color.White.copy(alpha = 0.82f),
            variant = NotmidTextVariant.BodySmall,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.sm)) {
            clip.moodTags.take(MaxVisibleMoodTags).forEach { tag ->
                NotmidPillButton(
                    label = tag,
                    selected = false,
                    onClick = {},
                )
            }
        }
    }
}

private const val MaxVisibleMoodTags = 3
