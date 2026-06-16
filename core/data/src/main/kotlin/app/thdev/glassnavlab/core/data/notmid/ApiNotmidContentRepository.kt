package app.thdev.glassnavlab.core.data.notmid

import app.thdev.glassnavlab.core.domain.notmid.NotmidContentRepository
import app.thdev.glassnavlab.core.model.notmid.NotmidCaptureDraft
import app.thdev.glassnavlab.core.model.notmid.NotmidCaptureMediaState
import app.thdev.glassnavlab.core.model.notmid.NotmidCaptureVisibility
import app.thdev.glassnavlab.core.model.notmid.NotmidChatAccess
import app.thdev.glassnavlab.core.model.notmid.NotmidChatInviteStatus
import app.thdev.glassnavlab.core.model.notmid.NotmidChatRelationship
import app.thdev.glassnavlab.core.model.notmid.NotmidClip
import app.thdev.glassnavlab.core.model.notmid.NotmidColor
import app.thdev.glassnavlab.core.model.notmid.NotmidColors
import app.thdev.glassnavlab.core.model.notmid.NotmidDestination
import app.thdev.glassnavlab.core.model.notmid.NotmidGeoPoint
import app.thdev.glassnavlab.core.model.notmid.NotmidMessageAttachment
import app.thdev.glassnavlab.core.model.notmid.NotmidNavigationIcon
import app.thdev.glassnavlab.core.model.notmid.NotmidPlace
import app.thdev.glassnavlab.core.model.notmid.NotmidThread
import app.thdev.glassnavlab.core.model.notmid.NotmidThreadMessage
import app.thdev.glassnavlab.core.network.notmid.NotmidApiPaths
import app.thdev.glassnavlab.core.network.notmid.NotmidHttpMethod
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkClient
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkError
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkException
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ApiNotmidContentRepository(
    private val client: NotmidNetworkClient,
) : NotmidContentRepository {
    override suspend fun destinations(): List<NotmidDestination> {
        val feed = getContract(NotmidApiPaths.FEED, JsonObject::toFeedResponse)
        val map = getContract(NotmidApiPaths.MAP, JsonObject::toMapResponse)
        val capture = getContract(
            NotmidApiPaths.CAPTURE_DRAFT,
            JsonObject::toCaptureDraftResponse,
        )
        val inbox = getContract(NotmidApiPaths.INBOX_THREADS, JsonObject::toInboxResponse)
        val threadDetails = inbox.threads.map { thread ->
            getContract(
                path = NotmidApiPaths.threadDetail(thread.id),
                transform = JsonObject::toThreadDetailResponse,
            )
        }
        val highlightedClipIds = map.highlightedClipIds.toSet()
        val inboxClips = (feed.clips + threadDetails.mapNotNull { detail ->
            detail.attachedClip
        }).distinctBy(NotmidClip::id)
        val inboxPlaces = (feed.places + threadDetails.mapNotNull { detail ->
            detail.attachedPlace
        }).distinctBy(NotmidPlace::id)
        val inboxThreads = threadDetails.map(ThreadDetailResponse::thread)
            .ifEmpty { inbox.threads }

        return listOf(
            NotmidDestination(
                id = "feed",
                title = "Feed",
                subtitle = "Short video receipts from the notmid API.",
                icon = NotmidNavigationIcon.Feed,
                clips = feed.clips,
                places = feed.places,
            ),
            NotmidDestination(
                id = "map",
                title = "Map",
                subtitle = "Place-first discovery backed by the notmid API.",
                icon = NotmidNavigationIcon.Map,
                clips = feed.clips.filter { it.id in highlightedClipIds },
                places = map.places,
            ),
            NotmidDestination(
                id = "capture",
                title = "Capture",
                subtitle = "Record, attach a place, and publish with API policy checks.",
                icon = NotmidNavigationIcon.Capture,
                clips = emptyList(),
                places = capture.candidatePlaces,
                captureDraft = capture.draft,
            ),
            NotmidDestination(
                id = "inbox",
                title = "Inbox",
                subtitle = "Place-aware threads from the notmid API.",
                icon = NotmidNavigationIcon.Inbox,
                clips = inboxClips,
                places = inboxPlaces,
                threads = inboxThreads,
                threadMessages = threadDetails.flatMap(ThreadDetailResponse::messages),
            ),
            NotmidDestination(
                id = "profile",
                title = "Profile",
                subtitle = "Account, privacy, and creator settings.",
                icon = NotmidNavigationIcon.Profile,
                clips = emptyList(),
                places = emptyList(),
            ),
        )
    }

    private suspend fun <T> getContract(path: String, transform: (JsonObject) -> T): T {
        val json = getJson(path)
        return try {
            transform(json)
        } catch (exception: RuntimeException) {
            throw ApiNotmidContentException.MalformedJson(
                path = path,
                cause = exception,
            )
        }
    }

    private suspend fun getJson(path: String): JsonObject {
        val response = try {
            client.execute(
                NotmidNetworkRequest(
                    method = NotmidHttpMethod.Get,
                    path = path,
                ),
            )
        } catch (exception: NotmidNetworkException) {
            throw ApiNotmidContentException.Network(
                path = path,
                error = exception.error,
            )
        }

        if (!response.isSuccessful) {
            throw ApiNotmidContentException.HttpStatus(
                path = path,
                statusCode = response.statusCode,
                body = response.body,
            )
        }

        return try {
            Json.parseToJsonElement(response.body).jsonObject
        } catch (exception: RuntimeException) {
            throw ApiNotmidContentException.MalformedJson(
                path = path,
                cause = exception,
            )
        }
    }
}

