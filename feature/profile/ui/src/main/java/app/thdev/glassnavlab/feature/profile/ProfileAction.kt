package app.thdev.glassnavlab.feature.profile

internal sealed interface ProfileAction {
    data object OpenSettings : ProfileAction
}
