package app.thdev.glassnavlab.feature.notmid.common.model

fun NotmidBadge.labelText(): String {
    return when (this) {
        is NotmidBadge.Label -> text
        NotmidBadge.LiveNow -> "LIVE"
        NotmidBadge.None -> ""
    }
}
