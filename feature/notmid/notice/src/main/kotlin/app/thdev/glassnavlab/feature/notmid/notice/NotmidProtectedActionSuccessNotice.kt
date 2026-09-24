package app.thdev.glassnavlab.feature.notmid.notice

import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteAction
import app.thdev.glassnavlab.core.notice.api.model.NoticePresentation
import app.thdev.glassnavlab.core.notice.api.model.NoticeRequest
import app.thdev.glassnavlab.core.notice.api.model.NoticeTone

fun NotmidProtectedWriteAction.toSuccessNotice(): NotmidProtectedActionNotice {
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

private fun NotmidProtectedWriteAction.successMessage(): String {
    return when (this) {
        NotmidProtectedWriteAction.CapturePublish -> "Receipt queued for moderation."
        NotmidProtectedWriteAction.ClipSave -> "Clip saved."
        NotmidProtectedWriteAction.ChatStart -> "Chat started."
        NotmidProtectedWriteAction.ChatMessage -> "Message sent."
        NotmidProtectedWriteAction.ChatInviteResponse -> "Chat request updated."
        NotmidProtectedWriteAction.ProfileSettings -> "Profile settings saved."
    }
}
