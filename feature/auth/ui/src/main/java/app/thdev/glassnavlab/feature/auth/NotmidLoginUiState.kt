package app.thdev.glassnavlab.feature.auth

internal data class NotmidLoginUiState(
    val identity: String = "",
    val password: String = "",
    val showPassword: Boolean = false,
    val isAuthenticating: Boolean = false,
    val errorMessage: String? = null,
)
