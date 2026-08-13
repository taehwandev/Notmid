package app.thdev.glassnavlab.core.model.notmid

data class NotmidDestination(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: NotmidNavigationIcon,
    val clips: List<NotmidClip>,
    val places: List<NotmidPlace>,
    val threads: List<NotmidThread> = emptyList(),
    val captureDraft: NotmidCaptureDraft? = null,
    val threadMessages: List<NotmidThreadMessage> = emptyList(),
)
