package app.thdev.glassnavlab.feature.feed

internal sealed interface FeedAction {
    data class ClipClicked(val clipId: String) : FeedAction
    data object Retry : FeedAction
}
