package app.thdev.glassnavlab.auth

import app.thdev.glassnavlab.core.auth.notmid.GoogleIdTokenResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidCredentialManagerGoogleIdTokenProviderTest {
    @Test
    fun missingServerClientIdRejectsBeforeReadingCredentials() {
        val reader = RecordingGoogleCredentialReader(
            GoogleCredentialReaderResult.Success("header.payload.signature"),
        )
        val provider = AndroidCredentialManagerGoogleIdTokenProvider(
            serverClientId = " ",
            credentialReader = reader,
        )

        val result = runSuspend { provider.idToken() }

        assertTrue(result is GoogleIdTokenResult.Rejected)
        assertEquals("google_server_client_id_missing", (result as GoogleIdTokenResult.Rejected).code)
        assertTrue(reader.serverClientIds.isEmpty())
    }

    @Test
    fun successfulCredentialReturnsTrimmedIdToken() {
        val reader = RecordingGoogleCredentialReader(
            GoogleCredentialReaderResult.Success(" header.payload.signature "),
        )
        val provider = AndroidCredentialManagerGoogleIdTokenProvider(
            serverClientId = " server-client-id ",
            credentialReader = reader,
        )

        val result = runSuspend { provider.idToken() }

        assertTrue(result is GoogleIdTokenResult.Success)
        assertEquals("header.payload.signature", (result as GoogleIdTokenResult.Success).token)
        assertEquals(listOf("server-client-id"), reader.serverClientIds)
    }

    @Test
    fun malformedCredentialTokenIsRejected() {
        val provider = AndroidCredentialManagerGoogleIdTokenProvider(
            serverClientId = "server-client-id",
            credentialReader = RecordingGoogleCredentialReader(
                GoogleCredentialReaderResult.Success("not-a-jwt"),
            ),
        )

        val result = runSuspend { provider.idToken() }

        assertTrue(result is GoogleIdTokenResult.Rejected)
        assertEquals("google_id_token_invalid", (result as GoogleIdTokenResult.Rejected).code)
    }

    @Test
    fun cancelledCredentialFlowReturnsTypedRejection() {
        val provider = AndroidCredentialManagerGoogleIdTokenProvider(
            serverClientId = "server-client-id",
            credentialReader = RecordingGoogleCredentialReader(
                GoogleCredentialReaderResult.Cancelled,
            ),
        )

        val result = runSuspend { provider.idToken() }

        assertTrue(result is GoogleIdTokenResult.Rejected)
        assertEquals("google_credential_cancelled", (result as GoogleIdTokenResult.Rejected).code)
    }

    @Test
    fun readerFailurePassesThroughTypedRejection() {
        val provider = AndroidCredentialManagerGoogleIdTokenProvider(
            serverClientId = "server-client-id",
            credentialReader = RecordingGoogleCredentialReader(
                GoogleCredentialReaderResult.Failure(
                    code = "google_credential_unavailable",
                    message = "Unavailable.",
                ),
            ),
        )

        val result = runSuspend { provider.idToken() }

        assertTrue(result is GoogleIdTokenResult.Rejected)
        assertEquals("google_credential_unavailable", (result as GoogleIdTokenResult.Rejected).code)
        assertEquals("Unavailable.", result.message)
    }
}

private class RecordingGoogleCredentialReader(
    private val result: GoogleCredentialReaderResult,
) : GoogleCredentialReader {
    val serverClientIds = mutableListOf<String>()

    override suspend fun idToken(serverClientId: String): GoogleCredentialReaderResult {
        serverClientIds += serverClientId
        return result
    }
}
