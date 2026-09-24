package app.thdev.glassnavlab

import app.thdev.glassnavlab.core.data.notmid.ApiNotmidContentException
import app.thdev.glassnavlab.core.data.notmid.NotmidContentSource
import app.thdev.glassnavlab.core.model.notmid.NotmidDestination
import app.thdev.glassnavlab.core.model.notmid.NotmidNavigationIcon
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

}

private val testDestination = NotmidDestination(
    id = "inbox",
    title = "Inbox",
    subtitle = "Receipt chats.",
    icon = NotmidNavigationIcon.Inbox,
    clips = emptyList(),
    places = emptyList(),
)
