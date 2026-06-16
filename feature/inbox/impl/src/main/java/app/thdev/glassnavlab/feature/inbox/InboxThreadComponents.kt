package app.thdev.glassnavlab.feature.inbox

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
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.glassnavlab.core.designsystem.component.NotmidGlassSurface
import app.thdev.glassnavlab.core.designsystem.component.NotmidMetricTile
import app.thdev.glassnavlab.core.designsystem.component.NotmidText
import app.thdev.glassnavlab.core.designsystem.component.NotmidTextVariant
import app.thdev.glassnavlab.core.designsystem.theme.NotmidColorTokens
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme

@Composable
internal fun InboxSummary(
    threads: List<InboxThreadUi>,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.sm)) {
        NotmidMetricTile(
            label = "threads",
            value = threads.size.toString(),
            modifier = Modifier.weight(1f),
        )
        NotmidMetricTile(
            label = "unread",
            value = threads.sumOf { it.unreadCount }.toString(),
            modifier = Modifier.weight(1f),
        )
        NotmidMetricTile(
            label = "plans",
            value = threads.count { it.place != null }.toString(),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun InboxThreadRow(
    thread: InboxThreadUi,
    onClick: () -> Unit,
) {
    NotmidGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        backgroundColor = NotmidTheme.colors.glassLightStrong,
        contentPadding = PaddingValues(NotmidTheme.spacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.md),
                verticalAlignment = Alignment.Top,
            ) {
                ThreadAvatar(thread = thread)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.xxs),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NotmidText(
                            text = thread.title,
                            variant = NotmidTextVariant.Headline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        NotmidText(
                            text = thread.updatedLabel,
                            variant = NotmidTextVariant.Caption,
                            color = NotmidTheme.colors.contentSubtle,
                            maxLines = 1,
                        )
                    }
                    NotmidText(
                        text = thread.subtitle,
                        variant = NotmidTextVariant.BodySmall,
                        color = NotmidTheme.colors.contentMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    NotmidText(
                        text = thread.preview,
                        variant = NotmidTextVariant.Body,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ThreadContextChip(
                    label = thread.participants,
                    modifier = Modifier.weight(1f),
                )
                if (thread.unreadCount > 0) {
                    ThreadUnreadBadge(count = thread.unreadCount)
                }
            }

            ThreadAttachmentPreview(thread = thread)
        }
    }
}

@Composable
private fun ThreadAvatar(
    thread: InboxThreadUi,
    modifier: Modifier = Modifier,
) {
    val palette = thread.palette()
    Box(
        modifier = modifier
            .size(58.dp)
            .background(
                brush = Brush.linearGradient(palette),
                shape = NotmidTheme.shapes.card,
            ),
        contentAlignment = Alignment.Center,
    ) {
        NotmidText(
            text = thread.title.firstOrNull()?.uppercase().orEmpty(),
            color = Color.White,
            variant = NotmidTextVariant.Title,
            maxLines = 1,
        )
    }
}

@Composable
private fun ThreadContextChip(
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(
                color = NotmidTheme.colors.surfaceRaised.copy(alpha = 0.58f),
                shape = NotmidTheme.shapes.pill,
            )
            .padding(horizontal = NotmidTheme.spacing.md, vertical = NotmidTheme.spacing.sm),
    ) {
        NotmidText(
            text = label,
            variant = NotmidTextVariant.Caption,
            color = NotmidTheme.colors.contentMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ThreadUnreadBadge(
    count: Int,
) {
    Box(
        modifier = Modifier
            .background(
                color = NotmidTheme.colors.signal,
                shape = NotmidTheme.shapes.pill,
            )
            .padding(horizontal = NotmidTheme.spacing.sm, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        NotmidText(
            text = count.toString(),
            variant = NotmidTextVariant.Caption,
            color = NotmidTheme.colors.contentOnMedia,
            maxLines = 1,
        )
    }
}

@Composable
internal fun ThreadAttachmentPreview(
    thread: InboxThreadUi,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        thread.clip?.let { clip ->
            InlineAttachmentCard(
                label = clip.badge.labelText().ifBlank { "clip" },
                title = clip.title,
                palette = clip.palette,
                modifier = Modifier.weight(1f),
            )
        }
        thread.place?.let { place ->
            InlineAttachmentCard(
                label = place.metric,
                title = place.title,
                palette = place.palette,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
internal fun InlineAttachmentCard(
    label: String,
    title: String,
    palette: List<Color>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(62.dp)
            .background(
                color = NotmidTheme.colors.surfaceRaised.copy(alpha = 0.62f),
                shape = NotmidTheme.shapes.card,
            )
            .padding(NotmidTheme.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(
                    brush = Brush.linearGradient(
                        palette.ifEmpty {
                            listOf(NotmidColorTokens.Ink, NotmidColorTokens.RouteBlue)
                        },
                    ),
                    shape = NotmidTheme.shapes.card,
                ),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.xxs),
            modifier = Modifier.weight(1f),
        ) {
            NotmidText(
                text = label,
                variant = NotmidTextVariant.Caption,
                color = NotmidTheme.colors.contentSubtle,
                maxLines = 1,
            )
            NotmidText(
                text = title,
                variant = NotmidTextVariant.Label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
