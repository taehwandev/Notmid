package app.thdev.glassnavlab.core.auth.impl

interface GoogleIdTokenProvider {
    suspend fun idToken(): GoogleIdTokenResult
}
