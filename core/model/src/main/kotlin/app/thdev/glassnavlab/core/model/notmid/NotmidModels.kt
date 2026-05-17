package app.thdev.glassnavlab.core.model.notmid

@JvmInline
value class NotmidColor(val argb: Long)

enum class NotmidNavigationIcon {
    Feed,
    Map,
    Capture,
    Inbox,
    Profile,
}

data class NotmidDestination(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: NotmidNavigationIcon,
    val clips: List<NotmidClip>,
    val places: List<NotmidPlace>,
)

data class NotmidClip(
    val title: String,
    val description: String,
    val badge: String,
    val palette: List<NotmidColor>,
    val id: String = title.toStableRouteId(),
)

data class NotmidPlace(
    val title: String,
    val description: String,
    val metric: String,
    val palette: List<NotmidColor>,
    val heightDp: Int,
    val contentColor: NotmidColor = NotmidColors.White,
    val id: String = title.toStableRouteId(),
)

object NotmidColors {
    val White = NotmidColor(0xFFFFFFFF)
    val DarkCardContent = NotmidColor(0xFF17202A)
}

private fun String.toStableRouteId(): String {
    return trim()
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .ifBlank { "item" }
}
