package app.thdev.glassnavlab.feature.inbox

import androidx.compose.ui.graphics.Color
import app.thdev.glassnavlab.core.designsystem.theme.NotmidColorTokens

internal fun List<InboxThreadUi>.filterFor(filter: String): List<InboxThreadUi> {
    return when (filter) {
        "Unread" -> filter { it.unreadCount > 0 }
        "Clips" -> filter { it.clip != null }
        "Places" -> filter { it.place != null }
        else -> this
    }
}

internal fun List<InboxThreadUi>.searchFor(query: String): List<InboxThreadUi> {
    val terms = query.trim().split(Regex("\\s+")).filter(String::isNotBlank)
    if (terms.isEmpty()) return this
    return filter { thread ->
        val searchable = listOfNotNull(
            thread.title,
            thread.participants,
            thread.preview,
            thread.subtitle,
            thread.clip?.title,
            thread.clip?.creatorHandle,
            thread.place?.title,
            thread.place?.address,
            thread.routePlan,
        )
        terms.all { term -> searchable.any { text -> text.contains(term, ignoreCase = true) } }
    }
}

internal fun List<InboxThreadUi>.findMatchingThread(threadId: String): InboxThreadUi? {
    return firstOrNull { thread -> thread.id == threadId }
}

internal fun InboxThreadUi.palette(): List<Color> {
    return clip?.palette?.takeIf { it.isNotEmpty() }
        ?: place?.palette?.takeIf { it.isNotEmpty() }
        ?: listOf(NotmidColorTokens.Ink, NotmidColorTokens.RouteBlue)
}
