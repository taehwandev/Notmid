package app.thdev.glassnavlab.feature.webview.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class NotmidWebViewViewModel : ViewModel() {
    private val mutableState = MutableStateFlow(WebViewUiState())
    val state = mutableState.asStateFlow()

    private val mutableEffects = MutableSharedFlow<WebViewEffect>(extraBufferCapacity = 1)
    val effects = mutableEffects.asSharedFlow()

    fun onAction(action: WebViewAction) {
        when (action) {
            is WebViewAction.HistoryChanged -> mutableState.value = WebViewUiState(action.canGoBack)
            WebViewAction.BackRequested -> if (state.value.canGoBack) {
                mutableEffects.tryEmit(WebViewEffect.GoBack)
            }
        }
    }
}
