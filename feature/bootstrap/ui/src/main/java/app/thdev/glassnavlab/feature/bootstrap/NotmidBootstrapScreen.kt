package app.thdev.glassnavlab.feature.bootstrap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.thdev.glassnavlab.core.model.notmid.NotmidDestination

@Composable
fun NotmidBootstrapScreen(
    content: @Composable (List<NotmidDestination>) -> Unit,
) {
    val viewModel: NotmidBootstrapViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    NotmidBootstrapContent(
        state = state,
        onAction = viewModel::onAction,
        content = content,
    )
}
