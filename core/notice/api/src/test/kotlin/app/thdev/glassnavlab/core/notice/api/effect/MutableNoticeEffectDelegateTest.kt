package app.thdev.glassnavlab.core.notice.api.effect

import app.thdev.glassnavlab.core.notice.api.model.NoticePresentation
import app.thdev.glassnavlab.core.notice.api.model.NoticeRequest
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
class MutableNoticeEffectDelegateTest {
    @Test
    fun emitDeliversEffectToActiveCollector() = runTest {
        val delegate = MutableNoticeEffectDelegate()
        val effects = mutableListOf<NoticeEffect>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            delegate.effects.take(1).toList(effects)
        }

        val emitted = delegate.emit(NoticeEffect.ShowNotice(testNotice))

        assertTrue(emitted)
        val effect = effects.single() as NoticeEffect.ShowNotice
        assertEquals(testNotice, effect.notice)
    }

    @Test
    fun effectsDoNotReplayWithoutCollector() = runTest {
        val delegate = MutableNoticeEffectDelegate()

        delegate.emit(NoticeEffect.ShowNotice(testNotice))

        val replayedEffect = withTimeoutOrNull(100) {
            delegate.effects.first()
        }

        assertNull(replayedEffect)
    }

    private companion object {
        val testNotice = NoticeRequest(
            id = "test-notice",
            message = "Saved.",
            presentation = NoticePresentation.Toast,
        )
    }
}
