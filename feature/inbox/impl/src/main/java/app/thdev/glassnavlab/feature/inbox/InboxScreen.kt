package app.thdev.glassnavlab.feature.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.thdev.glassnavlab.core.designsystem.component.NotmidPillButton
import app.thdev.glassnavlab.core.designsystem.component.NotmidSectionHeader
import app.thdev.glassnavlab.core.designsystem.theme.NotmidColorTokens
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme
import app.thdev.glassnavlab.feature.inbox.api.InboxRouteEvent
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidDestination

private val InboxFilters = listOf("All", "Unread", "Clips", "Places")

@Composable
fun InboxScreen(
    destination: NotmidDestination,
    listState: LazyListState,
    onRouteEvent: (InboxRouteEvent) -> Unit = {},
) {
    val threads = remember(
        destination.id,
        destination.clips,
        destination.places,
        destination.threads,
        destination.threadMessages,
    ) {
        destination.toInboxThreads()
    }
    var selectedFilter by rememberSaveable(destination.id) { mutableStateOf(InboxFilters.first()) }
    val visibleThreads = remember(threads, selectedFilter) {
        threads.filterFor(selectedFilter)
    }

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
        item(key = "inbox-header-${destination.id}") {
            NotmidSectionHeader(
                title = "receipt chats",
                subtitle = "Clip shares, place plans, and route decisions stay tied to the proof.",
                eyebrow = destination.title,
            )
        }

        item(key = "inbox-stats-${destination.id}") {
            InboxSummary(threads = threads)
        }

        item(key = "inbox-filters-${destination.id}") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.sm)) {
                items(InboxFilters, key = { it }) { filter ->
                    NotmidPillButton(
                        label = filter,
                        selected = filter == selectedFilter,
                        onClick = { selectedFilter = filter },
                    )
                }
            }
        }

        items(
            items = visibleThreads,
            key = { thread -> "${destination.id}-${thread.id}" },
        ) { thread ->
            InboxThreadRow(
                thread = thread,
                onClick = {
                    onRouteEvent(InboxRouteEvent.ChatThreadRequested(thread.id))
                },
            )
        }
    }
}
