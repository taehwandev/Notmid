package app.thdev.glassnavlab.feature.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.thdev.glassnavlab.core.designsystem.theme.notmidTheme
import app.thdev.glassnavlab.core.model.notmid.NotmidChatAccess
import app.thdev.glassnavlab.core.designsystem.component.NotmidSectionHeader
import app.thdev.glassnavlab.core.designsystem.theme.NotmidColorTokens
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme

@Composable
internal fun ChatThreadContent(
    state: ChatThreadUiState.Ready,
    listState: LazyListState,
    isSavingClip: Boolean,
    isSendingMessage: Boolean,
    isRespondingChatInvite: Boolean,
    clipSaveMessage: String?,
    chatMessage: String?,
    onAction: (ChatThreadAction) -> Unit,
) {
    val thread = state.thread
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NotmidColorTokens.WarmMist),
        state = listState,
        contentPadding = PaddingValues(
            start = NotmidTheme.spacing.screenHorizontal,
            top = NotmidTheme.spacing.screenTop,
            end = NotmidTheme.spacing.screenHorizontal,
            bottom = NotmidTheme.spacing.bottomNavigationPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.lg),
    ) {
        item(key = "chat-header-${thread.id}") {
            NotmidSectionHeader(
                title = thread.title,
                subtitle = thread.subtitle,
                eyebrow = "chats/${state.routeThreadId}",
            )
        }

        item(key = "chat-context-${thread.id}") {
            ChatContextPanel(
                thread = thread,
                isSavingClip = isSavingClip,
                isRespondingChatInvite = isRespondingChatInvite,
                statusMessage = clipSaveMessage,
                onSaveClip = { onAction(ChatThreadAction.SaveClip) },
                onOpenPlace = { onAction(ChatThreadAction.OpenPlace) },
                onAcceptInvite = { onAction(ChatThreadAction.AcceptInvite) },
                onRejectInvite = { onAction(ChatThreadAction.RejectInvite) },
            )
        }

        items(
            items = state.messages,
            key = { message -> "${thread.id}-${message.id}" },
        ) { message ->
            ChatMessageBubble(message = message)
        }

        item(key = "chat-composer-${thread.id}") {
            ChatComposer(
                draft = state.draft,
                onDraftChange = { onAction(ChatThreadAction.DraftChanged(it)) },
                enabled = thread.chatAccess.canSendMessage,
                isSending = isSendingMessage,
                statusMessage = chatMessage,
                disabledMessage = thread.chatAccess.reasonLabel,
                onSend = { onAction(ChatThreadAction.Send) },
            )
        }
    }
}

@Preview
@Composable
private fun ChatThreadDraftPreview() {
    val thread = InboxThreadUi(
        id = "preview", title = "Weekend plans", subtitle = "A shared place",
        preview = "Let's meet", participants = "You, friend", updatedLabel = "now",
        unreadCount = 0, chatAccess = NotmidChatAccess.AcceptedFriend,
        clip = null, place = null, routePlan = "Plan together",
    )
    notmidTheme {
        ChatThreadContent(
            state = ChatThreadUiState.Ready("preview", thread, emptyList(), "See you there"),
            listState = rememberLazyListState(),
            isSavingClip = false, isSendingMessage = false, isRespondingChatInvite = false,
            clipSaveMessage = null, chatMessage = null, onAction = {},
        )
    }
}
