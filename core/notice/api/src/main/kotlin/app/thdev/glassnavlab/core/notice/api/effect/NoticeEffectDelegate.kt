package app.thdev.glassnavlab.core.notice.api.effect

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

interface NoticeEffectViewModel {
    val effects: Flow<NoticeEffect>
}

interface NoticeEffectDelegate : NoticeEffectViewModel {
    fun emit(effect: NoticeEffect): Boolean
}

class MutableNoticeEffectDelegate(
    extraBufferCapacity: Int = 16,
) : NoticeEffectDelegate {
    private val mutableEffects = MutableSharedFlow<NoticeEffect>(
        replay = 0,
        extraBufferCapacity = extraBufferCapacity,
    )

    override val effects: Flow<NoticeEffect> = mutableEffects.asSharedFlow()

    override fun emit(effect: NoticeEffect): Boolean {
        return mutableEffects.tryEmit(effect)
    }
}
