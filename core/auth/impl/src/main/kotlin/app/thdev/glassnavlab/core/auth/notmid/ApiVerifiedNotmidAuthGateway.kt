package app.thdev.glassnavlab.core.auth.notmid

import app.thdev.glassnavlab.core.model.notmid.NotmidAuthMode
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthProvider
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthRequiredAction
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthSession
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthUser
import app.thdev.glassnavlab.core.network.notmid.NotmidApiPaths
import app.thdev.glassnavlab.core.network.notmid.NotmidHttpMethod
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkClient
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkException
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ApiVerifiedNotmidAuthGateway(
    private val client: NotmidNetworkClient,
    private val idTokenProvider: FirebaseIdTokenProvider,
) : NotmidAuthGateway {
    private var state: NotmidAuthState = signedOutState(NotmidAuthMode.Firebase)

    override fun currentState(): NotmidAuthState = state

    override suspend fun signIn(request: NotmidAuthSignInRequest): NotmidAuthResult {
        if (request.provider == NotmidAuthProvider.Fake) {
            return rejected(
                code = "firebase_provider_required",
                message = "Firebase auth mode requires an anonymous or Google provider.",
            )
        }

        val token = when (val tokenResult = idTokenProvider.idTokenFor(request.provider)) {
            is FirebaseIdTokenResult.Rejected -> {
                return rejected(
                    code = tokenResult.code,
                    message = tokenResult.message,
                )
            }

            is FirebaseIdTokenResult.Success -> tokenResult.token
        }

        if (token.isBlank()) {
            return rejected(
                code = "firebase_token_blank",
                message = "Firebase sign-in returned an empty ID token.",
            )
        }

        return when (val result = authStatus(token)) {
            is AuthStatusResult.Failure -> rejected(
                code = result.code,
                message = result.message,
            )

            is AuthStatusResult.Success -> {
                val response = result.response
                if (!response.authenticated || response.user == null) {
                    rejected(
                        code = "firebase_token_unverified",
                        message = "The notmid API did not verify this Firebase session.",
                    )
                } else {
                    state = NotmidAuthState(
                        mode = NotmidAuthMode.Firebase,
                        session = NotmidAuthSession(
                            accessToken = token,
                            provider = request.provider,
                            expiresAt = response.sessionExpiresAt.orEmpty(),
                            user = response.user,
                        ),
                        requiredActions = response.requiredActions,
                    )
                    NotmidAuthResult.Success(
                        state = state,
                        nextPath = request.nextPath(),
                    )
                }
            }
        }
    }

    override fun signOut(): NotmidAuthState {
        idTokenProvider.clearSession()
        state = signedOutState(NotmidAuthMode.Firebase)
        return state
    }

    private suspend fun authStatus(idToken: String): AuthStatusResult {
        val response = try {
            client.execute(
                NotmidNetworkRequest(
                    method = NotmidHttpMethod.Get,
                    path = NotmidApiPaths.AUTH_STATUS,
                    headers = mapOf("authorization" to "Bearer $idToken"),
                ),
            )
        } catch (exception: NotmidNetworkException) {
            return AuthStatusResult.Failure(
                code = "notmid_api_auth_network_error",
                message = "The notmid API auth check failed: ${exception.error.message}",
            )
        }

        if (!response.isSuccessful) {
            return AuthStatusResult.Failure(
                code = "notmid_api_auth_http_${response.statusCode}",
                message = "The notmid API auth check returned HTTP ${response.statusCode}.",
            )
        }

        return try {
            AuthStatusResult.Success(response.body.toAuthStatusResponse())
        } catch (exception: RuntimeException) {
            AuthStatusResult.Failure(
                code = "notmid_api_auth_malformed",
                message = "The notmid API auth response did not match the Android contract.",
            )
        }
    }

    private fun rejected(code: String, message: String): NotmidAuthResult.Rejected {
        return NotmidAuthResult.Rejected(
            code = code,
            message = message,
            state = state,
        )
    }
}

private sealed interface AuthStatusResult {
    data class Success(
        val response: AuthStatusResponse,
    ) : AuthStatusResult

    data class Failure(
        val code: String,
        val message: String,
    ) : AuthStatusResult
}

private data class AuthStatusResponse(
    val authenticated: Boolean,
    val user: NotmidAuthUser?,
    val sessionExpiresAt: String?,
    val requiredActions: List<NotmidAuthRequiredAction>,
)

private fun String.toAuthStatusResponse(): AuthStatusResponse {
    val root = Json.parseToJsonElement(this).jsonObject
    return AuthStatusResponse(
        authenticated = root.requiredBoolean("authenticated"),
        user = root.optionalObject("user")?.toAuthUser(),
        sessionExpiresAt = root.optionalString("sessionExpiresAt"),
        requiredActions = root.requiredArray("requiredFor").map(JsonElement::toRequiredAction),
    )
}

private fun JsonObject.toAuthUser(): NotmidAuthUser {
    return NotmidAuthUser(
        id = requiredString("id"),
        handle = requiredString("handle"),
        displayName = requiredString("displayName"),
        homeNeighborhood = requiredString("homeNeighborhood"),
        avatarImageUrl = requiredString("avatarImageUrl"),
        roles = requiredStringArray("roles"),
    )
}

private fun JsonElement.toRequiredAction(): NotmidAuthRequiredAction {
    return when (jsonPrimitive.content) {
        "capture" -> NotmidAuthRequiredAction.Capture
        "save" -> NotmidAuthRequiredAction.Save
        "chat" -> NotmidAuthRequiredAction.Chat
        "profile-edit" -> NotmidAuthRequiredAction.ProfileEdit
        "moderation" -> NotmidAuthRequiredAction.Moderation
        else -> error("Unsupported auth requirement: ${jsonPrimitive.content}")
    }
}

private fun JsonObject.requiredObject(name: String): JsonObject {
    return this[name]?.jsonObject ?: error("Missing object field: $name")
}

private fun JsonObject.optionalObject(name: String): JsonObject? {
    return this[name]?.jsonObject
}

private fun JsonObject.requiredArray(name: String): JsonArray {
    return this[name]?.jsonArray ?: error("Missing array field: $name")
}

private fun JsonObject.requiredString(name: String): String {
    return this[name]?.jsonPrimitive?.contentOrNull ?: error("Missing string field: $name")
}

private fun JsonObject.optionalString(name: String): String? {
    return this[name]?.jsonPrimitive?.contentOrNull
}

private fun JsonObject.requiredBoolean(name: String): Boolean {
    return this[name]?.jsonPrimitive?.booleanOrNull ?: error("Missing boolean field: $name")
}

private fun JsonObject.requiredStringArray(name: String): List<String> {
    return requiredArray(name).map { element ->
        element.jsonPrimitive.content
    }
}
