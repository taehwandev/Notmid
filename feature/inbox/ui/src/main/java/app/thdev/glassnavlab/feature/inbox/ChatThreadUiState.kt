package app.thdev.glassnavlab.feature.inbox

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface ChatThreadUiState {
    data object Loading : ChatThreadUiState
    data object Unavailable : ChatThreadUiState
    data class Ready(
        val routeThreadId: String,
        val thread: InboxThreadUi,
        val messages: List<ChatMessageUi>,
        val draft: String,
    ) : ChatThreadUiState
}
