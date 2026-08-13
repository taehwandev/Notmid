package app.thdev.glassnavlab.core.model.notmid

sealed interface NotmidMessageAttachment {
    data class Clip(val clipId: String) : NotmidMessageAttachment
    data class Place(val placeId: String) : NotmidMessageAttachment
    data class Route(
        val title: String,
        val placeIds: List<String>,
    ) : NotmidMessageAttachment
}
