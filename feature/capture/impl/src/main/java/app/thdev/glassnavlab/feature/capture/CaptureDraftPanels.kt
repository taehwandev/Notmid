package app.thdev.glassnavlab.feature.capture

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.glassnavlab.core.designsystem.component.NotmidButton
import app.thdev.glassnavlab.core.designsystem.component.NotmidButtonVariant
import app.thdev.glassnavlab.core.designsystem.component.NotmidGlassSurface
import app.thdev.glassnavlab.core.designsystem.component.NotmidOutlinedButton
import app.thdev.glassnavlab.core.designsystem.component.NotmidPillButton
import app.thdev.glassnavlab.core.designsystem.component.NotmidSelectionRow
import app.thdev.glassnavlab.core.designsystem.component.NotmidSwitch
import app.thdev.glassnavlab.core.designsystem.component.NotmidText
import app.thdev.glassnavlab.core.designsystem.component.NotmidTextField
import app.thdev.glassnavlab.core.designsystem.component.NotmidTextVariant
import app.thdev.glassnavlab.core.designsystem.theme.NotmidColorTokens
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidCaptureDraft
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidPlace

private val CaptureMoodTags = listOf(
    "line proof",
    "worth it",
    "good light",
    "quiet seats",
    "price check",
    "date safe",
)

@Composable
internal fun CaptureComposerPanel(
    draft: NotmidCaptureDraft?,
    caption: String,
    onCaptionChange: (String) -> Unit,
    selectedTags: Set<String>,
    onTagSelected: (String) -> Unit,
) {
    NotmidGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = NotmidTheme.colors.glassLightStrong,
        contentPadding = PaddingValues(NotmidTheme.spacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.lg)) {
            NotmidText(
                text = "Receipt details",
                variant = NotmidTextVariant.Headline,
            )
            NotmidTextField(
                value = caption,
                onValueChange = onCaptionChange,
                label = "Caption",
                placeholder = "What did the place actually look like?",
                supportingText = "${caption.length}/140",
                minLines = 3,
                maxLines = 4,
            )
            Column(verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.sm)) {
                NotmidText(
                    text = "Mood tags",
                    variant = NotmidTextVariant.Label,
                    color = NotmidTheme.colors.contentMuted,
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.sm),
                ) {
                    items(CaptureMoodTags, key = { it }) { tag ->
                        NotmidPillButton(
                            label = tag,
                            selected = tag in selectedTags,
                            onClick = { onTagSelected(tag) },
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.sm)) {
                CaptureFactTile(
                    label = "wait",
                    value = draft?.waitTimeLabel ?: "-",
                    modifier = Modifier.weight(1f),
                )
                CaptureFactTile(
                    label = "crowd",
                    value = draft?.crowdLabel ?: "-",
                    modifier = Modifier.weight(1f),
                )
                CaptureFactTile(
                    label = "price",
                    value = draft?.priceTierLabel ?: "-",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
internal fun CapturePlaceAttachment(place: NotmidPlace?) {
    NotmidGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = NotmidTheme.colors.glassLight,
        contentPadding = PaddingValues(NotmidTheme.spacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.md)) {
            NotmidText(
                text = "Attached place",
                variant = NotmidTextVariant.Headline,
            )
            if (place == null) {
                NotmidText(
                    text = "No local fake place is available for this draft.",
                    color = NotmidTheme.colors.contentMuted,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val placePalette = place.palette.ifEmpty {
                        listOf(NotmidColorTokens.Ink, NotmidColorTokens.RouteBlue)
                    }
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(
                                brush = Brush.linearGradient(placePalette),
                                shape = NotmidTheme.shapes.card,
                            ),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.xxs),
                    ) {
                        NotmidText(
                            text = place.title,
                            variant = NotmidTextVariant.Label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        NotmidText(
                            text = place.description,
                            variant = NotmidTextVariant.Caption,
                            color = NotmidTheme.colors.contentMuted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    NotmidText(
                        text = place.metric,
                        variant = NotmidTextVariant.Label,
                        color = NotmidTheme.colors.route,
                    )
                }
            }
        }
    }
}

@Composable
internal fun CapturePublishPanel(
    readyToPublish: Boolean,
    isPublishing: Boolean,
    publicReceipt: Boolean,
    onPublicReceiptChange: (Boolean) -> Unit,
    draftStatus: String,
    onSaveDraft: () -> Unit,
    onPublish: () -> Unit,
) {
    NotmidGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = NotmidTheme.colors.glassLightStrong,
        contentPadding = PaddingValues(NotmidTheme.spacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.md)) {
            NotmidSelectionRow(
                title = "Public receipt",
                subtitle = "Public clips can appear in Feed, Map, and place pages.",
                trailing = {
                    NotmidSwitch(
                        checked = publicReceipt,
                        onCheckedChange = onPublicReceiptChange,
                    )
                },
            )
            NotmidText(
                text = draftStatus,
                variant = NotmidTextVariant.Caption,
                color = if (readyToPublish) NotmidTheme.colors.signal else NotmidTheme.colors.contentMuted,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.sm)) {
                NotmidOutlinedButton(
                    text = "Save draft",
                    onClick = onSaveDraft,
                    modifier = Modifier.weight(1f),
                )
                NotmidButton(
                    text = if (isPublishing) "Publishing" else "Publish",
                    onClick = onPublish,
                    modifier = Modifier.weight(1f),
                    enabled = readyToPublish && !isPublishing,
                    variant = NotmidButtonVariant.Secondary,
                )
            }
        }
    }
}

@Composable
private fun CaptureFactTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    NotmidGlassSurface(
        modifier = modifier,
        shape = NotmidTheme.shapes.card,
        backgroundColor = NotmidTheme.colors.glassLight,
        contentPadding = PaddingValues(NotmidTheme.spacing.md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.xxs)) {
            NotmidText(
                text = label,
                variant = NotmidTextVariant.Caption,
                color = NotmidTheme.colors.contentMuted,
                maxLines = 1,
            )
            NotmidText(
                text = value,
                variant = NotmidTextVariant.Label,
                maxLines = 1,
            )
        }
    }
}
