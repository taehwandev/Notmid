package app.thdev.glassnavlab.feature.notmid.common.model

sealed interface NotmidThreadMessageAttachment {
    data class Clip(val clipId: String) : NotmidThreadMessageAttachment
    data class Place(val placeId: String) : NotmidThreadMessageAttachment
    data class Route(
        val title: String,
        val placeIds: List<String>,
    ) : NotmidThreadMessageAttachment
}
