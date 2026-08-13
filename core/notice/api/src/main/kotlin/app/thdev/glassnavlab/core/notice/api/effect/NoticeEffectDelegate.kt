package app.thdev.glassnavlab.core.notice.api.effect

interface NoticeEffectDelegate : NoticeEffectViewModel {
    fun emit(effect: NoticeEffect): Boolean
}
