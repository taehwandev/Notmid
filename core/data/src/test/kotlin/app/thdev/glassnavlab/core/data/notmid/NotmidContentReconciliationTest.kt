package app.thdev.glassnavlab.core.data.notmid

import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteResult
import app.thdev.glassnavlab.core.model.notmid.NotmidClip
import app.thdev.glassnavlab.core.model.notmid.NotmidDestination
import app.thdev.glassnavlab.core.model.notmid.NotmidNavigationIcon
import app.thdev.glassnavlab.core.model.notmid.NotmidPlace
import app.thdev.glassnavlab.core.model.notmid.NotmidThread
import app.thdev.glassnavlab.core.model.notmid.NotmidThreadMessage
import org.junit.Assert.assertEquals
import org.junit.Test

class NotmidContentReconciliationTest {
    @Test
    fun readyContentAppendsThreadMessageReceiptToOwningDestination() {
        val destinations = listOf(testThreadDestination)
        val message = testThreadMessage.copy(
            body = "latest receipt",
            createdAtLabel = "just now",
        )

        val updated = destinations.withWriteResult(NotmidProtectedWriteResult.MessageSent(message))

        val destination = updated.single()
        assertEquals(listOf(message), destination.threadMessages)
        assertEquals("latest receipt", destination.threads.single().preview)
        assertEquals("just now", destination.threads.single().updatedAtLabel)
    }

    @Test
    fun readyContentReplacesExistingThreadMessageWithSameId() {
        val destinations = listOf(
            testThreadDestination.copy(threadMessages = listOf(testThreadMessage.copy(body = "old body"))),
        )

        val updated = destinations.withWriteResult(NotmidProtectedWriteResult.MessageSent(testThreadMessage))

        assertEquals(listOf(testThreadMessage), updated.single().threadMessages)
    }

    @Test
    fun readyContentReplacesOwningThread() {
        val destinations = listOf(testThreadDestination)
        val acceptedThread = testThreadDestination.threads.single().copy(
            preview = "Chat request accepted. You can message now.",
        )

        val updated = destinations.withWriteResult(NotmidProtectedWriteResult.ThreadUpdated(acceptedThread))

        assertEquals(acceptedThread, updated.single().threads.single())
    }

    @Test
    fun readyContentInsertsStartedThreadIntoInboxAndAttachedClipDestination() {
        val destinations = listOf(testFeedDestinationWithClipAndPlace, testDestination)
        val thread = NotmidThread(
            id = "thread-start",
            title = "chat with min.zip",
            preview = "Can we chat?",
            updatedAtLabel = "now",
            participantHandles = listOf("you", "min.zip"),
            attachedClipId = "clip-1",
            attachedPlaceId = "place-1",
        )

        val updated = destinations.withWriteResult(NotmidProtectedWriteResult.ThreadStarted(thread, null))

        assertEquals(thread, updated[0].threads.single())
        assertEquals(thread, updated[1].threads.single())
        assertEquals("clip-1", updated[1].clips.single().id)
        assertEquals("place-1", updated[1].places.single().id)
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
