package app.thdev.glassnavlab

import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteAction
import app.thdev.glassnavlab.core.notice.api.effect.NoticeEffect
import app.thdev.glassnavlab.core.notice.api.model.NoticeRequest

internal data class NotmidProtectedActionNotice(
    val action: NotmidProtectedWriteAction,
    val notice: NoticeRequest,
) {
    val message: String
        get() = notice.message

    val effect: NoticeEffect
        get() = NoticeEffect.ShowNotice(notice)
}
