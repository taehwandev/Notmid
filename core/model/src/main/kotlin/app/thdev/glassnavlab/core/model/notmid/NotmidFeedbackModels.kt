package app.thdev.glassnavlab.core.model.notmid

data class NotmidUiFeedback(
    val id: String,
    val message: String,
    val presentation: NotmidFeedbackPresentation,
    val tone: NotmidFeedbackTone = NotmidFeedbackTone.Info,
    val action: NotmidFeedbackAction? = null,
)

enum class NotmidFeedbackPresentation {
    Toast,
    Alert,
    Inline,
    FullPage,
}

enum class NotmidFeedbackTone {
    Info,
    Success,
    Warning,
    Error,
}

data class NotmidFeedbackAction(
    val label: String,
    val deepLink: String? = null,
)

sealed interface NotmidUiEffect {
    data class ShowFeedback(
        val feedback: NotmidUiFeedback,
    ) : NotmidUiEffect

    data class NavigateDeepLink(
        val deepLink: String,
    ) : NotmidUiEffect
}
