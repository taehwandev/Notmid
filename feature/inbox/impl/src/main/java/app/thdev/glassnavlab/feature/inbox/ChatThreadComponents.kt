package app.thdev.glassnavlab.feature.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.glassnavlab.core.designsystem.component.NotmidButton
import app.thdev.glassnavlab.core.designsystem.component.NotmidButtonVariant
import app.thdev.glassnavlab.core.designsystem.component.NotmidGlassSurface
import app.thdev.glassnavlab.core.designsystem.component.NotmidOutlinedButton
import app.thdev.glassnavlab.core.designsystem.component.NotmidPillButton
import app.thdev.glassnavlab.core.designsystem.component.NotmidText
import app.thdev.glassnavlab.core.designsystem.component.NotmidTextField
import app.thdev.glassnavlab.core.designsystem.component.NotmidTextVariant
import app.thdev.glassnavlab.core.designsystem.theme.NotmidColorTokens
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidChatInviteStatus

@Composable
internal fun ChatContextPanel(
    thread: InboxThreadUi,
    isSavingClip: Boolean = false,
    isRespondingChatInvite: Boolean = false,
    statusMessage: String? = null,
    onSaveClip: (String) -> Unit = {},
    onOpenPlace: (String) -> Unit = {},
    onAcceptInvite: (String) -> Unit = {},
    onRejectInvite: (String) -> Unit = {},
) {
    NotmidGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = NotmidTheme.shapes.sheet,
        backgroundColor = NotmidTheme.colors.glassLightStrong,
        contentPadding = PaddingValues(NotmidTheme.spacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.md)) {
            NotmidText(
                text = "Shared context",
                variant = NotmidTextVariant.Headline,
            )
            ThreadAttachmentPreview(thread = thread)
            ChatAccessStatus(
                thread = thread,
                isResponding = isRespondingChatInvite,
                onAcceptInvite = onAcceptInvite,
                onRejectInvite = onRejectInvite,
            )
            statusMessage?.let { message ->
                NotmidText(
                    text = message,
                    variant = NotmidTextVariant.Caption,
                    color = NotmidTheme.colors.contentMuted,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.sm)) {
                NotmidOutlinedButton(
                    text = if (isSavingClip) "Saving" else "Save clip",
                    onClick = { thread.clip?.id?.let(onSaveClip) },
                    modifier = Modifier.weight(1f),
                    enabled = thread.clip != null && !isSavingClip,
                )
                NotmidButton(
                    text = "Open route",
                    onClick = { thread.place?.id?.let(onOpenPlace) },
                    modifier = Modifier.weight(1f),
                    variant = NotmidButtonVariant.Secondary,
                    enabled = thread.place != null,
                )
            }
        }
    }
}

@Composable
private fun ChatAccessStatus(
    thread: InboxThreadUi,
    isResponding: Boolean,
    onAcceptInvite: (String) -> Unit,
    onRejectInvite: (String) -> Unit,
) {
    val access = thread.chatAccess
    val title = when (access.inviteStatus) {
        NotmidChatInviteStatus.Accepted -> {
            if (access.canSendMessage) "Chat available" else "Chat unavailable"
        }

        NotmidChatInviteStatus.PendingInbound -> "Chat request"
        NotmidChatInviteStatus.PendingOutbound -> "Waiting for acceptance"
        NotmidChatInviteStatus.Rejected -> "Request rejected"
    }

    Column(verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.xs)) {
        NotmidText(
            text = title,
            variant = NotmidTextVariant.Label,
            color = NotmidTheme.colors.content,
        )
        NotmidText(
            text = access.reasonLabel,
            variant = NotmidTextVariant.Caption,
            color = NotmidTheme.colors.contentMuted,
        )
        if (access.canAcceptInvite || access.canRejectInvite) {
            Row(horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.sm)) {
                NotmidButton(
                    text = if (isResponding) "Accepting" else "Accept",
                    onClick = { onAcceptInvite(thread.id) },
                    modifier = Modifier.weight(1f),
                    variant = NotmidButtonVariant.Secondary,
                    enabled = access.canAcceptInvite && !isResponding,
                )
                NotmidOutlinedButton(
                    text = if (isResponding) "Rejecting" else "Reject",
                    onClick = { onRejectInvite(thread.id) },
                    modifier = Modifier.weight(1f),
                    enabled = access.canRejectInvite && !isResponding,
                )
            }
        }
    }
}

