package app.thdev.glassnavlab.feature.bootstrap

import androidx.compose.runtime.Composable
import app.thdev.glassnavlab.core.model.notmid.NotmidDestination

@Composable
internal fun NotmidBootstrapContent(
    state: NotmidBootstrapUiState,
    onAction: (NotmidBootstrapAction) -> Unit,
    content: @Composable (List<NotmidDestination>) -> Unit,
) {
    when (val contentState = state.content) {
        NotmidContentUiState.Loading -> {
            NotmidBootstrapLoadingContent(sourceLabel = state.contentSource.label)
        }

        is NotmidContentUiState.Error -> {
            NotmidBootstrapErrorContent(
                title = contentState.title,
                message = contentState.message,
                onRetry = { onAction(NotmidBootstrapAction.ReloadContent) },
            )
        }

        is NotmidContentUiState.Ready -> content(contentState.destinations)
    }
}
