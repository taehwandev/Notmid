package app.thdev.glassnavlab.core.model.notmid

data class NotmidStartThreadRequest(
    val participantHandle: String,
    val body: String,
    val attachedClipId: String? = null,
    val attachedPlaceId: String? = null,
)
