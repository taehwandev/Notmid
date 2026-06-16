package app.thdev.glassnavlab.core.feedback.api.effect

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

interface FeedbackEffectViewModel {
    val effects: Flow<FeedbackEffect>
}

interface FeedbackEffectDelegate : FeedbackEffectViewModel {
    fun emit(effect: FeedbackEffect): Boolean
}

class MutableFeedbackEffectDelegate(
    extraBufferCapacity: Int = 16,
) : FeedbackEffectDelegate {
    private val mutableEffects = MutableSharedFlow<FeedbackEffect>(
        replay = 0,
        extraBufferCapacity = extraBufferCapacity,
    )

    override val effects: Flow<FeedbackEffect> = mutableEffects.asSharedFlow()

    override fun emit(effect: FeedbackEffect): Boolean {
        return mutableEffects.tryEmit(effect)
    }
}
