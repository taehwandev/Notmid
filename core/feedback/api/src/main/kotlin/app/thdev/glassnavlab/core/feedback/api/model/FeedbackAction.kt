package app.thdev.glassnavlab.core.feedback.api.model

sealed interface FeedbackAction {
    val label: String

    data class OpenDeepLink(
        override val label: String,
        val deepLink: String,
    ) : FeedbackAction
}
