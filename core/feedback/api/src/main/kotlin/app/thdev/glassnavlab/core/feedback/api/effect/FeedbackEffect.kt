package app.thdev.glassnavlab.core.feedback.api.effect

import app.thdev.glassnavlab.core.feedback.api.model.FeedbackRequest

sealed interface FeedbackEffect {
    data class ShowFeedback(
        val feedback: FeedbackRequest,
    ) : FeedbackEffect

    data class NavigateDeepLink(
        val deepLink: String,
    ) : FeedbackEffect
}
