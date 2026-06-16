package app.thdev.glassnavlab.coreapp.feedback.host

import app.thdev.glassnavlab.core.feedback.api.model.FeedbackAction

internal fun FeedbackAction.perform(onActionDeepLink: (String) -> Unit) {
    when (this) {
        is FeedbackAction.OpenDeepLink -> {
            deepLink
                .takeIf(String::isNotBlank)
                ?.let(onActionDeepLink)
        }
    }
}