@Composable
internal fun ChatMessageBubble(
    message: ChatMessageUi,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.mine) Arrangement.End else Arrangement.Start,
    ) {
        NotmidGlassSurface(
            modifier = Modifier.fillMaxWidth(0.84f),
            shape = NotmidTheme.shapes.cardLarge,
            backgroundColor = if (message.mine) {
                NotmidTheme.colors.surfaceInverse
            } else {
                NotmidTheme.colors.glassLightStrong
            },
            borderColor = if (message.mine) {
                Color.Transparent
            } else {
                NotmidTheme.colors.glassStroke
            },
            contentPadding = PaddingValues(NotmidTheme.spacing.lg),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.sm)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NotmidText(
                        text = message.sender,
                        variant = NotmidTextVariant.Label,
                        color = if (message.mine) {
                            Color.White.copy(alpha = 0.78f)
                        } else {
                            NotmidTheme.colors.contentMuted
                        },
                        maxLines = 1,
                    )
                    NotmidText(
                        text = message.timestamp,
                        variant = NotmidTextVariant.Caption,
                        color = if (message.mine) {
                            Color.White.copy(alpha = 0.62f)
                        } else {
                            NotmidTheme.colors.contentSubtle
                        },
                        maxLines = 1,
                    )
                }
                NotmidText(
                    text = message.body,
                    variant = NotmidTextVariant.Body,
                    color = if (message.mine) Color.White else NotmidTheme.colors.content,
                )
                message.attachment?.let { attachment ->
                    ChatAttachmentPreview(attachment = attachment, mine = message.mine)
                }
            }
        }
    }
}

@Composable
private fun ChatAttachmentPreview(
    attachment: ChatAttachmentUi,
    mine: Boolean,
) {
    val label: String
    val title: String
    val body: String
    val palette: List<Color>
    when (attachment) {
        is ChatAttachmentUi.Clip -> {
            label = attachment.clip.badge.labelText().ifBlank { "clip" }
            title = attachment.clip.title
            body = attachment.clip.description
            palette = attachment.clip.palette
        }

        is ChatAttachmentUi.Place -> {
            label = attachment.place.metric
            title = attachment.place.title
            body = attachment.place.description
            palette = attachment.place.palette
        }

        is ChatAttachmentUi.RoutePlan -> {
            label = "route"
            title = attachment.title
            body = attachment.description
            palette = listOf(NotmidColorTokens.RouteBlue, NotmidColorTokens.SignalGreen)
        }
    }

    val foreground = if (mine) Color.White else NotmidTheme.colors.content
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (mine) Color.White.copy(alpha = 0.12f) else NotmidTheme.colors.surfaceRaised,
                shape = NotmidTheme.shapes.card,
            )
            .padding(NotmidTheme.spacing.md),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        brush = Brush.linearGradient(
                            palette.ifEmpty {
                                listOf(NotmidColorTokens.Ink, NotmidColorTokens.RouteBlue)
                            },
                        ),
                        shape = NotmidTheme.shapes.card,
                    ),
            )
            Column(verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.xxs)) {
                NotmidText(
                    text = label,
                    variant = NotmidTextVariant.Caption,
                    color = foreground.copy(alpha = 0.68f),
                    maxLines = 1,
                )
                NotmidText(
                    text = title,
                    variant = NotmidTextVariant.Label,
                    color = foreground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                NotmidText(
                    text = body,
                    variant = NotmidTextVariant.Caption,
                    color = foreground.copy(alpha = 0.70f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
internal fun ChatComposer(
    draft: String,
    onDraftChange: (String) -> Unit,
    enabled: Boolean = true,
    isSending: Boolean = false,
    statusMessage: String? = null,
    disabledMessage: String? = null,
    onSend: (String) -> Unit,
) {
    NotmidGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = NotmidTheme.shapes.sheet,
        backgroundColor = NotmidTheme.colors.glassLightStrong,
        contentPadding = PaddingValues(NotmidTheme.spacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.md)) {
            NotmidTextField(
                value = draft,
                onValueChange = onDraftChange,
                enabled = enabled && !isSending,
                label = "Message",
                placeholder = "Ask for timing, route, or another receipt",
                minLines = 2,
                maxLines = 4,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.sm)) {
                items(listOf("Clip", "Place", "Route"), key = { it }) { action ->
                    NotmidPillButton(
                        label = action,
                        enabled = enabled && !isSending,
                        onClick = {},
                    )
                }
            }
            statusMessage?.let { message ->
                NotmidText(
                    text = message,
                    variant = NotmidTextVariant.Caption,
                    color = NotmidTheme.colors.contentMuted,
                )
            }
            if (!enabled && !disabledMessage.isNullOrBlank()) {
                NotmidText(
                    text = disabledMessage,
                    variant = NotmidTextVariant.Caption,
                    color = NotmidTheme.colors.contentMuted,
                )
            }
            NotmidButton(
                text = if (isSending) "Sending" else "Send",
                onClick = { onSend(draft.trim()) },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled && draft.isNotBlank() && !isSending,
                variant = NotmidButtonVariant.Secondary,
            )
        }
    }
}
