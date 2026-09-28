package app.thdev.glassnavlab.feature.profile

internal sealed interface ProfileAction {
    data class SelectTab(val tab: ProfileTab) : ProfileAction
    data object OpenSettings : ProfileAction
}
