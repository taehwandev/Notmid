package app.thdev.glassnavlab.core.auth.impl

sealed interface GoogleIdTokenResult {
    data class Success(
        val token: String,
    ) : GoogleIdTokenResult

    data class Rejected(
        val code: String,
        val message: String,
    ) : GoogleIdTokenResult
}
