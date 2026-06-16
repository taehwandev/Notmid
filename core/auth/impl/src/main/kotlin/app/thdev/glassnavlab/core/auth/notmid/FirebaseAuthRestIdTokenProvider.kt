package app.thdev.glassnavlab.core.auth.notmid

import app.thdev.glassnavlab.core.model.notmid.NotmidAuthProvider
import app.thdev.glassnavlab.core.network.notmid.NotmidHttpMethod
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkClient
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkException
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkRequest
import java.net.URLEncoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

data class FirebaseAuthRestConfig(
    val apiKey: String,
    val requestUri: String = DefaultFirebaseAuthRequestUri,
) {
    val isConfigured: Boolean
        get() = apiKey.isNotBlank()
}

interface GoogleIdTokenProvider {
    suspend fun idToken(): GoogleIdTokenResult
}

sealed interface GoogleIdTokenResult {
    data class Success(
        val token: String,
    ) : GoogleIdTokenResult

    data class Rejected(
        val code: String,
        val message: String,
    ) : GoogleIdTokenResult
}

class UnavailableGoogleIdTokenProvider(
    private val message: String = "Google sign-in is not configured for this Android build.",
) : GoogleIdTokenProvider {
    override suspend fun idToken(): GoogleIdTokenResult {
        return GoogleIdTokenResult.Rejected(
            code = "google_id_token_provider_unavailable",
            message = message,
        )
    }
}

class FirebaseAuthRestIdTokenProvider(
    private val client: NotmidNetworkClient,
    private val config: FirebaseAuthRestConfig,
    private val googleIdTokenProvider: GoogleIdTokenProvider = UnavailableGoogleIdTokenProvider(),
) : FirebaseIdTokenProvider {
    private var currentSession: FirebaseAuthRestSession? = null

    override suspend fun idTokenFor(provider: NotmidAuthProvider): FirebaseIdTokenResult {
        if (!config.isConfigured) {
            return FirebaseIdTokenResult.Rejected(
                code = "firebase_auth_config_missing",
                message = "Firebase Auth REST config is missing for this Android build.",
            )
        }

        return when (provider) {
            NotmidAuthProvider.Anonymous -> signInAnonymously()
            NotmidAuthProvider.Google -> signInWithGoogle()
            NotmidAuthProvider.Fake -> FirebaseIdTokenResult.Rejected(
                code = "firebase_provider_required",
                message = "Firebase auth mode requires an anonymous or Google provider.",
            )
        }
    }

    override fun clearSession() {
        currentSession = null
    }

    private suspend fun signInAnonymously(): FirebaseIdTokenResult {
        return executeFirebaseAuthRequest(
            path = "/v1/accounts:signUp?key=${config.apiKey.urlEncoded()}",
            body = buildJsonObject {
                put("returnSecureToken", true)
            }.toString(),
            failureCodePrefix = "firebase_anonymous_auth",
        ).rememberSession(NotmidAuthProvider.Anonymous)
    }

    private suspend fun signInWithGoogle(): FirebaseIdTokenResult {
        val googleIdToken = when (val result = googleIdTokenProvider.idToken()) {
            is GoogleIdTokenResult.Success -> result.token.trim()
            is GoogleIdTokenResult.Rejected -> {
                return FirebaseIdTokenResult.Rejected(
                    code = result.code,
                    message = result.message,
                )
            }
        }

        if (!googleIdToken.looksLikeJwt()) {
            return FirebaseIdTokenResult.Rejected(
                code = "google_id_token_invalid",
                message = "Google sign-in did not return a valid ID token.",
            )
        }
        if (config.requestUri.isBlank()) {
            return FirebaseIdTokenResult.Rejected(
                code = "firebase_google_request_uri_missing",
                message = "Firebase Google sign-in requires a request URI.",
            )
        }

        val anonymousSessionIdToken = currentSession
            ?.takeIf { session -> session.provider == NotmidAuthProvider.Anonymous }
            ?.idToken
            ?.takeIf(String::isNotBlank)

        return executeFirebaseAuthRequest(
            path = "/v1/accounts:signInWithIdp?key=${config.apiKey.urlEncoded()}",
            body = buildJsonObject {
                put(
                    "postBody",
                    "id_token=${googleIdToken.urlEncoded()}&providerId=google.com",
                )
                put("requestUri", config.requestUri)
                if (anonymousSessionIdToken != null) {
                    put("idToken", anonymousSessionIdToken)
                }
                put("returnIdpCredential", false)
                put("returnSecureToken", true)
            }.toString(),
            failureCodePrefix = "firebase_google_auth",
        ).rememberSession(NotmidAuthProvider.Google)
    }

    private suspend fun executeFirebaseAuthRequest(
        path: String,
        body: String,
        failureCodePrefix: String,
    ): FirebaseIdTokenResult {
        val response = try {
            client.execute(
                NotmidNetworkRequest(
                    method = NotmidHttpMethod.Post,
                    path = path,
                    body = body,
                ),
            )
        } catch (exception: NotmidNetworkException) {
            return FirebaseIdTokenResult.Rejected(
                code = "${failureCodePrefix}_network_error",
                message = "Firebase Auth REST request failed: ${exception.error.message}",
            )
        }

        if (!response.isSuccessful) {
            return FirebaseIdTokenResult.Rejected(
                code = "${failureCodePrefix}_http_${response.statusCode}",
                message = "Firebase Auth rejected the sign-in request.",
            )
        }

        return response.body.toFirebaseIdTokenResult(
            malformedCode = "${failureCodePrefix}_malformed",
        )
    }

    private fun FirebaseIdTokenResult.rememberSession(
        provider: NotmidAuthProvider,
    ): FirebaseIdTokenResult {
        if (this is FirebaseIdTokenResult.Success) {
            currentSession = FirebaseAuthRestSession(
                idToken = token,
                provider = provider,
            )
        }
        return this
    }
}

private data class FirebaseAuthRestSession(
    val idToken: String,
    val provider: NotmidAuthProvider,
)

private fun String.toFirebaseIdTokenResult(malformedCode: String): FirebaseIdTokenResult {
    val idToken = runCatching {
        Json.parseToJsonElement(this)
            .jsonObject["idToken"]
            ?.jsonPrimitive
            ?.contentOrNull
            ?.trim()
            .orEmpty()
    }.getOrDefault("")

    if (idToken.isBlank()) {
        return FirebaseIdTokenResult.Rejected(
            code = malformedCode,
            message = "Firebase Auth did not return an ID token.",
        )
    }

    return FirebaseIdTokenResult.Success(idToken)
}

private fun String.urlEncoded(): String = URLEncoder.encode(this, Charsets.UTF_8.name())

private fun String.looksLikeJwt(): Boolean = split(".").size == 3

private const val DefaultFirebaseAuthRequestUri = "https://thdev.app/notmid/firebase-auth/android"
