package app.thdev.glassnavlab.core.data.notmid

import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteAction
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteException
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteFailure
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteRepository
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthUser
import app.thdev.glassnavlab.core.model.notmid.NotmidCaptureModerationStatus
import app.thdev.glassnavlab.core.model.notmid.NotmidCapturePublishReceipt
import app.thdev.glassnavlab.core.model.notmid.NotmidCapturePublishRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidCaptureVisibility
import app.thdev.glassnavlab.core.model.notmid.NotmidChatAccess
import app.thdev.glassnavlab.core.model.notmid.NotmidChatInviteDecision
import app.thdev.glassnavlab.core.model.notmid.NotmidChatInviteResponseReceipt
import app.thdev.glassnavlab.core.model.notmid.NotmidChatInviteStatus
import app.thdev.glassnavlab.core.model.notmid.NotmidChatRelationship
import app.thdev.glassnavlab.core.model.notmid.NotmidClip
import app.thdev.glassnavlab.core.model.notmid.NotmidClipSaveReceipt
import app.thdev.glassnavlab.core.model.notmid.NotmidColor
import app.thdev.glassnavlab.core.model.notmid.NotmidMessageAttachment
import app.thdev.glassnavlab.core.model.notmid.NotmidProfilePrivacySettings
import app.thdev.glassnavlab.core.model.notmid.NotmidProfileSettings
import app.thdev.glassnavlab.core.model.notmid.NotmidProfileSettingsUpdateReceipt
import app.thdev.glassnavlab.core.model.notmid.NotmidProfileSettingsUpdateRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidSendThreadMessageReceipt
import app.thdev.glassnavlab.core.model.notmid.NotmidSendThreadMessageRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidStartThreadReceipt
import app.thdev.glassnavlab.core.model.notmid.NotmidStartThreadRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidThread
import app.thdev.glassnavlab.core.model.notmid.NotmidThreadMessage
import app.thdev.glassnavlab.core.network.notmid.NotmidApiPaths
import app.thdev.glassnavlab.core.network.notmid.NotmidHttpMethod
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkClient
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkException
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

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

private fun NotmidCapturePublishRequest.toJsonBody(): String {
    return buildJsonObject {
        put("draftId", draftId)
        put("caption", caption)
        put("placeId", placeId)
        putJsonArray("moodTags") {
            moodTags.forEach { tag -> add(JsonPrimitive(tag)) }
        }
        put("visibility", visibility.toApiValue())
    }.toString()
}

private fun NotmidSendThreadMessageRequest.toJsonBody(): String {
    return buildJsonObject {
        put("body", body)
        attachment?.let { value ->
            put("attachment", value.toJsonObject())
        }
    }.toString()
}

private fun NotmidStartThreadRequest.toJsonBody(): String {
    return buildJsonObject {
        put("participantHandle", participantHandle)
        put("body", body)
        attachedClipId?.let { clipId -> put("attachedClipId", clipId) }
        attachedPlaceId?.let { placeId -> put("attachedPlaceId", placeId) }
    }.toString()
}

private fun NotmidProfileSettingsUpdateRequest.toJsonBody(): String {
    return buildJsonObject {
        put("displayName", displayName)
        put("homeNeighborhood", homeNeighborhood)
    }.toString()
}

private fun NotmidMessageAttachment.toJsonObject(): JsonObject {
    return buildJsonObject {
        when (val attachment = this@toJsonObject) {
            is NotmidMessageAttachment.Clip -> {
                put("type", "clip")
                put("clipId", attachment.clipId)
            }

            is NotmidMessageAttachment.Place -> {
                put("type", "place")
                put("placeId", attachment.placeId)
            }

            is NotmidMessageAttachment.Route -> {
                put("type", "route")
                put("title", attachment.title)
                putJsonArray("placeIds") {
                    attachment.placeIds.forEach { placeId -> add(JsonPrimitive(placeId)) }
                }
            }
        }
    }
}

private fun JsonObject.toClip(): NotmidClip {
    val id = requiredString("id")
    val metrics = requiredObject("metrics")
    return NotmidClip(
        id = id,
        title = requiredString("title"),
        description = requiredString("caption"),
        badge = optionalStringArray("moodTags").firstOrNull()
            ?: requiredString("capturedAtLabel"),
        palette = paletteForStableId(id),
        isLive = requiredString("capturedAtLabel").contains("m") ||
            requiredString("capturedAtLabel") == "now",
        placeId = requiredString("placeId"),
        creatorHandle = requiredString("creatorHandle"),
        moodTags = optionalStringArray("moodTags"),
        capturedAtLabel = requiredString("capturedAtLabel"),
        qualityLabel = metrics.optionalString("distanceLabel") ?: "HD",
        playbackProgress = stableProgressFor(id),
    )
}

private fun JsonObject.toThreadMessage(): NotmidThreadMessage {
    return NotmidThreadMessage(
        id = requiredString("id"),
        threadId = requiredString("threadId"),
        senderHandle = requiredString("senderHandle"),
        body = requiredString("body"),
        createdAtLabel = requiredString("createdAtLabel"),
        mine = requiredBoolean("mine"),
        attachment = optionalObject("attachment")?.toMessageAttachment(),
    )
}

