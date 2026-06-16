package app.thdev.glassnavlab

import app.thdev.glassnavlab.core.data.notmid.NotmidContentSource
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteAction
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState

internal data class NotmidAppUiState(
    val contentSource: NotmidContentSource,
    val content: NotmidContentUiState = NotmidContentUiState.Loading,
    val authState: NotmidAuthState,
    val authErrorMessage: String? = null,
    val isAuthenticating: Boolean = false,
    val protectedActionInFlight: NotmidProtectedWriteAction? = null,
    val protectedActionFeedback: NotmidProtectedActionFeedback? = null,
) {
    val isPublishingCapture: Boolean
        get() = protectedActionInFlight == NotmidProtectedWriteAction.CapturePublish

    val isSavingClip: Boolean
        get() = protectedActionInFlight == NotmidProtectedWriteAction.ClipSave

    val isSendingMessage: Boolean
        get() = protectedActionInFlight == NotmidProtectedWriteAction.ChatMessage

    val isStartingChat: Boolean
        get() = protectedActionInFlight == NotmidProtectedWriteAction.ChatStart

    val isRespondingChatInvite: Boolean
        get() = protectedActionInFlight == NotmidProtectedWriteAction.ChatInviteResponse

    val isSavingProfileSettings: Boolean
        get() = protectedActionInFlight == NotmidProtectedWriteAction.ProfileSettings

    fun messageFor(action: NotmidProtectedWriteAction): String? {
        return protectedActionFeedback
            ?.takeIf { feedback -> feedback.action == action }
            ?.message
    }
}
