package app.thdev.glassnavlab.feature.auth

internal sealed interface NotmidLoginAction {
    data class IdentityChanged(val value: String) : NotmidLoginAction
    data class PasswordChanged(val value: String) : NotmidLoginAction
    data object PasswordVisibilityToggled : NotmidLoginAction
    data object ContinuePrimaryAuth : NotmidLoginAction
    data object ContinueGoogleAuth : NotmidLoginAction
    data object BrowseSignedOut : NotmidLoginAction
}
