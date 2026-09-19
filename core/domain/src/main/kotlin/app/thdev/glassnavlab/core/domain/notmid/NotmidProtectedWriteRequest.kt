package app.thdev.glassnavlab.core.domain.notmid

import app.thdev.glassnavlab.core.model.notmid.NotmidCapturePublishRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidChatInviteDecision
import app.thdev.glassnavlab.core.model.notmid.NotmidProfileSettingsUpdateRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidSendThreadMessageRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidStartThreadRequest

sealed interface NotmidProtectedWriteRequest {
    val writeAction: NotmidProtectedWriteAction

    data class PublishCapture(
        val request: NotmidCapturePublishRequest,
    ) : NotmidProtectedWriteRequest {
        override val writeAction = NotmidProtectedWriteAction.CapturePublish
    }

    data class SaveClip(
        val clipId: String,
    ) : NotmidProtectedWriteRequest {
        override val writeAction = NotmidProtectedWriteAction.ClipSave
    }

    data class SendThreadMessage(
        val threadId: String,
        val request: NotmidSendThreadMessageRequest,
    ) : NotmidProtectedWriteRequest {
        override val writeAction = NotmidProtectedWriteAction.ChatMessage
    }

    data class StartThread(
        val request: NotmidStartThreadRequest,
    ) : NotmidProtectedWriteRequest {
        override val writeAction = NotmidProtectedWriteAction.ChatStart
    }

    data class RespondThreadInvite(
        val threadId: String,
        val decision: NotmidChatInviteDecision,
    ) : NotmidProtectedWriteRequest {
        override val writeAction = NotmidProtectedWriteAction.ChatInviteResponse
    }

    data class UpdateProfileSettings(
        val request: NotmidProfileSettingsUpdateRequest,
    ) : NotmidProtectedWriteRequest {
        override val writeAction = NotmidProtectedWriteAction.ProfileSettings
    }
}
