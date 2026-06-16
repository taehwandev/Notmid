package app.thdev.glassnavlab.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import app.thdev.glassnavlab.core.auth.notmid.GoogleIdTokenProvider
import app.thdev.glassnavlab.core.auth.notmid.GoogleIdTokenResult
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidCredentialManagerGoogleIdTokenProvider : GoogleIdTokenProvider {
    private val serverClientId: String
    private val credentialReader: GoogleCredentialReader

    constructor(
        context: Context,
        serverClientId: String,
    ) : this(
        serverClientId = serverClientId,
        credentialReader = CredentialManagerGoogleCredentialReader(context),
    )

    internal constructor(
        serverClientId: String,
        credentialReader: GoogleCredentialReader,
    ) {
        this.serverClientId = serverClientId
        this.credentialReader = credentialReader
    }

    override suspend fun idToken(): GoogleIdTokenResult {
        val trimmedServerClientId = serverClientId.trim()
        if (trimmedServerClientId.isBlank()) {
            return GoogleIdTokenResult.Rejected(
                code = "google_server_client_id_missing",
                message = "Google sign-in requires an Android server client ID.",
            )
        }

        return when (val result = credentialReader.idToken(trimmedServerClientId)) {
            GoogleCredentialReaderResult.Cancelled -> GoogleIdTokenResult.Rejected(
                code = "google_credential_cancelled",
                message = "Google sign-in was cancelled.",
            )

            is GoogleCredentialReaderResult.Failure -> GoogleIdTokenResult.Rejected(
                code = result.code,
                message = result.message,
            )

            is GoogleCredentialReaderResult.Success -> {
                val idToken = result.idToken.trim()
                if (idToken.looksLikeJwt()) {
                    GoogleIdTokenResult.Success(idToken)
                } else {
                    GoogleIdTokenResult.Rejected(
                        code = "google_id_token_invalid",
                        message = "Google sign-in did not return a valid ID token.",
                    )
                }
            }
        }
    }
}

internal interface GoogleCredentialReader {
    suspend fun idToken(serverClientId: String): GoogleCredentialReaderResult
}

internal sealed interface GoogleCredentialReaderResult {
    data class Success(
        val idToken: String,
    ) : GoogleCredentialReaderResult

    data class Failure(
        val code: String,
        val message: String,
    ) : GoogleCredentialReaderResult

    data object Cancelled : GoogleCredentialReaderResult
}

private class CredentialManagerGoogleCredentialReader(
    context: Context,
) : GoogleCredentialReader {
    private val context: Context = context
    private val credentialManager: CredentialManager = CredentialManager.create(context)

    override suspend fun idToken(serverClientId: String): GoogleCredentialReaderResult {
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder()
                    .setServerClientId(serverClientId)
                    .setFilterByAuthorizedAccounts(false)
                    .setAutoSelectEnabled(true)
                    .build(),
            )
            .build()

        return runCatching {
            withContext(Dispatchers.Main.immediate) {
                credentialManager.getCredential(
                    context = context,
                    request = request,
                )
            }
        }.fold(
            onSuccess = GetCredentialResponse::toReaderResult,
            onFailure = Throwable::toReaderResult,
        )
    }
}

private fun GetCredentialResponse.toReaderResult(): GoogleCredentialReaderResult {
    val customCredential = credential as? CustomCredential
        ?: return GoogleCredentialReaderResult.Failure(
            code = "google_credential_type_unsupported",
            message = "Google sign-in returned an unsupported credential type.",
        )

    if (customCredential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        return GoogleCredentialReaderResult.Failure(
            code = "google_credential_type_unsupported",
            message = "Google sign-in returned an unsupported credential type.",
        )
    }

    return try {
        GoogleCredentialReaderResult.Success(
            idToken = GoogleIdTokenCredential.createFrom(customCredential.data).idToken,
        )
    } catch (exception: GoogleIdTokenParsingException) {
        GoogleCredentialReaderResult.Failure(
            code = "google_id_token_parse_failed",
            message = "Google sign-in returned a malformed ID token credential.",
        )
    }
}

private fun Throwable.toReaderResult(): GoogleCredentialReaderResult {
    return when (this) {
        is GetCredentialCancellationException -> GoogleCredentialReaderResult.Cancelled
        is GetCredentialException -> GoogleCredentialReaderResult.Failure(
            code = "google_credential_unavailable",
            message = "Google sign-in is unavailable on this device.",
        )

        else -> GoogleCredentialReaderResult.Failure(
            code = "google_credential_error",
            message = "Google sign-in could not start.",
        )
    }
}

private fun String.looksLikeJwt(): Boolean = split(".").size == 3