sealed class ApiNotmidContentException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause) {
    class HttpStatus(
        val path: String,
        val statusCode: Int,
        val body: String,
    ) : ApiNotmidContentException(
        "notmid API request failed for $path with HTTP $statusCode.",
    )

    class Network(
        val path: String,
        val error: NotmidNetworkError,
    ) : ApiNotmidContentException(
        "notmid API network request failed for $path: ${error.message}",
    )

    class MalformedJson(
        val path: String,
        cause: Throwable,
    ) : ApiNotmidContentException(
        "notmid API response for $path did not match the expected contract.",
        cause,
    )
}

private data class FeedResponse(
    val clips: List<NotmidClip>,
    val places: List<NotmidPlace>,
)

private data class MapResponse(
    val places: List<NotmidPlace>,
    val highlightedClipIds: List<String>,
)

private data class CaptureDraftResponse(
    val draft: NotmidCaptureDraft,
    val candidatePlaces: List<NotmidPlace>,
)

private data class InboxResponse(
    val threads: List<NotmidThread>,
)

private data class ThreadDetailResponse(
    val thread: NotmidThread,
    val messages: List<NotmidThreadMessage>,
    val attachedClip: NotmidClip?,
    val attachedPlace: NotmidPlace?,
)

private fun JsonObject.toFeedResponse(): FeedResponse {
    return FeedResponse(
        clips = requiredArray("clips").map(JsonElement::toClip),
        places = requiredArray("places").map(JsonElement::toPlace),
    )
}

private fun JsonObject.toMapResponse(): MapResponse {
    return MapResponse(
        places = requiredArray("places").map(JsonElement::toPlace),
        highlightedClipIds = requiredArray("highlightedClipIds").map { element ->
            element.jsonPrimitive.content
        },
    )
}

private fun JsonObject.toCaptureDraftResponse(): CaptureDraftResponse {
    return CaptureDraftResponse(
        draft = requiredObject("draft").toCaptureDraft(),
        candidatePlaces = requiredArray("candidatePlaces").map(JsonElement::toPlace),
    )
}

private fun JsonObject.toInboxResponse(): InboxResponse {
    return InboxResponse(
        threads = requiredArray("threads").map(JsonElement::toThread),
    )
}

private fun JsonObject.toThreadDetailResponse(): ThreadDetailResponse {
    return ThreadDetailResponse(
        thread = requiredObject("thread").toThread(),
        messages = requiredArray("messages").map(JsonElement::toThreadMessage),
        attachedClip = optionalObject("attachedClip")?.let(JsonElement::toClip),
        attachedPlace = optionalObject("attachedPlace")?.let(JsonElement::toPlace),
    )
}

private fun JsonElement.toClip(): NotmidClip {
    val item = jsonObject
    val id = item.requiredString("id")
    val metrics = item.requiredObject("metrics")
    return NotmidClip(
        id = id,
        title = item.requiredString("title"),
        description = item.requiredString("caption"),
        badge = item.optionalStringArray("moodTags").firstOrNull()
            ?: item.requiredString("capturedAtLabel"),
        palette = paletteForStableId(id),
        isLive = item.requiredString("capturedAtLabel").contains("m") ||
            item.requiredString("capturedAtLabel") == "now",
        placeId = item.requiredString("placeId"),
        creatorHandle = item.requiredString("creatorHandle"),
        moodTags = item.optionalStringArray("moodTags"),
        capturedAtLabel = item.requiredString("capturedAtLabel"),
        qualityLabel = metrics.optionalString("distanceLabel") ?: "HD",
        playbackProgress = stableProgressFor(id),
    )
}

