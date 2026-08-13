package app.thdev.glassnavlab.core.data.notmid

import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteAction
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteException
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteFailure
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteRepository
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState
import app.thdev.glassnavlab.core.model.notmid.NotmidCapturePublishReceipt
import app.thdev.glassnavlab.core.model.notmid.NotmidCapturePublishRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidChatInviteDecision
import app.thdev.glassnavlab.core.model.notmid.NotmidChatInviteResponseReceipt
import app.thdev.glassnavlab.core.model.notmid.NotmidClipSaveReceipt
import app.thdev.glassnavlab.core.model.notmid.NotmidProfileSettingsUpdateReceipt
import app.thdev.glassnavlab.core.model.notmid.NotmidProfileSettingsUpdateRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidSendThreadMessageReceipt
import app.thdev.glassnavlab.core.model.notmid.NotmidSendThreadMessageRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidStartThreadReceipt
import app.thdev.glassnavlab.core.model.notmid.NotmidStartThreadRequest
import app.thdev.glassnavlab.core.network.notmid.NotmidApiPaths
import app.thdev.glassnavlab.core.network.notmid.NotmidHttpMethod
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkClient
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkException
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

class ApiNotmidProtectedWriteRepository(
    private val client: NotmidNetworkClient,
) : NotmidProtectedWriteRepository {
    override suspend fun publishCapture(
        authState: NotmidAuthState,
        request: NotmidCapturePublishRequest,
    ): NotmidCapturePublishReceipt {
        return executeJson(
            authState = authState,
            action = NotmidProtectedWriteAction.CapturePublish,
            method = NotmidHttpMethod.Post,
            path = NotmidApiPaths.CAPTURE_PUBLISH,
            body = request.toJsonBody(),
        ) { root ->
            NotmidCapturePublishReceipt(
                clip = root.requiredObject("clip").toClip(),
                moderationStatus = root.requiredString("moderationStatus").toModerationStatus(),
            )
        }
    }

    override suspend fun saveClip(
        authState: NotmidAuthState,
        clipId: String,
    ): NotmidClipSaveReceipt {
        return executeJson(
            authState = authState,
            action = NotmidProtectedWriteAction.ClipSave,
            method = NotmidHttpMethod.Post,
            path = NotmidApiPaths.clipSave(clipId),
            body = "{}",
        ) { root ->
            NotmidClipSaveReceipt(
                clip = root.requiredObject("clip").toClip(),
                saved = root.requiredBoolean("saved"),
            )
        }
    }

    override suspend fun sendThreadMessage(
        authState: NotmidAuthState,
        threadId: String,
        request: NotmidSendThreadMessageRequest,
    ): NotmidSendThreadMessageReceipt {
        return executeJson(
            authState = authState,
            action = NotmidProtectedWriteAction.ChatMessage,
            method = NotmidHttpMethod.Post,
            path = NotmidApiPaths.threadMessages(threadId),
            body = request.toJsonBody(),
        ) { root ->
            NotmidSendThreadMessageReceipt(
                message = root.requiredObject("message").toThreadMessage(),
            )
        }
    }

    override suspend fun startThread(
        authState: NotmidAuthState,
        request: NotmidStartThreadRequest,
    ): NotmidStartThreadReceipt {
        return executeJson(
            authState = authState,
            action = NotmidProtectedWriteAction.ChatStart,
            method = NotmidHttpMethod.Post,
            path = NotmidApiPaths.INBOX_THREADS,
            body = request.toJsonBody(),
        ) { root ->
            NotmidStartThreadReceipt(
                thread = root.requiredObject("thread").toThread(),
                message = root.optionalObject("message")?.toThreadMessage(),
            )
        }
    }

    override suspend fun respondThreadInvite(
        authState: NotmidAuthState,
        threadId: String,
        decision: NotmidChatInviteDecision,
    ): NotmidChatInviteResponseReceipt {
        return executeJson(
            authState = authState,
            action = NotmidProtectedWriteAction.ChatInviteResponse,
            method = NotmidHttpMethod.Post,
            path = decision.toApiPath(threadId),
            body = "{}",
        ) { root ->
            NotmidChatInviteResponseReceipt(
                thread = root.requiredObject("thread").toThread(),
            )
        }
    }

    override suspend fun updateProfileSettings(
        authState: NotmidAuthState,
        request: NotmidProfileSettingsUpdateRequest,
    ): NotmidProfileSettingsUpdateReceipt {
        return executeJson(
            authState = authState,
            action = NotmidProtectedWriteAction.ProfileSettings,
            method = NotmidHttpMethod.Patch,
            path = NotmidApiPaths.PROFILE_SETTINGS,
            body = request.toJsonBody(),
        ) { root ->
            NotmidProfileSettingsUpdateReceipt(
                settings = root.requiredObject("settings").toProfileSettings(),
                updated = root.requiredBoolean("updated"),
            )
        }
    }

    private suspend fun <T> executeJson(
        authState: NotmidAuthState,
        action: NotmidProtectedWriteAction,
        method: NotmidHttpMethod,
        path: String,
        body: String,
        transform: (JsonObject) -> T,
    ): T {
        val accessToken = authState.session?.accessToken?.trim()
        if (accessToken.isNullOrBlank()) {
            throw NotmidProtectedWriteException(
                NotmidProtectedWriteFailure.MissingAuth(action),
            )
        }

        val response = try {
            client.execute(
                NotmidNetworkRequest(
                    method = method,
                    path = path,
                    headers = mapOf("authorization" to "Bearer $accessToken"),
                    body = body,
                ),
            )
        } catch (exception: NotmidNetworkException) {
            throw NotmidProtectedWriteException(
                NotmidProtectedWriteFailure.Network(
                    action = action,
                    path = path,
                    code = exception.error.code.name,
                    message = exception.error.message,
                    causeName = exception.error.causeName,
                ),
            )
        }

        if (!response.isSuccessful) {
            throw NotmidProtectedWriteException(
                response.toProtectedWriteFailure(action, path),
            )
        }

        return try {
            transform(Json.parseToJsonElement(response.body).jsonObject)
        } catch (exception: RuntimeException) {
            throw NotmidProtectedWriteException(
                NotmidProtectedWriteFailure.MalformedResponse(
                    action = action,
                    path = path,
                    causeName = exception::class.java.simpleName,
                ),
            )
        }
    }
}

private fun NotmidChatInviteDecision.toApiPath(threadId: String): String {
    return when (this) {
        NotmidChatInviteDecision.Accept -> NotmidApiPaths.threadInviteAccept(threadId)
        NotmidChatInviteDecision.Reject -> NotmidApiPaths.threadInviteReject(threadId)
    }
}
