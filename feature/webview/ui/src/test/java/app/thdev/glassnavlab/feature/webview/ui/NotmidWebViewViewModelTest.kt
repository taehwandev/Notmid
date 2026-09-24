package app.thdev.glassnavlab.feature.webview.ui

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotmidWebViewViewModelTest {
    @Test fun backIsEmittedOnlyWhenWebHistoryCanGoBack() = runTest {
        val viewModel = NotmidWebViewViewModel()
        val effects = mutableListOf<WebViewEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect(effects::add)
        }

        viewModel.onAction(WebViewAction.BackRequested)
        assertEquals(emptyList<WebViewEffect>(), effects)

        viewModel.onAction(WebViewAction.HistoryChanged(true))
        assertEquals(WebViewUiState(canGoBack = true), viewModel.state.value)
        viewModel.onAction(WebViewAction.BackRequested)
        assertEquals(listOf(WebViewEffect.GoBack), effects)

        viewModel.onAction(WebViewAction.HistoryChanged(false))
        viewModel.onAction(WebViewAction.BackRequested)
        assertEquals(WebViewUiState(canGoBack = false), viewModel.state.value)
        assertEquals(listOf(WebViewEffect.GoBack), effects)
    }
}
