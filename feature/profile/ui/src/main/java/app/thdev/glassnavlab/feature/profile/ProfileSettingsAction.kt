package app.thdev.glassnavlab.feature.profile

internal sealed interface ProfileSettingsAction {
    data class DisplayNameChanged(val value: String) : ProfileSettingsAction
    data class NeighborhoodChanged(val value: String) : ProfileSettingsAction
    data object Save : ProfileSettingsAction
}
