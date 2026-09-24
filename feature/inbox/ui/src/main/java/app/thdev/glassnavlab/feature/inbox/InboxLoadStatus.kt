package app.thdev.glassnavlab.feature.inbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import app.thdev.glassnavlab.core.designsystem.theme.notmidTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import app.thdev.glassnavlab.core.designsystem.component.NotmidPillButton
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme

@Composable
internal fun InboxLoadStatus(state: InboxUiState, onAction: (InboxAction) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(if (state == InboxUiState.Loading) R.string.inbox_loading else R.string.inbox_unavailable))
        if (state == InboxUiState.Unavailable) {
            NotmidPillButton(
                label = stringResource(R.string.inbox_retry),
                selected = false,
                onClick = { onAction(InboxAction.Retry) },
            )
        }
    }
}

@Preview
@Composable
private fun InboxLoadingPreview() { notmidTheme { InboxLoadStatus(InboxUiState.Loading) {} } }

@Preview
@Composable
private fun InboxUnavailablePreview() { notmidTheme { InboxLoadStatus(InboxUiState.Unavailable) {} } }
