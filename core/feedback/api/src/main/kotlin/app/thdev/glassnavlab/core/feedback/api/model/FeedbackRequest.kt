package app.thdev.glassnavlab.core.feedback.api.model

data class FeedbackRequest(
    val id: String,
    val message: String,
    val presentation: FeedbackPresentation,
    val tone: FeedbackTone = FeedbackTone.Info,
    val action: FeedbackAction? = null,
)