private fun JsonElement.toPlace(): NotmidPlace {
    val item = jsonObject
    val id = item.requiredString("id")
    val category = item.requiredString("category")
    val neighborhood = item.requiredString("neighborhood")
    val openNow = item.requiredBoolean("openNow")
    val receiptCount = item.requiredInt("receiptCount")
    return NotmidPlace(
        id = id,
        title = item.requiredString("name"),
        description = "$category in $neighborhood",
        metric = item.requiredInt("score").toString(),
        palette = paletteForStableId(id),
        heightDp = 136 + (receiptCount % 4) * 18,
        contentColor = if (openNow) NotmidColors.White else NotmidColors.DarkCardContent,
        category = category,
        address = item.requiredString("address"),
        coordinate = NotmidGeoPoint(
            latitude = item.requiredDouble("lat"),
            longitude = item.requiredDouble("lng"),
        ),
        openNow = openNow,
        receiptCount = receiptCount,
    )
}

private fun JsonElement.toThread(): NotmidThread {
    val item = jsonObject
    return NotmidThread(
        id = item.requiredString("id"),
        title = item.requiredString("title"),
        preview = item.requiredString("preview"),
        updatedAtLabel = item.requiredString("updatedAtLabel"),
        participantHandles = item.requiredStringArray("participantHandles"),
        attachedPlaceId = item.optionalString("attachedPlaceId"),
        attachedClipId = item.optionalString("attachedClipId"),
        unreadCount = item.requiredInt("unreadCount"),
        chatAccess = item.optionalObject("chatAccess")?.toChatAccess()
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

private fun JsonElement.toThreadMessage(): NotmidThreadMessage {
    val item = jsonObject
    return NotmidThreadMessage(
        id = item.requiredString("id"),
        threadId = item.requiredString("threadId"),
        senderHandle = item.requiredString("senderHandle"),
        body = item.requiredString("body"),
        createdAtLabel = item.requiredString("createdAtLabel"),
        mine = item.requiredBoolean("mine"),
        attachment = item.optionalObject("attachment")?.toMessageAttachment(),
    )
}

private fun JsonObject.toMessageAttachment(): NotmidMessageAttachment {
    val type = requiredString("type")
    return when (type) {
        "clip" -> NotmidMessageAttachment.Clip(
            clipId = requiredString("clipId"),
        )

        "place" -> NotmidMessageAttachment.Place(
            placeId = requiredString("placeId"),
        )

        "route" -> NotmidMessageAttachment.Route(
            title = requiredString("title"),
            placeIds = requiredStringArray("placeIds"),
        )

        else -> error("Unsupported message attachment type: $type")
    }
}

private fun JsonObject.toCaptureDraft(): NotmidCaptureDraft {
    return NotmidCaptureDraft(
        id = requiredString("id"),
        caption = requiredString("caption"),
        placeId = optionalString("placeId"),
        moodTags = requiredStringArray("moodTags"),
        visibility = requiredString("visibility").toCaptureVisibility(),
        mediaState = requiredString("mediaState").toCaptureMediaState(),
        statusLabel = "Draft synced from API",
        waitTimeLabel = "pending",
        crowdLabel = "live",
        priceTierLabel = "$$",
    )
}

private fun String.toCaptureVisibility(): NotmidCaptureVisibility {
    return when (this) {
        "public" -> NotmidCaptureVisibility.Public
        "friends" -> NotmidCaptureVisibility.Friends
        "private" -> NotmidCaptureVisibility.Private
        else -> error("Unsupported capture visibility: $this")
    }
}

private fun String.toCaptureMediaState(): NotmidCaptureMediaState {
    return when (this) {
        "empty" -> NotmidCaptureMediaState.Empty
        "local-preview" -> NotmidCaptureMediaState.LocalPreview
        "uploaded" -> NotmidCaptureMediaState.Uploaded
        else -> error("Unsupported capture media state: $this")
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

private fun JsonObject.requiredInt(name: String): Int {
    return this[name]?.jsonPrimitive?.intOrNull ?: error("Missing int field: $name")
}

private fun JsonObject.requiredDouble(name: String): Double {
    return this[name]?.jsonPrimitive?.doubleOrNull ?: error("Missing double field: $name")
}

private fun JsonObject.requiredBoolean(name: String): Boolean {
    return this[name]?.jsonPrimitive?.booleanOrNull ?: error("Missing boolean field: $name")
}

private fun JsonObject.requiredStringArray(name: String): List<String> {
    return requiredArray(name).map { element ->
        element.jsonPrimitive.content
    }
}

private fun JsonObject.optionalStringArray(name: String): List<String> {
    return this[name]?.jsonArray?.map { element ->
        element.jsonPrimitive.content
    }.orEmpty()
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
    return if (rgb < 0x202020) {
        this or 0x004A4A4A
    } else {
        this
    }
}

private fun stableProgressFor(id: String): Float {
    val bucket = id.fold(17) { acc, char -> acc + char.code }.mod(70)
    return (bucket + 20) / 100f
}
