package app.thdev.glassnavlab.feature.profile

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.thdev.glassnavlab.core.navigation.notmid.NotmidRoute

@Composable
fun ProfileSettingsScreen(
    navigationStack: List<NotmidRoute>,
    isSaving: Boolean = false,
    statusMessage: String? = null,
) {
    val viewModel: ProfileSettingsViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProfileSettingsContent(
        state = state,
        routeLabel = navigationStack.joinToString(" > ") { it.deepLinkPathSegments.last() },
        listState = rememberLazyListState(),
        isSaving = isSaving,
        statusMessage = statusMessage,
        onAction = viewModel::onAction,
    )
}
