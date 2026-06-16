package app.thdev.glassnavlab

import app.thdev.glassnavlab.core.model.notmid.NotmidAuthProvider
import app.thdev.glassnavlab.core.model.notmid.NotmidCapturePublishRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidChatInviteDecision
import app.thdev.glassnavlab.core.model.notmid.NotmidProfileSettingsUpdateRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidSendThreadMessageRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidStartThreadRequest

internal sealed interface NotmidAppAction {
    data object ReloadContent : NotmidAppAction

    data class ContinueAuth(
        val provider: NotmidAuthProvider,
    ) : NotmidAppAction

    data object BrowseSignedOut : NotmidAppAction

    data class PublishCapture(
        val request: NotmidCapturePublishRequest,
    ) : NotmidAppAction

    data class SaveClip(
        val clipId: String,
    ) : NotmidAppAction

    data class SendThreadMessage(
        val threadId: String,
        val request: NotmidSendThreadMessageRequest,
    ) : NotmidAppAction

    data class StartThread(
        val request: NotmidStartThreadRequest,
    ) : NotmidAppAction

    data class RespondThreadInvite(
        val threadId: String,
        val decision: NotmidChatInviteDecision,
    ) : NotmidAppAction

    data class UpdateProfileSettings(
        val request: NotmidProfileSettingsUpdateRequest,
    ) : NotmidAppAction
}
