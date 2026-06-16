package app.thdev.glassnavlab.core.feedback.api.effect

import app.thdev.glassnavlab.core.feedback.api.model.FeedbackPresentation
import app.thdev.glassnavlab.core.feedback.api.model.FeedbackRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MutableFeedbackEffectDelegateTest {
    @Test
    fun emitDeliversEffectToActiveCollector() = runTest {
        val delegate = MutableFeedbackEffectDelegate()
        val effects = mutableListOf<FeedbackEffect>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            delegate.effects.take(1).toList(effects)
        }

        val emitted = delegate.emit(FeedbackEffect.ShowFeedback(testFeedback))

        assertTrue(emitted)
        val effect = effects.single() as FeedbackEffect.ShowFeedback
        assertEquals(testFeedback, effect.feedback)
    }

    @Test
    fun effectsDoNotReplayWithoutCollector() = runTest {
        val delegate = MutableFeedbackEffectDelegate()

        delegate.emit(FeedbackEffect.ShowFeedback(testFeedback))

        val replayedEffect = withTimeoutOrNull(100) {
            delegate.effects.first()
        }

        assertNull(replayedEffect)
    }

    private companion object {
        val testFeedback = FeedbackRequest(
            id = "test-feedback",
            message = "Saved.",
            presentation = FeedbackPresentation.Toast,
        )
    }
}
