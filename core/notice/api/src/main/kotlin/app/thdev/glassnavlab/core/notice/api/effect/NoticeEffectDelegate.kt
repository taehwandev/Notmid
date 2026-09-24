package app.thdev.glassnavlab.core.notice.api.effect

import kotlinx.coroutines.flow.Flow

interface NoticeEffectDelegate {
    val effects: Flow<NoticeEffect>
    fun emit(effect: NoticeEffect): Boolean
}
