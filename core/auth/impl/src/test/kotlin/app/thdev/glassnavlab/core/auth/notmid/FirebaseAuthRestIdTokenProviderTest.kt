package app.thdev.glassnavlab.core.auth.notmid

import app.thdev.glassnavlab.core.model.notmid.NotmidAuthProvider
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkClient
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkError
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkErrorCode
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkException
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkRequest
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebaseAuthRestIdTokenProviderTest {
    @Test
    fun anonymousProviderUsesFirebaseSignUpRestEndpoint() {
        val client = RecordingNetworkClient(success("""{"idToken":"firebase.id.token"}"""))
        val provider = FirebaseAuthRestIdTokenProvider(
            client = client,
            config = FirebaseAuthRestConfig(apiKey = "public api key"),
        )

        val result = runSuspend { provider.idTokenFor(NotmidAuthProvider.Anonymous) }

        assertTrue(result is FirebaseIdTokenResult.Success)
        assertEquals("firebase.id.token", (result as FirebaseIdTokenResult.Success).token)

        val request = client.requests.single()
        assertEquals("/v1/accounts:signUp?key=public+api+key", request.path)
        assertEquals("""{"returnSecureToken":true}""", request.body)
    }

    @Test
    fun googleProviderExchangesInjectedGoogleIdToken() {
        val client = RecordingNetworkClient(success("""{"idToken":"firebase.google.token"}"""))
        val provider = FirebaseAuthRestIdTokenProvider(
            client = client,
            config = FirebaseAuthRestConfig(
                apiKey = "public-key",
                requestUri = "https://example.com/notmid/firebase-auth/android",
            ),
            googleIdTokenProvider = StaticGoogleIdTokenProvider("google.header.payload"),
        )

        val result = runSuspend { provider.idTokenFor(NotmidAuthProvider.Google) }

        assertTrue(result is FirebaseIdTokenResult.Success)
        assertEquals("firebase.google.token", (result as FirebaseIdTokenResult.Success).token)

        val request = client.requests.single()
        assertEquals("/v1/accounts:signInWithIdp?key=public-key", request.path)
        assertTrue(request.body.orEmpty().contains("providerId=google.com"))
        assertTrue(request.body.orEmpty().contains("id_token=google.header.payload"))
        assertTrue(
            request.body.orEmpty().contains(
                """"requestUri":"https://example.com/notmid/firebase-auth/android"""",
            ),
        )
        assertTrue(request.body.orEmpty().contains(""""returnSecureToken":true"""))
    }

    @Test
    fun googleProviderLinksCurrentAnonymousSessionWhenAvailable() {
        val client = RecordingNetworkClient(
            success("""{"idToken":"anonymous.firebase.token"}"""),
            success("""{"idToken":"linked.firebase.token"}"""),
        )
        val provider = FirebaseAuthRestIdTokenProvider(
            client = client,
            config = FirebaseAuthRestConfig(
                apiKey = "public-key",
                requestUri = "https://example.com/notmid/firebase-auth/android",
            ),
            googleIdTokenProvider = StaticGoogleIdTokenProvider("google.header.payload"),
        )

        runSuspend { provider.idTokenFor(NotmidAuthProvider.Anonymous) }
        val result = runSuspend { provider.idTokenFor(NotmidAuthProvider.Google) }

        assertTrue(result is FirebaseIdTokenResult.Success)
        assertEquals("linked.firebase.token", (result as FirebaseIdTokenResult.Success).token)

        val request = client.requests[1]
        assertTrue(request.body.orEmpty().contains(""""idToken":"anonymous.firebase.token""""))
    }

    @Test
    fun clearSessionPreventsGoogleLinkingWithPreviousAnonymousSession() {
        val client = RecordingNetworkClient(
            success("""{"idToken":"anonymous.firebase.token"}"""),
            success("""{"idToken":"google.firebase.token"}"""),
        )
        val provider = FirebaseAuthRestIdTokenProvider(
            client = client,
            config = FirebaseAuthRestConfig(
                apiKey = "public-key",
                requestUri = "https://example.com/notmid/firebase-auth/android",
            ),
            googleIdTokenProvider = StaticGoogleIdTokenProvider("google.header.payload"),
        )

        runSuspend { provider.idTokenFor(NotmidAuthProvider.Anonymous) }
        provider.clearSession()
        runSuspend { provider.idTokenFor(NotmidAuthProvider.Google) }

        val request = client.requests[1]
        assertTrue(request.body.orEmpty().contains("providerId=google.com"))
        assertTrue(!request.body.orEmpty().contains(""""idToken":"anonymous.firebase.token""""))
    }

    @Test
    fun missingConfigRejectsBeforeNetwork() {
        val client = RecordingNetworkClient(success("""{"idToken":"firebase.id.token"}"""))
        val provider = FirebaseAuthRestIdTokenProvider(
            client = client,
            config = FirebaseAuthRestConfig(apiKey = ""),
        )

        val result = runSuspend { provider.idTokenFor(NotmidAuthProvider.Anonymous) }

        assertTrue(result is FirebaseIdTokenResult.Rejected)
        assertEquals("firebase_auth_config_missing", (result as FirebaseIdTokenResult.Rejected).code)
        assertTrue(client.requests.isEmpty())
    }

    @Test
    fun googleProviderRejectionReturnsTypedFailureBeforeNetwork() {
        val client = RecordingNetworkClient(success("""{"idToken":"firebase.google.token"}"""))
        val provider = FirebaseAuthRestIdTokenProvider(
            client = client,
            config = FirebaseAuthRestConfig(apiKey = "public-key"),
            googleIdTokenProvider = UnavailableGoogleIdTokenProvider(),
        )

        val result = runSuspend { provider.idTokenFor(NotmidAuthProvider.Google) }

        assertTrue(result is FirebaseIdTokenResult.Rejected)
        assertEquals(
            "google_id_token_provider_unavailable",
            (result as FirebaseIdTokenResult.Rejected).code,
        )
        assertTrue(client.requests.isEmpty())
    }

    @Test
    fun firebaseHttpErrorIsRejected() {
        val provider = FirebaseAuthRestIdTokenProvider(
            client = RecordingNetworkClient(success("""{"error":"bad"}""", statusCode = 400)),
            config = FirebaseAuthRestConfig(apiKey = "public-key"),
        )

        val result = runSuspend { provider.idTokenFor(NotmidAuthProvider.Anonymous) }

        assertTrue(result is FirebaseIdTokenResult.Rejected)
        assertEquals(
            "firebase_anonymous_auth_http_400",
            (result as FirebaseIdTokenResult.Rejected).code,
        )
    }

    @Test
    fun malformedFirebaseBodyIsRejected() {
        val provider = FirebaseAuthRestIdTokenProvider(
            client = RecordingNetworkClient(success("""{"refreshToken":"refresh"}""")),
            config = FirebaseAuthRestConfig(apiKey = "public-key"),
        )

        val result = runSuspend { provider.idTokenFor(NotmidAuthProvider.Anonymous) }

        assertTrue(result is FirebaseIdTokenResult.Rejected)
        assertEquals(
            "firebase_anonymous_auth_malformed",
            (result as FirebaseIdTokenResult.Rejected).code,
        )
    }

    @Test
    fun transportFailureIsRejected() {
        val provider = FirebaseAuthRestIdTokenProvider(
            client = RecordingNetworkClient(
                networkFailure(
                    NotmidNetworkError(
                        code = NotmidNetworkErrorCode.Transport,
                        message = "offline",
                    ),
                ),
            ),
            config = FirebaseAuthRestConfig(apiKey = "public-key"),
        )

        val result = runSuspend { provider.idTokenFor(NotmidAuthProvider.Anonymous) }

        assertTrue(result is FirebaseIdTokenResult.Rejected)
        assertEquals(
            "firebase_anonymous_auth_network_error",
            (result as FirebaseIdTokenResult.Rejected).code,
        )
    }
}

private class RecordingNetworkClient(
    vararg result: RecordedNetworkResult,
) : NotmidNetworkClient {
    val requests = mutableListOf<NotmidNetworkRequest>()
    private val results = result.toMutableList()

    override suspend fun execute(request: NotmidNetworkRequest): NotmidNetworkResponse {
        requests += request
        return when (results.size) {
            0 -> error("No recorded network result.")
            1 -> results.single()
            else -> results.removeAt(0)
        }.responseOrThrow()
    }
}

private sealed interface RecordedNetworkResult {
    data class Success(
        val response: NotmidNetworkResponse,
    ) : RecordedNetworkResult

    data class Failure(
        val exception: NotmidNetworkException,
    ) : RecordedNetworkResult
}

private class StaticGoogleIdTokenProvider(
    private val token: String,
) : GoogleIdTokenProvider {
    override suspend fun idToken(): GoogleIdTokenResult {
        return GoogleIdTokenResult.Success(token)
    }
}

private fun success(
    body: String,
    statusCode: Int = 200,
): RecordedNetworkResult {
    return RecordedNetworkResult.Success(
        NotmidNetworkResponse(
            statusCode = statusCode,
            body = body,
            headers = emptyMap(),
        ),
    )
}

private fun networkFailure(
    error: NotmidNetworkError,
): RecordedNetworkResult {
    return RecordedNetworkResult.Failure(NotmidNetworkException(error))
}

private fun RecordedNetworkResult.responseOrThrow(): NotmidNetworkResponse {
    return when (this) {
        is RecordedNetworkResult.Failure -> throw exception
        is RecordedNetworkResult.Success -> response
    }
}
