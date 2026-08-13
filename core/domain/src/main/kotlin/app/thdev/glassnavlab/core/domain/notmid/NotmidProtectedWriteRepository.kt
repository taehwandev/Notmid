package app.thdev.glassnavlab.core.domain.notmid

import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState
import app.thdev.glassnavlab.core.model.notmid.NotmidCapturePublishReceipt
import app.thdev.glassnavlab.core.model.notmid.NotmidCapturePublishRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidChatInviteDecision
import app.thdev.glassnavlab.core.model.notmid.NotmidChatInviteResponseReceipt
import app.thdev.glassnavlab.core.model.notmid.NotmidClipSaveReceipt
import app.thdev.glassnavlab.core.model.notmid.NotmidProfileSettingsUpdateReceipt
import app.thdev.glassnavlab.core.model.notmid.NotmidProfileSettingsUpdateRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidSendThreadMessageReceipt
import app.thdev.glassnavlab.core.model.notmid.NotmidSendThreadMessageRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidStartThreadReceipt
import app.thdev.glassnavlab.core.model.notmid.NotmidStartThreadRequest

interface NotmidProtectedWriteRepository {
    suspend fun publishCapture(
        authState: NotmidAuthState,
        request: NotmidCapturePublishRequest,
    ): NotmidCapturePublishReceipt

    suspend fun saveClip(
        authState: NotmidAuthState,
        clipId: String,
    ): NotmidClipSaveReceipt

    suspend fun sendThreadMessage(
        authState: NotmidAuthState,
        threadId: String,
        request: NotmidSendThreadMessageRequest,
    ): NotmidSendThreadMessageReceipt

    suspend fun startThread(
        authState: NotmidAuthState,
        request: NotmidStartThreadRequest,
    ): NotmidStartThreadReceipt

    suspend fun respondThreadInvite(
        authState: NotmidAuthState,
        threadId: String,
        decision: NotmidChatInviteDecision,
    ): NotmidChatInviteResponseReceipt

    suspend fun updateProfileSettings(
        authState: NotmidAuthState,
        request: NotmidProfileSettingsUpdateRequest,
    ): NotmidProfileSettingsUpdateReceipt
}
