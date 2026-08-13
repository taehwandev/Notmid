package app.thdev.glassnavlab.feature.notmid.common.model

sealed interface NotmidBadge {
    object LiveNow : NotmidBadge
    data class Label(val text: String) : NotmidBadge
    object None : NotmidBadge
}
