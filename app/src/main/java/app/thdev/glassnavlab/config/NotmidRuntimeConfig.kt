package app.thdev.glassnavlab.config

import app.thdev.glassnavlab.BuildConfig
import app.thdev.glassnavlab.core.auth.notmid.FirebaseAuthRestConfig
import app.thdev.glassnavlab.core.data.notmid.NotmidContentSource
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthMode
import app.thdev.glassnavlab.core.network.notmid.NotmidApiConfig

object NotmidRuntimeConfig {
    val apiBaseUrl: String = BuildConfig.NOTMID_API_BASE_URL
    val authMode: NotmidAuthMode = BuildConfig.NOTMID_AUTH_MODE.toNotmidAuthMode()
    val contentSource: NotmidContentSource =
        NotmidContentSource.from(BuildConfig.NOTMID_CONTENT_SOURCE)
    val mapProvider: String = BuildConfig.NOTMID_MAP_PROVIDER
    val apiConfig: NotmidApiConfig = NotmidApiConfig(apiBaseUrl)
    val firebaseAuthConfig: FirebaseAuthRestConfig = FirebaseAuthRestConfig(
        apiKey = BuildConfig.NOTMID_FIREBASE_API_KEY,
        requestUri = BuildConfig.NOTMID_FIREBASE_AUTH_REQUEST_URI,
    )
    val googleServerClientId: String = BuildConfig.NOTMID_GOOGLE_SERVER_CLIENT_ID
    val firebaseIdentityToolkitApiConfig: NotmidApiConfig =
        NotmidApiConfig("https://identitytoolkit.googleapis.com")

    val hasGoogleMapsApiKey: Boolean
        get() = BuildConfig.NOTMID_GOOGLE_MAPS_API_KEY.isNotBlank()

    val hasMapboxAccessToken: Boolean
        get() = BuildConfig.NOTMID_MAPBOX_ACCESS_TOKEN.isNotBlank()

    val hasGoogleServerClientId: Boolean
        get() = googleServerClientId.isNotBlank()
}

private fun String.toNotmidAuthMode(): NotmidAuthMode {
    return when (trim().lowercase()) {
        "fake" -> NotmidAuthMode.Fake
        "firebase" -> NotmidAuthMode.Firebase
        "disabled" -> NotmidAuthMode.Disabled
        else -> error("Unsupported NOTMID_AUTH_MODE: $this")
    }
}
