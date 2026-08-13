package app.thdev.glassnavlab.core.auth.impl

sealed interface FirebaseIdTokenResult {
    data class Success(
        val token: String,
    ) : FirebaseIdTokenResult

    data class Rejected(
        val code: String,
        val message: String,
    ) : FirebaseIdTokenResult
}
