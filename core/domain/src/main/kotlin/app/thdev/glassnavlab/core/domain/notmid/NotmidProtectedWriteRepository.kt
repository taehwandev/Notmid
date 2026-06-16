package app.thdev.glassnavlab.core.domain.notmid

import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState
import app.thdev.glassnavlab.core.model.notmid.NotmidChatInviteDecision
import app.thdev.glassnavlab.core.model.notmid.NotmidChatInviteResponseReceipt
import app.thdev.glassnavlab.core.model.notmid.NotmidCapturePublishReceipt
import app.thdev.glassnavlab.core.model.notmid.NotmidCapturePublishRequest
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

enum class NotmidProtectedWriteAction {
    CapturePublish,
    ClipSave,
    ChatStart,
    ChatMessage,
    ChatInviteResponse,
    ProfileSettings,
}

sealed class NotmidProtectedWriteFailure(
    val action: NotmidProtectedWriteAction,
) {
    class MissingAuth(
        action: NotmidProtectedWriteAction,
    ) : NotmidProtectedWriteFailure(action)

    class InvalidRequest(
        action: NotmidProtectedWriteAction,
        val code: String,
        val message: String,
    ) : NotmidProtectedWriteFailure(action)

    class HttpStatus(
        action: NotmidProtectedWriteAction,
        val path: String,
        val statusCode: Int,
        val body: String,
    ) : NotmidProtectedWriteFailure(action)

    class Network(
        action: NotmidProtectedWriteAction,
        val path: String,
        val code: String,
        val message: String,
        val causeName: String?,
    ) : NotmidProtectedWriteFailure(action)

    class MalformedResponse(
        action: NotmidProtectedWriteAction,
        val path: String,
        val causeName: String?,
    ) : NotmidProtectedWriteFailure(action)
}

class NotmidProtectedWriteException(
    val failure: NotmidProtectedWriteFailure,
) : IllegalStateException(failure.toExceptionMessage())

private fun NotmidProtectedWriteFailure.toExceptionMessage(): String {
    return when (this) {
        is NotmidProtectedWriteFailure.MissingAuth -> {
            "notmid protected write requires auth for $action."
        }

        is NotmidProtectedWriteFailure.InvalidRequest -> {
            "notmid protected write request is invalid for $action: $message"
        }

        is NotmidProtectedWriteFailure.HttpStatus -> {
            "notmid protected write failed for $path with HTTP $statusCode."
        }

        is NotmidProtectedWriteFailure.Network -> {
            "notmid protected write network request failed for $path: $message"
        }

        is NotmidProtectedWriteFailure.MalformedResponse -> {
            "notmid protected write response did not match the Android contract for $path."
        }
    }
}
