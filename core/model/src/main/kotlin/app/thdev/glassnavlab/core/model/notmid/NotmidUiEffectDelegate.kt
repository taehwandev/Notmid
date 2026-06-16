package app.thdev.glassnavlab.core.model.notmid

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * ViewModel-facing source for one-shot UI side effects.
 *
 * Implementations should avoid replay so stopped UI does not receive stale
 * toast or alert effects when it starts again.
 */
interface NotmidUiEffectViewModel {
    val effects: Flow<NotmidUiEffect>
}

/**
 * Delegate injected into a ViewModel when it needs to emit UI effects.
 */
interface NotmidUiEffectDelegate : NotmidUiEffectViewModel {
    fun emit(effect: NotmidUiEffect): Boolean
}

class MutableNotmidUiEffectDelegate(
    bufferCapacity: Int = DefaultBufferCapacity,
) : NotmidUiEffectDelegate {
    private val mutableEffects = MutableSharedFlow<NotmidUiEffect>(
        replay = 0,
        extraBufferCapacity = bufferCapacity,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override val effects: Flow<NotmidUiEffect> = mutableEffects.asSharedFlow()

    override fun emit(effect: NotmidUiEffect): Boolean {
        return mutableEffects.tryEmit(effect)
    }

    private companion object {
        const val DefaultBufferCapacity = 64
    }
}
