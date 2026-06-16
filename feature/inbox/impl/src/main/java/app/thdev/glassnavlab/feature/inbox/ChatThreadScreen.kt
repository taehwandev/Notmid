package app.thdev.glassnavlab.feature.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.thdev.glassnavlab.core.designsystem.component.NotmidSectionHeader
import app.thdev.glassnavlab.core.designsystem.theme.NotmidColorTokens
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme
import app.thdev.glassnavlab.feature.inbox.api.route.ChatThreadRoute
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidDestination

@Composable
fun ChatThreadScreen(
    destination: NotmidDestination,
    route: ChatThreadRoute,
    listState: LazyListState,
    isSavingClip: Boolean = false,
    isSendingMessage: Boolean = false,
    isRespondingChatInvite: Boolean = false,
    clipSaveMessage: String? = null,
    chatMessage: String? = null,
    onSaveClip: (String) -> Unit = {},
    onOpenPlace: (String) -> Unit = {},
    onAcceptInvite: (String) -> Unit = {},
    onRejectInvite: (String) -> Unit = {},
    onSendMessage: (threadId: String, body: String) -> Unit = { _, _ -> },
) {
    val threads = remember(
        destination.id,
        destination.clips,
        destination.places,
        destination.threads,
        destination.threadMessages,
    ) {
        destination.toInboxThreads()
    }
    val thread = remember(threads, route.threadId) {
        threads.findMatchingThread(route.threadId) ?: destination.fallbackThread(route.threadId)
    }
    val messages = remember(thread.id, thread.messages) { thread.toMessages() }
    var draft by rememberSaveable(route.threadId) { mutableStateOf("") }

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
                eyebrow = "chats/${route.threadId}",
            )
        }

        item(key = "chat-context-${thread.id}") {
            ChatContextPanel(
                thread = thread,
                isSavingClip = isSavingClip,
                isRespondingChatInvite = isRespondingChatInvite,
                statusMessage = clipSaveMessage,
                onSaveClip = onSaveClip,
                onOpenPlace = onOpenPlace,
                onAcceptInvite = onAcceptInvite,
                onRejectInvite = onRejectInvite,
            )
        }

        items(
            items = messages,
            key = { message -> "${thread.id}-${message.id}" },
        ) { message ->
            ChatMessageBubble(message = message)
        }

        item(key = "chat-composer-${thread.id}") {
            ChatComposer(
                draft = draft,
                onDraftChange = { draft = it },
                enabled = thread.chatAccess.canSendMessage,
                isSending = isSendingMessage,
                statusMessage = chatMessage,
                disabledMessage = thread.chatAccess.reasonLabel,
                onSend = { body ->
                    if (thread.chatAccess.canSendMessage) {
                        onSendMessage(thread.id, body)
                        draft = ""
                    }
                },
            )
        }
    }
}