private fun JsonObject.toThread(): NotmidThread {
    return NotmidThread(
        id = requiredString("id"),
        title = requiredString("title"),
        preview = requiredString("preview"),
        updatedAtLabel = requiredString("updatedAtLabel"),
        participantHandles = requiredStringArray("participantHandles"),
        attachedPlaceId = optionalString("attachedPlaceId"),
        attachedClipId = optionalString("attachedClipId"),
        unreadCount = requiredInt("unreadCount"),
        chatAccess = optionalObject("chatAccess")?.toChatAccess()
            ?: NotmidChatAccess.AcceptedFriend,
    )
}

private fun JsonObject.toChatAccess(): NotmidChatAccess {
    return NotmidChatAccess(
        relationship = requiredString("relationship").toChatRelationship(),
        inviteStatus = requiredString("inviteStatus").toChatInviteStatus(),
        canSendMessage = requiredBoolean("canSendMessage"),
        canAcceptInvite = requiredBoolean("canAcceptInvite"),
        canRejectInvite = requiredBoolean("canRejectInvite"),
        reasonLabel = requiredString("reasonLabel"),
    )
}

private fun JsonObject.toMessageAttachment(): NotmidMessageAttachment {
    return when (requiredString("type")) {
        "clip" -> NotmidMessageAttachment.Clip(clipId = requiredString("clipId"))
        "place" -> NotmidMessageAttachment.Place(placeId = requiredString("placeId"))
        "route" -> NotmidMessageAttachment.Route(
            title = requiredString("title"),
            placeIds = requiredStringArray("placeIds"),
        )

        else -> error("Unsupported message attachment type.")
    }
}

private fun JsonObject.toProfileSettings(): NotmidProfileSettings {
    return NotmidProfileSettings(
        user = requiredObject("user").toAuthUser(),
        privacy = requiredObject("privacy").toPrivacySettings(),
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

private fun JsonObject.toPrivacySettings(): NotmidProfilePrivacySettings {
    return NotmidProfilePrivacySettings(
        savedPlacesVisibility = requiredString("savedPlacesVisibility"),
        chatInvites = requiredString("chatInvites"),
        defaultReceiptVisibility = requiredString("defaultReceiptVisibility").toCaptureVisibility(),
    )
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

private fun JsonObject.requiredInt(name: String): Int {
    return this[name]?.jsonPrimitive?.intOrNull ?: error("Missing int field: $name")
}

private fun JsonObject.requiredStringArray(name: String): List<String> {
    return requiredArray(name).map { element -> element.jsonPrimitive.content }
}

private fun JsonObject.optionalStringArray(name: String): List<String> {
    return this[name]?.jsonArray?.map { element -> element.jsonPrimitive.content }.orEmpty()
}

private fun String.toModerationStatus(): NotmidCaptureModerationStatus {
    return when (this) {
        "queued" -> NotmidCaptureModerationStatus.Queued
        "published" -> NotmidCaptureModerationStatus.Published
        else -> error("Unsupported moderation status: $this")
    }
}

private fun String.toCaptureVisibility(): NotmidCaptureVisibility {
    return when (this) {
        "public" -> NotmidCaptureVisibility.Public
        "friends" -> NotmidCaptureVisibility.Friends
        "private" -> NotmidCaptureVisibility.Private
        else -> error("Unsupported capture visibility: $this")
    }
}

private fun NotmidCaptureVisibility.toApiValue(): String {
    return when (this) {
        NotmidCaptureVisibility.Public -> "public"
        NotmidCaptureVisibility.Friends -> "friends"
        NotmidCaptureVisibility.Private -> "private"
    }
}

private fun NotmidChatInviteDecision.toApiPath(threadId: String): String {
    return when (this) {
        NotmidChatInviteDecision.Accept -> NotmidApiPaths.threadInviteAccept(threadId)
        NotmidChatInviteDecision.Reject -> NotmidApiPaths.threadInviteReject(threadId)
    }
}

private fun String.toChatRelationship(): NotmidChatRelationship {
    return when (this) {
        "friend" -> NotmidChatRelationship.Friend
        "non-friend" -> NotmidChatRelationship.NonFriend
        else -> error("Unsupported chat relationship: $this")
    }
}

private fun String.toChatInviteStatus(): NotmidChatInviteStatus {
    return when (this) {
        "accepted" -> NotmidChatInviteStatus.Accepted
        "pending-inbound" -> NotmidChatInviteStatus.PendingInbound
        "pending-outbound" -> NotmidChatInviteStatus.PendingOutbound
        "rejected" -> NotmidChatInviteStatus.Rejected
        else -> error("Unsupported chat invite status: $this")
    }
}

private fun paletteForStableId(id: String): List<NotmidColor> {
    val hash = id.fold(0) { acc, char -> (acc * 31 + char.code).and(0x00FFFFFF) }
    val primary = 0xFF000000L or hash.toLong()
    val secondary = 0xFF000000L or hash.rotateColor(8).toLong()
    val tertiary = 0xFF000000L or hash.rotateColor(16).toLong()
    return listOf(
        NotmidColor(primary.ensureVisibleColor()),
        NotmidColor(secondary.ensureVisibleColor()),
        NotmidColor(tertiary.ensureVisibleColor()),
    )
}

private fun Int.rotateColor(bits: Int): Int {
    return ((this shl bits) or (this ushr (24 - bits))).and(0x00FFFFFF)
}

private fun Long.ensureVisibleColor(): Long {
    val rgb = this and 0x00FFFFFF
    return if (rgb < 0x202020) this or 0x004A4A4A else this
}

private fun stableProgressFor(id: String): Float {
    val bucket = id.fold(17) { acc, char -> acc + char.code }.mod(70)
    return (bucket + 20) / 100f
}
