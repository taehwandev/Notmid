package app.thdev.glassnavlab.core.auth.token

interface GoogleIdTokenProvider {
    suspend fun idToken(): GoogleIdTokenResult
}
