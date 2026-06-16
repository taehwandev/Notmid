package app.thdev.glassnavlab

import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteAction
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteException
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteFailure
import app.thdev.glassnavlab.core.notice.api.effect.NoticeEffect
import app.thdev.glassnavlab.core.notice.api.model.NoticePresentation
import app.thdev.glassnavlab.core.notice.api.model.NoticeRequest
import app.thdev.glassnavlab.core.notice.api.model.NoticeTone
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

internal data class NotmidProtectedActionNotice(
    val action: NotmidProtectedWriteAction,
    val notice: NoticeRequest,
) {
    val message: String
        get() = notice.message

    val effect: NoticeEffect
        get() = NoticeEffect.ShowNotice(notice)
}

internal fun NotmidProtectedWriteAction.toSuccessNotice(): NotmidProtectedActionNotice {
    return NotmidProtectedActionNotice(
        action = this,
        notice = NoticeRequest(
            id = "notmid-${name}-success",
            message = successMessage(),
            presentation = NoticePresentation.Toast,
            tone = NoticeTone.Success,
        ),
    )
}

internal fun NotmidProtectedWriteFailure.toNotice(): NotmidProtectedActionNotice {
    return NotmidProtectedActionNotice(
        action = action,
        notice = NoticeRequest(
            id = "notmid-${action.name}-failure-${noticeCode()}",
            message = toUserMessage(),
            presentation = toPresentation(),
            tone = toTone(),
        ),
    )
}

internal fun Throwable.toProtectedActionNotice(
    fallbackAction: NotmidProtectedWriteAction,
): NotmidProtectedActionNotice {
    val failure = (this as? NotmidProtectedWriteException)?.failure
    return failure?.toNotice() ?: NotmidProtectedActionNotice(
        action = fallbackAction,
        notice = NoticeRequest(
            id = "notmid-${fallbackAction.name}-failure-unexpected",
            message = "This action failed. Try again.",
            presentation = NoticePresentation.Alert,
            tone = NoticeTone.Error,
        ),
    )
}

internal fun NotmidProtectedWriteFailure.toUserMessage(): String {
    return when (this) {
        is NotmidProtectedWriteFailure.MissingAuth -> "Sign in before using this action."
        is NotmidProtectedWriteFailure.InvalidRequest -> message
        is NotmidProtectedWriteFailure.HttpStatus -> when (statusCode) {
            401 -> "Your session needs to be refreshed before this action can continue."
            403 -> "This account cannot use that action yet."
            404 -> "The linked notmid item could not be found."
            else -> "The notmid API returned HTTP $statusCode."
        }

        is NotmidProtectedWriteFailure.Network -> "The notmid API could not be reached: $message"
        is NotmidProtectedWriteFailure.MalformedResponse -> {
            "The notmid API response did not match the Android contract."
        }
    }
}

internal fun NotmidProtectedWriteAction.successMessage(): String {
    return when (this) {
        NotmidProtectedWriteAction.CapturePublish -> "Receipt queued for moderation."
        NotmidProtectedWriteAction.ClipSave -> "Clip saved."
        NotmidProtectedWriteAction.ChatStart -> "Chat started."
        NotmidProtectedWriteAction.ChatMessage -> "Message sent."
        NotmidProtectedWriteAction.ChatInviteResponse -> "Chat request updated."
        NotmidProtectedWriteAction.ProfileSettings -> "Profile settings saved."
    }
}

private fun NotmidProtectedWriteFailure.noticeCode(): String {
    return when (this) {
        is NotmidProtectedWriteFailure.MissingAuth -> "missing-auth"
        is NotmidProtectedWriteFailure.InvalidRequest -> code
        is NotmidProtectedWriteFailure.HttpStatus -> "http-$statusCode"
        is NotmidProtectedWriteFailure.Network -> "network-$code"
        is NotmidProtectedWriteFailure.MalformedResponse -> "malformed-response"
    }
}

private fun NotmidProtectedWriteFailure.toPresentation(): NoticePresentation {
    return when (this) {
        is NotmidProtectedWriteFailure.InvalidRequest -> NoticePresentation.Toast
        is NotmidProtectedWriteFailure.MissingAuth,
        is NotmidProtectedWriteFailure.HttpStatus,
        is NotmidProtectedWriteFailure.Network,
        is NotmidProtectedWriteFailure.MalformedResponse,
        -> NoticePresentation.Alert
    }
}

private fun NotmidProtectedWriteFailure.toTone(): NoticeTone {
    return when (this) {
        is NotmidProtectedWriteFailure.InvalidRequest,
        is NotmidProtectedWriteFailure.MissingAuth,
        -> NoticeTone.Warning

        is NotmidProtectedWriteFailure.HttpStatus,
        is NotmidProtectedWriteFailure.Network,
        is NotmidProtectedWriteFailure.MalformedResponse,
        -> NoticeTone.Error
    }
}
