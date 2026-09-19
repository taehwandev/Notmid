package app.thdev.glassnavlab.feature.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import app.thdev.glassnavlab.core.designsystem.component.NotmidButton
import app.thdev.glassnavlab.core.designsystem.component.NotmidText
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme
import app.thdev.glassnavlab.core.designsystem.theme.notmidTheme

@Composable
internal fun FeedLoadContent(
    state: FeedLoadState,
    listState: LazyListState,
    onRetry: () -> Unit,
    onClipSelected: (String) -> Unit,
) {
    when (state) {
        is FeedLoadState.Ready -> FeedContent(state.content, listState, onClipSelected)
        else -> Column(
            modifier = Modifier.fillMaxSize().padding(NotmidTheme.spacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.md, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            NotmidText(
                text = stringResource(
                    if (state == FeedLoadState.Loading) R.string.feed_loading else R.string.feed_unavailable,
                ),
            )
            if (state == FeedLoadState.Unavailable) {
                NotmidButton(text = stringResource(R.string.feed_retry), onClick = onRetry)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun FeedLoadingPreview() {
    notmidTheme { FeedLoadContent(FeedLoadState.Loading, rememberLazyListState(), {}, {}) }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun FeedUnavailablePreview() {
    notmidTheme { FeedLoadContent(FeedLoadState.Unavailable, rememberLazyListState(), {}, {}) }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun FeedEmptyPreview() {
    notmidTheme {
        FeedLoadContent(
            FeedLoadState.Ready(FeedUiState("Feed", "Receipts", null, emptyList(), emptyList())),
            rememberLazyListState(), {}, {},
        )
    }
}
