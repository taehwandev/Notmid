package app.thdev.glassnavlab.feature.bootstrap

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
    val mainDispatcherRule = BootstrapMainDispatcherRule()

    @Test
    fun initLoadsContentIntoState() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = newViewModel(
            contentRepository = FakeBootstrapContentRepository(listOf(bootstrapTestDestination)),
        )

        advanceUntilIdle()

        assertEquals(
            NotmidContentUiState.Ready(
                source = NotmidContentSource.Static,
                destinations = listOf(bootstrapTestDestination),
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
    fun retryActionReloadsContentAfterFailure() = runTest(mainDispatcherRule.dispatcher) {
        var calls = 0
        val repository = object : NotmidContentRepository {
            override suspend fun destinations(): List<NotmidDestination> {
                calls += 1
                if (calls == 1) error("temporarily unavailable")
                return listOf(bootstrapTestDestination)
            }
        }
        val viewModel = newViewModel(contentRepository = repository)

        advanceUntilIdle()
        check(viewModel.state.value.content is NotmidContentUiState.Error)

        viewModel.onAction(NotmidBootstrapAction.ReloadContent)
        advanceUntilIdle()

        assertEquals(2, calls)
        assertEquals(
            NotmidContentUiState.Ready(
                source = NotmidContentSource.Static,
                destinations = listOf(bootstrapTestDestination),
            ),
            viewModel.state.value.content,
        )
    }

    private fun newViewModel(
        contentRepository: NotmidContentRepository = FakeBootstrapContentRepository(listOf(bootstrapTestDestination)),
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
