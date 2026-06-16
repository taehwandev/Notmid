package app.thdev.glassnavlab.core.notice.api.effect

import app.thdev.glassnavlab.core.notice.api.model.NoticeRequest

sealed interface NoticeEffect {
    data class ShowNotice(
        val notice: NoticeRequest,
    ) : NoticeEffect

    data class NavigateDeepLink(
        val deepLink: String,
    ) : NoticeEffect
}
