package app.thdev.glassnavlab

import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteAction
import app.thdev.glassnavlab.core.model.notmid.NotmidCapturePublishRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidChatInviteDecision
import app.thdev.glassnavlab.core.model.notmid.NotmidProfileSettingsUpdateRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidSendThreadMessageRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidStartThreadRequest

internal sealed interface PendingNotmidProtectedAction {
    val writeAction: NotmidProtectedWriteAction

    data class PublishCapture(
        val request: NotmidCapturePublishRequest,
    ) : PendingNotmidProtectedAction {
        override val writeAction = NotmidProtectedWriteAction.CapturePublish
    }

    data class SaveClip(
        val clipId: String,
    ) : PendingNotmidProtectedAction {
        override val writeAction = NotmidProtectedWriteAction.ClipSave
    }

    data class SendThreadMessage(
        val threadId: String,
        val request: NotmidSendThreadMessageRequest,
    ) : PendingNotmidProtectedAction {
        override val writeAction = NotmidProtectedWriteAction.ChatMessage
    }

    data class StartThread(
        val request: NotmidStartThreadRequest,
    ) : PendingNotmidProtectedAction {
        override val writeAction = NotmidProtectedWriteAction.ChatStart
    }

    data class RespondThreadInvite(
        val threadId: String,
        val decision: NotmidChatInviteDecision,
    ) : PendingNotmidProtectedAction {
        override val writeAction = NotmidProtectedWriteAction.ChatInviteResponse
    }

    data class UpdateProfileSettings(
        val request: NotmidProfileSettingsUpdateRequest,
    ) : PendingNotmidProtectedAction {
        override val writeAction = NotmidProtectedWriteAction.ProfileSettings
    }
}
