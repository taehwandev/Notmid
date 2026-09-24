package app.thdev.glassnavlab

import app.thdev.glassnavlab.core.data.notmid.NotmidContentSource
import app.thdev.glassnavlab.core.data.notmid.ObservableNotmidContentRepository
import app.thdev.glassnavlab.core.domain.notmid.GetNotmidDestinationsUseCase
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentRepository
import app.thdev.glassnavlab.core.notice.api.effect.NoticeEffect
import app.thdev.glassnavlab.core.notice.api.effect.NoticeEffectDelegate
import app.thdev.glassnavlab.core.notice.api.effect.MutableNoticeEffectDelegate
import app.thdev.glassnavlab.core.model.notmid.NotmidDestination
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotmidAppViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initLoadsContentIntoState() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = newViewModel(
            contentRepository = FakeContentRepository(listOf(viewModelTestDestination)),
        )

        advanceUntilIdle()

        assertEquals(
            NotmidContentUiState.Ready(
                source = NotmidContentSource.Static,
                destinations = listOf(viewModelTestDestination),
            ),
            viewModel.state.value.content,
        )
    }

    @Test
    fun contentCancellationIsNotMappedToErrorState() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = newViewModel(
            contentRepository = object : NotmidContentRepository {
                override suspend fun destinations(): List<NotmidDestination> {
                    throw CancellationException("content load cancelled")
                }
            },
        )

        advanceUntilIdle()

        assertEquals(NotmidContentUiState.Loading, viewModel.state.value.content)
    }

    @Test
    fun injectedFeatureNoticeStreamIsExposedWithoutAppWriteHandling() = runTest(mainDispatcherRule.dispatcher) {
        val notices = MutableNoticeEffectDelegate()
        val vm = newViewModel(uiEffects = notices)
        val effects = mutableListOf<NoticeEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.effects.toList(effects) }
        val effect = NoticeEffect.NavigateDeepLink("https://thdev.app/notmid/inbox")
        notices.emit(effect)
        advanceUntilIdle()
        assertEquals(listOf(effect), effects)
    }

    private fun newViewModel(
        contentRepository: NotmidContentRepository = FakeContentRepository(listOf(viewModelTestDestination)),
        uiEffects: NoticeEffectDelegate = MutableNoticeEffectDelegate(),
    ): NotmidAppViewModel {
        val sharedContent = ObservableNotmidContentRepository(contentRepository)
        return NotmidAppViewModel(
            contentSource = NotmidContentSource.Static,
            getDestinations = GetNotmidDestinationsUseCase(sharedContent),
            contentUpdates = sharedContent,
            uiEffects = uiEffects,
            ioDispatcher = mainDispatcherRule.dispatcher,
        )
    }
}
