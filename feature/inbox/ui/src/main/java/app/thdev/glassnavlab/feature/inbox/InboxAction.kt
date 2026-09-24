package app.thdev.glassnavlab.feature.inbox

internal sealed interface InboxAction {
    data class FilterSelected(val filter: String) : InboxAction
    data class ThreadClicked(val threadId: String) : InboxAction
    data object Retry : InboxAction
}
