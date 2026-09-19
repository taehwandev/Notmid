package app.thdev.glassnavlab.feature.feed

internal sealed interface ClipDetailAction {
    data object ChatClicked : ClipDetailAction
    data object Retry : ClipDetailAction
}
