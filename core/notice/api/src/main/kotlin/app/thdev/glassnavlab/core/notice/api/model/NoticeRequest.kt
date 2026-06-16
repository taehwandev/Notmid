package app.thdev.glassnavlab.core.notice.api.model

data class NoticeRequest(
    val id: String,
    val message: String,
    val presentation: NoticePresentation,
    val tone: NoticeTone = NoticeTone.Info,
    val action: NoticeAction? = null,
)
