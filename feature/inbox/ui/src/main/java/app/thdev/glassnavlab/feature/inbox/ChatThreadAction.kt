package app.thdev.glassnavlab.feature.inbox

internal sealed interface ChatThreadAction {
    data class DraftChanged(val draft: String) : ChatThreadAction
    data object Send : ChatThreadAction
    data object SaveClip : ChatThreadAction
    data object OpenPlace : ChatThreadAction
    data object AcceptInvite : ChatThreadAction
    data object RejectInvite : ChatThreadAction
    data object Retry : ChatThreadAction
}
