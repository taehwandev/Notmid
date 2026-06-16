package app.thdev.glassnavlab.core.auth.notmid

import app.thdev.glassnavlab.core.model.notmid.NotmidAuthMode
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthProvider
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthRequiredAction
import app.thdev.glassnavlab.core.network.assertions.QueuedNetworkFailure
import app.thdev.glassnavlab.core.network.assertions.QueuedNetworkResponse
import app.thdev.glassnavlab.core.network.assertions.RecordingNotmidNetworkClient
import app.thdev.glassnavlab.core.network.notmid.NotmidApiPaths
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkError
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkErrorCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiVerifiedNotmidAuthGatewayTest {
    @Test
    fun firebaseAnonymousSignInVerifiesTokenWithApi() {
        val client = RecordingNotmidNetworkClient(QueuedNetworkResponse(authenticatedStatusJson))
        val gateway = ApiVerifiedNotmidAuthGateway(
            client = client,
            idTokenProvider = StaticFirebaseIdTokenProvider("firebase.id.token"),
        )

        val result = runSuspend {
            gateway.signIn(
                NotmidAuthSignInRequest(
                    provider = NotmidAuthProvider.Anonymous,
                    intent = NotmidAuthIntent.Capture,
                ),
            )
        }

        assertTrue(result is NotmidAuthResult.Success)
        val success = result as NotmidAuthResult.Success
        assertEquals("/notmid/capture", success.nextPath)
        assertEquals(NotmidAuthMode.Firebase, success.state.mode)
        assertEquals(NotmidAuthProvider.Anonymous, success.state.session?.provider)
        assertEquals("firebase.id.token", success.state.session?.accessToken)
        assertEquals("firebase:uid-123", success.state.session?.user?.id)
        assertEquals(
            listOf(
                NotmidAuthRequiredAction.Capture,
                NotmidAuthRequiredAction.Save,
                NotmidAuthRequiredAction.Chat,
                NotmidAuthRequiredAction.ProfileEdit,
                NotmidAuthRequiredAction.Moderation,
            ),
            success.state.requiredActions,
        )
        assertTrue(gateway.currentState().isAuthenticated)

        val request = client.requests.single()
        assertEquals(NotmidApiPaths.AUTH_STATUS, request.path)
        assertEquals("Bearer firebase.id.token", request.headers["authorization"])
    }

    @Test
    fun firebaseGoogleSignInPreservesProviderAndSafeReturnPath() {
        val gateway = ApiVerifiedNotmidAuthGateway(
            client = RecordingNotmidNetworkClient(QueuedNetworkResponse(authenticatedStatusJson)),
            idTokenProvider = StaticFirebaseIdTokenProvider("google.firebase.id.token"),
        )

        val result = runSuspend {
            gateway.signIn(
                NotmidAuthSignInRequest(
                    provider = NotmidAuthProvider.Google,
                    returnToPath = "/notmid/profile/settings",
                ),
            )
        }

        assertTrue(result is NotmidAuthResult.Success)
        val success = result as NotmidAuthResult.Success
        assertEquals("/notmid/profile/settings", success.nextPath)
        assertEquals(NotmidAuthProvider.Google, success.state.session?.provider)
    }

    @Test
    fun firebaseModeRejectsFakeProviderBeforeNetworkRequest() {
        val client = RecordingNotmidNetworkClient(QueuedNetworkResponse(authenticatedStatusJson))
        val gateway = ApiVerifiedNotmidAuthGateway(
            client = client,
            idTokenProvider = StaticFirebaseIdTokenProvider("firebase.id.token"),
        )

        val result = runSuspend {
            gateway.signIn(
                NotmidAuthSignInRequest(provider = NotmidAuthProvider.Fake),
            )
        }

        assertTrue(result is NotmidAuthResult.Rejected)
        assertEquals("firebase_provider_required", (result as NotmidAuthResult.Rejected).code)
        assertTrue(client.requests.isEmpty())
    }

    @Test
    fun tokenProviderRejectionKeepsSignedOutState() {
        val client = RecordingNotmidNetworkClient(QueuedNetworkResponse(authenticatedStatusJson))
        val gateway = ApiVerifiedNotmidAuthGateway(
            client = client,
            idTokenProvider = RejectingFirebaseIdTokenProvider,
        )

        val result = runSuspend {
            gateway.signIn(
                NotmidAuthSignInRequest(provider = NotmidAuthProvider.Google),
            )
        }

        assertTrue(result is NotmidAuthResult.Rejected)
        val rejected = result as NotmidAuthResult.Rejected
        assertEquals("google_unavailable", rejected.code)
        assertFalse(rejected.state.isAuthenticated)
        assertFalse(gateway.currentState().isAuthenticated)
        assertTrue(client.requests.isEmpty())
    }

    @Test
    fun unverifiedApiStatusIsRejected() {
        val gateway = ApiVerifiedNotmidAuthGateway(
            client = RecordingNotmidNetworkClient(QueuedNetworkResponse(signedOutStatusJson)),
            idTokenProvider = StaticFirebaseIdTokenProvider("firebase.id.token"),
        )

        val result = runSuspend {
            gateway.signIn(
                NotmidAuthSignInRequest(provider = NotmidAuthProvider.Anonymous),
            )
        }

        assertTrue(result is NotmidAuthResult.Rejected)
        assertEquals("firebase_token_unverified", (result as NotmidAuthResult.Rejected).code)
        assertFalse(gateway.currentState().isAuthenticated)
    }

    @Test
    fun apiTransportFailureIsRejected() {
        val gateway = ApiVerifiedNotmidAuthGateway(
            client = RecordingNotmidNetworkClient(
                QueuedNetworkFailure(
                    NotmidNetworkError(
                        code = NotmidNetworkErrorCode.Transport,
                        message = "offline",
                    ),
                ),
            ),
            idTokenProvider = StaticFirebaseIdTokenProvider("firebase.id.token"),
        )

        val result = runSuspend {
            gateway.signIn(
                NotmidAuthSignInRequest(provider = NotmidAuthProvider.Anonymous),
            )
        }

        assertTrue(result is NotmidAuthResult.Rejected)
        assertEquals(
            "notmid_api_auth_network_error",
            (result as NotmidAuthResult.Rejected).code,
        )
    }

    @Test
    fun malformedApiStatusIsRejected() {
        val gateway = ApiVerifiedNotmidAuthGateway(
            client = RecordingNotmidNetworkClient(QueuedNetworkResponse("""{"authenticated":true}""")),
            idTokenProvider = StaticFirebaseIdTokenProvider("firebase.id.token"),
        )

        val result = runSuspend {
            gateway.signIn(
                NotmidAuthSignInRequest(provider = NotmidAuthProvider.Anonymous),
            )
        }

        assertTrue(result is NotmidAuthResult.Rejected)
        assertEquals("notmid_api_auth_malformed", (result as NotmidAuthResult.Rejected).code)
    }

    @Test
    fun signOutClearsFirebaseTokenProviderSession() {
        val idTokenProvider = StaticFirebaseIdTokenProvider("firebase.id.token")
        val gateway = ApiVerifiedNotmidAuthGateway(
            client = RecordingNotmidNetworkClient(QueuedNetworkResponse(authenticatedStatusJson)),
            idTokenProvider = idTokenProvider,
        )

        runSuspend {
            gateway.signIn(NotmidAuthSignInRequest(provider = NotmidAuthProvider.Anonymous))
        }
        val state = gateway.signOut()

        assertFalse(state.isAuthenticated)
        assertEquals(1, idTokenProvider.clearSessionCalls)
    }
}

