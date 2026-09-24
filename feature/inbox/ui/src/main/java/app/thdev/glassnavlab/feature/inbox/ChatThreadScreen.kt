package app.thdev.glassnavlab.feature.inbox

import android.os.Bundle
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import app.thdev.glassnavlab.feature.inbox.api.route.ChatThreadRoute

@Composable
fun ChatThreadScreen(
    route: ChatThreadRoute,
    isSavingClip: Boolean = false,
    isSendingMessage: Boolean = false,
    isRespondingChatInvite: Boolean = false,
    clipSaveMessage: String? = null,
    chatMessage: String? = null,
) {
    val owner = checkNotNull(LocalViewModelStoreOwner.current)
    val extras = remember(owner, route.threadId) {
        MutableCreationExtras((owner as HasDefaultViewModelProviderFactory).defaultViewModelCreationExtras).apply {
            set(DEFAULT_ARGS_KEY, Bundle().apply { putString(ChatThreadViewModel.THREAD_ID, route.threadId) })
        }
    }
    val viewModel: ChatThreadViewModel = viewModel(key = route.route, extras = extras)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    when (val current = state) {
        is ChatThreadUiState.Ready -> ChatThreadContent(
            state = current,
            listState = listState,
            isSavingClip = isSavingClip,
            isSendingMessage = isSendingMessage,
            isRespondingChatInvite = isRespondingChatInvite,
            clipSaveMessage = clipSaveMessage,
            chatMessage = chatMessage,
            onAction = viewModel::onAction,
        )
        else -> InboxLoadStatus(
            state = if (current == ChatThreadUiState.Loading) InboxUiState.Loading else InboxUiState.Unavailable,
            onAction = { viewModel.onAction(ChatThreadAction.Retry) },
        )
    }
}
