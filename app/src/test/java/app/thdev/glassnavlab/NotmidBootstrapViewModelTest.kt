package app.thdev.glassnavlab

import app.thdev.glassnavlab.core.data.api.notmid.NotmidContentSource
import app.thdev.glassnavlab.core.data.impl.notmid.ObservableNotmidContentRepository
import app.thdev.glassnavlab.core.domain.notmid.GetNotmidDestinationsUseCase
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentRepository
import app.thdev.glassnavlab.core.model.notmid.NotmidDestination
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotmidBootstrapViewModelTest {
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

    private fun newViewModel(
        contentRepository: NotmidContentRepository = FakeContentRepository(listOf(viewModelTestDestination)),
    ): NotmidBootstrapViewModel {
        val sharedContent = ObservableNotmidContentRepository(contentRepository)
        return NotmidBootstrapViewModel(
            contentSource = NotmidContentSource.Static,
            getDestinations = GetNotmidDestinationsUseCase(sharedContent),
            contentUpdates = sharedContent,
            ioDispatcher = mainDispatcherRule.dispatcher,
        )
    }
}
