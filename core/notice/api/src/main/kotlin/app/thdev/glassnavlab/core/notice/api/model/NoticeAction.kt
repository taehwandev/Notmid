package app.thdev.glassnavlab.core.notice.api.model

sealed interface NoticeAction {
    val label: String

    data class OpenDeepLink(
        override val label: String,
        val deepLink: String,
    ) : NoticeAction
}
