package app.thdev.glassnavlab.feature.profile

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ProfileSettingsScreen(
    routeLabel: String,
) {
    val viewModel: ProfileSettingsViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProfileSettingsContent(
        state = state,
        routeLabel = routeLabel,
        listState = rememberLazyListState(),
        isSaving = state.isSaving,
        statusMessage = state.statusMessage,
        onAction = viewModel::onAction,
    )
}
