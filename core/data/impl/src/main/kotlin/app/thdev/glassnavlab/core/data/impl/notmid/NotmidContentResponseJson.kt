package app.thdev.glassnavlab.core.data.impl.notmid

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Decoders from notmid content API responses to this module's response shapes.
 */
internal fun JsonObject.toFeedResponse(): FeedResponse {
    return FeedResponse(
        clips = requiredArray("clips").map(JsonElement::toClipItem),
        places = requiredArray("places").map(JsonElement::toPlaceItem),
    )
}

internal fun JsonObject.toMapResponse(): MapResponse {
    return MapResponse(
        places = requiredArray("places").map(JsonElement::toPlaceItem),
        highlightedClipIds = requiredArray("highlightedClipIds").map { element ->
            element.jsonPrimitive.content
        },
    )
}

internal fun JsonObject.toCaptureDraftResponse(): CaptureDraftResponse {
    return CaptureDraftResponse(
        draft = requiredObject("draft").toCaptureDraft(),
        candidatePlaces = requiredArray("candidatePlaces").map(JsonElement::toPlaceItem),
    )
}

internal fun JsonObject.toInboxResponse(): InboxResponse {
    return InboxResponse(
        threads = requiredArray("threads").map(JsonElement::toThreadItem),
    )
}

internal fun JsonObject.toThreadDetailResponse(): ThreadDetailResponse {
    return ThreadDetailResponse(
        thread = requiredObject("thread").toThread(),
        messages = requiredArray("messages").map(JsonElement::toThreadMessageItem),
        attachedClip = optionalObject("attachedClip")?.toClip(),
        attachedPlace = optionalObject("attachedPlace")?.toPlace(),
    )
}

private fun JsonElement.toClipItem() = jsonObject.toClip()

private fun JsonElement.toPlaceItem() = jsonObject.toPlace()

private fun JsonElement.toThreadItem() = jsonObject.toThread()

private fun JsonElement.toThreadMessageItem() = jsonObject.toThreadMessage()