private class StaticFirebaseIdTokenProvider(
    private val token: String,
) : FirebaseIdTokenProvider {
    var clearSessionCalls = 0
        private set

    override suspend fun idTokenFor(provider: NotmidAuthProvider): FirebaseIdTokenResult {
        return FirebaseIdTokenResult.Success(token)
    }

    override fun clearSession() {
        clearSessionCalls += 1
    }
}

private object RejectingFirebaseIdTokenProvider : FirebaseIdTokenProvider {
    override suspend fun idTokenFor(provider: NotmidAuthProvider): FirebaseIdTokenResult {
        return FirebaseIdTokenResult.Rejected(
            code = "google_unavailable",
            message = "Google sign-in is not available.",
        )
    }
}

private val authenticatedStatusJson = """
{
  "source": "api",
  "generatedAt": "2026-05-30T00:00:00.000Z",
  "mode": "firebase",
  "authenticated": true,
  "user": {
    "id": "firebase:uid-123",
    "handle": "min.zip",
    "displayName": "Min Zip",
    "homeNeighborhood": "Seongsu",
    "avatarImageUrl": "",
    "roles": ["creator"]
  },
  "sessionExpiresAt": "2026-05-30T01:00:00.000Z",
  "requiredFor": ["capture", "save", "chat", "profile-edit", "moderation"]
}
""".trimIndent()

private val signedOutStatusJson = """
{
  "source": "api",
  "generatedAt": "2026-05-30T00:00:00.000Z",
  "mode": "firebase",
  "authenticated": false,
  "requiredFor": ["capture", "save", "chat", "profile-edit", "moderation"]
}
""".trimIndent()
