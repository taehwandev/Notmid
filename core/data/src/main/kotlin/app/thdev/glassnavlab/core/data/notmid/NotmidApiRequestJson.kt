package app.thdev.glassnavlab.core.data.notmid

import app.thdev.glassnavlab.core.model.notmid.NotmidCapturePublishRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidMessageAttachment
import app.thdev.glassnavlab.core.model.notmid.NotmidProfileSettingsUpdateRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidSendThreadMessageRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidStartThreadRequest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * Encoders from protected-write requests to notmid API JSON bodies.
 */
internal fun NotmidCapturePublishRequest.toJsonBody(): String {
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

internal fun NotmidSendThreadMessageRequest.toJsonBody(): String {
    return buildJsonObject {
        put("body", body)
        attachment?.let { value ->
            put("attachment", value.toJsonObject())
        }
    }.toString()
}

internal fun NotmidStartThreadRequest.toJsonBody(): String {
    return buildJsonObject {
        put("participantHandle", participantHandle)
        put("body", body)
        attachedClipId?.let { clipId -> put("attachedClipId", clipId) }
        attachedPlaceId?.let { placeId -> put("attachedPlaceId", placeId) }
    }.toString()
}

internal fun NotmidProfileSettingsUpdateRequest.toJsonBody(): String {
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
