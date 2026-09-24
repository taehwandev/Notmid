package app.thdev.glassnavlab.feature.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import app.thdev.glassnavlab.core.designsystem.theme.notmidTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.thdev.glassnavlab.core.designsystem.component.NotmidPillButton
import app.thdev.glassnavlab.core.designsystem.component.NotmidSectionHeader
import app.thdev.glassnavlab.core.designsystem.theme.NotmidColorTokens
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme

@Composable
internal fun InboxContent(
    state: InboxUiState.Ready,
    listState: LazyListState,
    onAction: (InboxAction) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NotmidColorTokens.WarmMist),
        state = listState,
        contentPadding = PaddingValues(
            start = NotmidTheme.spacing.screenHorizontal,
            top = NotmidTheme.spacing.screenTop,
            end = NotmidTheme.spacing.screenHorizontal,
            bottom = NotmidTheme.spacing.bottomNavigationPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.lg),
    ) {
        item(key = "inbox-header-${state.destinationId}") {
            NotmidSectionHeader(
                title = "receipt chats",
                subtitle = "Clip shares, place plans, and route decisions stay tied to the proof.",
                eyebrow = state.title,
            )
        }

        item(key = "inbox-stats-${state.destinationId}") {
            InboxSummary(threads = state.threads)
        }

        item(key = "inbox-filters-${state.destinationId}") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.sm)) {
                items(InboxFilters, key = { it }) { filter ->
                    NotmidPillButton(
                        label = filter,
                        selected = filter == state.selectedFilter,
                        onClick = { onAction(InboxAction.FilterSelected(filter)) },
                    )
                }
            }
        }

        items(
            items = state.visibleThreads,
            key = { thread -> "${state.destinationId}-${thread.id}" },
        ) { thread ->
            InboxThreadRow(
                thread = thread,
                onClick = {
                    onAction(InboxAction.ThreadClicked(thread.id))
                },
            )
        }
    }
}

@Preview
@Composable
private fun InboxEmptyPreview() {
    notmidTheme {
        InboxContent(InboxUiState.Ready("inbox", "Inbox", emptyList(), "Unread", emptyList()), rememberLazyListState()) {}
    }
}

@Preview
@Composable
private fun InboxReadyPreview() {
    val thread = InboxThreadUi(
        id = "preview", title = "Lunch receipts", subtitle = "Open route", preview = "Meet nearby",
        participants = "you + crew", updatedLabel = "now", unreadCount = 1,
        chatAccess = app.thdev.glassnavlab.core.model.notmid.NotmidChatAccess.AcceptedFriend,
        clip = null, place = null, routePlan = "Local plan",
    )
    notmidTheme {
        InboxContent(InboxUiState.Ready("inbox", "Inbox", listOf(thread), "All", listOf(thread)), rememberLazyListState()) {}
    }
}
