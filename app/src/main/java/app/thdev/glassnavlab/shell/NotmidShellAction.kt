package app.thdev.glassnavlab.shell

internal sealed interface NotmidShellAction {
    data class DestinationClicked(val destinationId: String) : NotmidShellAction
    data class ThreadClicked(val threadId: String) : NotmidShellAction
    data class PlaceClicked(val placeId: String) : NotmidShellAction
    data object SettingsClicked : NotmidShellAction
}
