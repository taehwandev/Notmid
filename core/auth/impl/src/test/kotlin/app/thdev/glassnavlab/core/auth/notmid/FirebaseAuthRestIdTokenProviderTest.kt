package app.thdev.glassnavlab.core.auth.notmid

import app.thdev.glassnavlab.core.model.notmid.NotmidAuthProvider
import app.thdev.glassnavlab.core.network.assertions.QueuedNetworkFailure
import app.thdev.glassnavlab.core.network.assertions.QueuedNetworkResponse
import app.thdev.glassnavlab.core.network.assertions.RecordingNotmidNetworkClient
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkError
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkErrorCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebaseAuthRestIdTokenProviderTest {
    @Test
    fun anonymousProviderUsesFirebaseSignUpRestEndpoint() {
        val client = RecordingNotmidNetworkClient(QueuedNetworkResponse("""{"idToken":"firebase.id.token"}"""))
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
        val client = RecordingNotmidNetworkClient(QueuedNetworkResponse("""{"idToken":"firebase.google.token"}"""))
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
        val client = RecordingNotmidNetworkClient(
            QueuedNetworkResponse("""{"idToken":"anonymous.firebase.token"}"""),
            QueuedNetworkResponse("""{"idToken":"linked.firebase.token"}"""),
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
        val client = RecordingNotmidNetworkClient(
            QueuedNetworkResponse("""{"idToken":"anonymous.firebase.token"}"""),
            QueuedNetworkResponse("""{"idToken":"google.firebase.token"}"""),
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
        val client = RecordingNotmidNetworkClient(QueuedNetworkResponse("""{"idToken":"firebase.id.token"}"""))
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
        val client = RecordingNotmidNetworkClient(QueuedNetworkResponse("""{"idToken":"firebase.google.token"}"""))
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
            client = RecordingNotmidNetworkClient(
                QueuedNetworkResponse("""{"error":"bad"}""", statusCode = 400),
            ),
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
            client = RecordingNotmidNetworkClient(QueuedNetworkResponse("""{"refreshToken":"refresh"}""")),
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
            client = RecordingNotmidNetworkClient(
                QueuedNetworkFailure(
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

private class StaticGoogleIdTokenProvider(
    private val token: String,
) : GoogleIdTokenProvider {
    override suspend fun idToken(): GoogleIdTokenResult {
        return GoogleIdTokenResult.Success(token)
    }
}
