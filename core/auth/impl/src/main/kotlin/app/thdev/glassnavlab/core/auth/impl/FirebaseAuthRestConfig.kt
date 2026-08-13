package app.thdev.glassnavlab.core.auth.impl

data class FirebaseAuthRestConfig(
    val apiKey: String,
    val requestUri: String = DefaultFirebaseAuthRequestUri,
) {
    val isConfigured: Boolean
        get() = apiKey.isNotBlank()
}

private const val DefaultFirebaseAuthRequestUri =
    "https://thdev.app/notmid/firebase-auth/android"
