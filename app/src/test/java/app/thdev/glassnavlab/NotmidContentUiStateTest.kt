package app.thdev.glassnavlab

import app.thdev.glassnavlab.core.data.notmid.ApiNotmidContentException
import app.thdev.glassnavlab.core.data.notmid.NotmidContentSource
import app.thdev.glassnavlab.core.model.notmid.NotmidClip
import app.thdev.glassnavlab.core.model.notmid.NotmidDestination
import app.thdev.glassnavlab.core.model.notmid.NotmidNavigationIcon
import app.thdev.glassnavlab.core.model.notmid.NotmidPlace
import app.thdev.glassnavlab.core.model.notmid.NotmidThread
import app.thdev.glassnavlab.core.model.notmid.NotmidThreadMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test

class NotmidContentUiStateTest {
    @Test
    fun readyOrErrorTreatsEmptyContentAsError() {
        val state = notmidContentReadyOrError(
            source = NotmidContentSource.Static,
            destinations = emptyList(),
        )

        assertEquals(
            NotmidContentUiState.Error(
                source = NotmidContentSource.Static,
                title = "No content",
                message = "Local content returned no notmid destinations.",
            ),
            state,
        )
    }

    @Test
    fun readyOrErrorPreservesLoadedDestinations() {
        val destinations = listOf(testDestination)

        val state = notmidContentReadyOrError(
            source = NotmidContentSource.Api,
            destinations = destinations,
        )

        check(state is NotmidContentUiState.Ready)
        assertSame(destinations, state.destinations)
        assertEquals(NotmidContentSource.Api, state.source)
    }

    @Test
    fun apiHttpErrorMessageDoesNotExposeBody() {
        val state = notmidContentError(
            source = NotmidContentSource.Api,
            throwable = ApiNotmidContentException.HttpStatus(
                path = "/v1/feed",
                statusCode = 500,
                body = "debug body that must stay out of UI",
            ),
        )

        assertEquals("notmid API unavailable", state.title)
        assertEquals("The notmid API returned HTTP 500 for /v1/feed.", state.message)
        assertFalse(state.message.contains("debug body"))
    }

    @Test
    fun readyContentAppendsThreadMessageReceiptToOwningDestination() {
        val state = NotmidContentUiState.Ready(
            source = NotmidContentSource.Api,
            destinations = listOf(testThreadDestination),
        )
        val message = testThreadMessage.copy(
            body = "latest receipt",
            createdAtLabel = "just now",
        )

        val updated = state.withThreadMessage(message)

        check(updated is NotmidContentUiState.Ready)
        val destination = updated.destinations.single()
        assertEquals(listOf(message), destination.threadMessages)
        assertEquals("latest receipt", destination.threads.single().preview)
        assertEquals("just now", destination.threads.single().updatedAtLabel)
    }

    @Test
    fun readyContentReplacesExistingThreadMessageWithSameId() {
        val state = NotmidContentUiState.Ready(
            source = NotmidContentSource.Api,
            destinations = listOf(
                testThreadDestination.copy(
                    threadMessages = listOf(testThreadMessage.copy(body = "old body")),
                ),
            ),
        )

        val updated = state.withThreadMessage(testThreadMessage)

        check(updated is NotmidContentUiState.Ready)
        assertEquals(listOf(testThreadMessage), updated.destinations.single().threadMessages)
    }

    @Test
    fun readyContentReplacesOwningThread() {
        val state = NotmidContentUiState.Ready(
            source = NotmidContentSource.Api,
            destinations = listOf(testThreadDestination),
        )
        val acceptedThread = testThreadDestination.threads.single().copy(
            preview = "Chat request accepted. You can message now.",
        )

        val updated = state.withThread(acceptedThread)

        check(updated is NotmidContentUiState.Ready)
        assertEquals(acceptedThread, updated.destinations.single().threads.single())
    }

    @Test
    fun readyContentInsertsStartedThreadIntoInboxAndAttachedClipDestination() {
        val state = NotmidContentUiState.Ready(
            source = NotmidContentSource.Api,
            destinations = listOf(testFeedDestinationWithClipAndPlace, testDestination),
        )
        val thread = NotmidThread(
            id = "thread-start",
            title = "chat with min.zip",
            preview = "Can we chat?",
            updatedAtLabel = "now",
            participantHandles = listOf("you", "min.zip"),
            attachedClipId = "clip-1",
            attachedPlaceId = "place-1",
        )

        val updated = state.withThread(thread)

        check(updated is NotmidContentUiState.Ready)
        assertEquals(thread, updated.destinations[0].threads.single())
        assertEquals(thread, updated.destinations[1].threads.single())
        assertEquals("clip-1", updated.destinations[1].clips.single().id)
        assertEquals("place-1", updated.destinations[1].places.single().id)
    }
}

private val testDestination = NotmidDestination(
    id = "inbox",
    title = "Inbox",
    subtitle = "Receipt chats.",
    icon = NotmidNavigationIcon.Inbox,
    clips = emptyList(),
    places = emptyList(),
)

private val testFeedDestinationWithClipAndPlace = NotmidDestination(
    id = "feed",
    title = "Feed",
    subtitle = "Short video receipts.",
    icon = NotmidNavigationIcon.Feed,
    clips = listOf(
        NotmidClip(
            id = "clip-1",
            title = "Clip",
            description = "A local clip.",
            badge = "Local",
            palette = emptyList(),
            placeId = "place-1",
        ),
    ),
    places = listOf(
        NotmidPlace(
            id = "place-1",
            title = "Place",
            description = "A local place.",
            metric = "4.8",
            palette = emptyList(),
            heightDp = 120,
        ),
    ),
)

private val testThreadDestination = NotmidDestination(
    id = "inbox",
    title = "Inbox",
    subtitle = "Receipt chats.",
    icon = NotmidNavigationIcon.Inbox,
    clips = emptyList(),
    places = emptyList(),
    threads = listOf(
        NotmidThread(
            id = "thread-1",
            title = "Thread",
            preview = "Preview",
            updatedAtLabel = "now",
            participantHandles = listOf("you"),
        ),
    ),
)

private val testThreadMessage = NotmidThreadMessage(
    id = "message-1",
    threadId = "thread-1",
    senderHandle = "you",
    body = "hello",
    createdAtLabel = "now",
    mine = true,
)
