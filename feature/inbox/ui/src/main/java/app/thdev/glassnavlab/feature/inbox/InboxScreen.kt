package app.thdev.glassnavlab.feature.inbox

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun InboxScreen() {
    val viewModel: InboxViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    when (val current = state) {
        is InboxUiState.Ready -> InboxContent(current, listState, viewModel::onAction)
        else -> InboxLoadStatus(current, viewModel::onAction)
    }
}
