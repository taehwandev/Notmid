package app.thdev.glassnavlab.config

import app.thdev.glassnavlab.BuildConfig
import app.thdev.glassnavlab.core.auth.notmid.FirebaseAuthRestConfig
import app.thdev.glassnavlab.core.data.notmid.NotmidContentSource
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthMode
import app.thdev.glassnavlab.core.network.notmid.NotmidApiConfig

data class NotmidRuntimeConfig(
    val apiBaseUrl: String,
    val authMode: NotmidAuthMode,
    val contentSource: NotmidContentSource,
    val mapProvider: String,
    val firebaseAuthConfig: FirebaseAuthRestConfig,
    val googleServerClientId: String,
) {
    val apiConfig: NotmidApiConfig = NotmidApiConfig(apiBaseUrl)
    val firebaseIdentityToolkitApiConfig: NotmidApiConfig =
        NotmidApiConfig("https://identitytoolkit.googleapis.com")

    val hasGoogleMapsApiKey: Boolean
        get() = BuildConfig.NOTMID_GOOGLE_MAPS_API_KEY.isNotBlank()

    val hasMapboxAccessToken: Boolean
        get() = BuildConfig.NOTMID_MAPBOX_ACCESS_TOKEN.isNotBlank()

    val hasGoogleServerClientId: Boolean
        get() = googleServerClientId.isNotBlank()
}

fun notmidRuntimeConfigFromBuildConfig(): NotmidRuntimeConfig {
    return NotmidRuntimeConfig(
        apiBaseUrl = BuildConfig.NOTMID_API_BASE_URL,
        authMode = BuildConfig.NOTMID_AUTH_MODE.toNotmidAuthMode(),
        contentSource = NotmidContentSource.from(BuildConfig.NOTMID_CONTENT_SOURCE),
        mapProvider = BuildConfig.NOTMID_MAP_PROVIDER,
        firebaseAuthConfig = FirebaseAuthRestConfig(
            apiKey = BuildConfig.NOTMID_FIREBASE_API_KEY,
            requestUri = BuildConfig.NOTMID_FIREBASE_AUTH_REQUEST_URI,
        ),
        googleServerClientId = BuildConfig.NOTMID_GOOGLE_SERVER_CLIENT_ID,
    )
}

private fun String.toNotmidAuthMode(): NotmidAuthMode {
    return when (trim().lowercase()) {
        "fake" -> NotmidAuthMode.Fake
        "firebase" -> NotmidAuthMode.Firebase
        "disabled" -> NotmidAuthMode.Disabled
        else -> error("Unsupported NOTMID_AUTH_MODE: $this")
    }
}
