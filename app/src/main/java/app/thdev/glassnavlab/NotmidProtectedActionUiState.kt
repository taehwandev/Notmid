package app.thdev.glassnavlab

import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteAction
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteException
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteFailure
import app.thdev.glassnavlab.core.feedback.api.effect.FeedbackEffect
import app.thdev.glassnavlab.core.feedback.api.model.FeedbackPresentation
import app.thdev.glassnavlab.core.feedback.api.model.FeedbackRequest
import app.thdev.glassnavlab.core.feedback.api.model.FeedbackTone
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

internal data class NotmidProtectedActionFeedback(
    val action: NotmidProtectedWriteAction,
    val feedback: FeedbackRequest,
) {
    val message: String
        get() = feedback.message

    val effect: FeedbackEffect
        get() = FeedbackEffect.ShowFeedback(feedback)
}

internal fun NotmidProtectedWriteAction.toSuccessFeedback(): NotmidProtectedActionFeedback {
    return NotmidProtectedActionFeedback(
        action = this,
        feedback = FeedbackRequest(
            id = "notmid-${name}-success",
            message = successMessage(),
            presentation = FeedbackPresentation.Toast,
            tone = FeedbackTone.Success,
        ),
    )
}

internal fun NotmidProtectedWriteFailure.toFeedback(): NotmidProtectedActionFeedback {
    return NotmidProtectedActionFeedback(
        action = action,
        feedback = FeedbackRequest(
            id = "notmid-${action.name}-failure-${feedbackCode()}",
            message = toUserMessage(),
            presentation = toPresentation(),
            tone = toTone(),
        ),
    )
}

internal fun Throwable.toProtectedActionFeedback(
    fallbackAction: NotmidProtectedWriteAction,
): NotmidProtectedActionFeedback {
    val failure = (this as? NotmidProtectedWriteException)?.failure
    return failure?.toFeedback() ?: NotmidProtectedActionFeedback(
        action = fallbackAction,
        feedback = FeedbackRequest(
            id = "notmid-${fallbackAction.name}-failure-unexpected",
            message = "This action failed. Try again.",
            presentation = FeedbackPresentation.Alert,
            tone = FeedbackTone.Error,
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

private fun NotmidProtectedWriteFailure.feedbackCode(): String {
    return when (this) {
        is NotmidProtectedWriteFailure.MissingAuth -> "missing-auth"
        is NotmidProtectedWriteFailure.InvalidRequest -> code
        is NotmidProtectedWriteFailure.HttpStatus -> "http-$statusCode"
        is NotmidProtectedWriteFailure.Network -> "network-$code"
        is NotmidProtectedWriteFailure.MalformedResponse -> "malformed-response"
    }
}

private fun NotmidProtectedWriteFailure.toPresentation(): FeedbackPresentation {
    return when (this) {
        is NotmidProtectedWriteFailure.InvalidRequest -> FeedbackPresentation.Toast
        is NotmidProtectedWriteFailure.MissingAuth,
        is NotmidProtectedWriteFailure.HttpStatus,
        is NotmidProtectedWriteFailure.Network,
        is NotmidProtectedWriteFailure.MalformedResponse,
        -> FeedbackPresentation.Alert
    }
}

private fun NotmidProtectedWriteFailure.toTone(): FeedbackTone {
    return when (this) {
        is NotmidProtectedWriteFailure.InvalidRequest,
        is NotmidProtectedWriteFailure.MissingAuth,
        -> FeedbackTone.Warning

        is NotmidProtectedWriteFailure.HttpStatus,
        is NotmidProtectedWriteFailure.Network,
        is NotmidProtectedWriteFailure.MalformedResponse,
        -> FeedbackTone.Error
    }
}
