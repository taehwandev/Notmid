package app.thdev.glassnavlab.core.data.notmid

import app.thdev.glassnavlab.core.model.notmid.NotmidCaptureMediaState
import app.thdev.glassnavlab.core.model.notmid.NotmidCaptureVisibility
import app.thdev.glassnavlab.core.model.notmid.NotmidMessageAttachment
import app.thdev.glassnavlab.core.network.assertions.QueuedNetworkFailure
import app.thdev.glassnavlab.core.network.assertions.QueuedNetworkResponse
import app.thdev.glassnavlab.core.network.assertions.RecordingNotmidNetworkClient
import app.thdev.glassnavlab.core.network.notmid.NotmidApiPaths
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkError
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkErrorCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiNotmidContentRepositoryTest {
    @Test
    fun destinationsMapsFeedMapCaptureAndInboxResponses() {
        val client = RecordingNotmidNetworkClient(
            QueuedNetworkResponse(feedJson),
            QueuedNetworkResponse(mapJson),
            QueuedNetworkResponse(captureDraftJson),
            QueuedNetworkResponse(inboxJson),
            QueuedNetworkResponse(threadDetailJson),
        )
        val repository = ApiNotmidContentRepository(client)

        val destinations = runSuspend { repository.destinations() }

        assertEquals(
            listOf(
                NotmidApiPaths.FEED,
                NotmidApiPaths.MAP,
                NotmidApiPaths.CAPTURE_DRAFT,
                NotmidApiPaths.INBOX_THREADS,
                NotmidApiPaths.threadDetail("tonight-seongsu"),
            ),
            client.requests.map { request -> request.path },
        )
        assertEquals(listOf("feed", "map", "capture", "inbox", "profile"), destinations.map { it.id })

        val feed = destinations.first { it.id == "feed" }
        assertEquals("latte line was worth it", feed.clips.single().title)
        assertEquals("Neon Yard", feed.places.single().title)
        assertEquals(37.5446, feed.places.single().coordinate?.latitude ?: 0.0, 0.0001)

        val map = destinations.first { it.id == "map" }
        assertEquals("latte-line-was-worth-it", map.clips.single().id)
        assertEquals("Neon Yard", map.places.single().title)

        val capture = destinations.first { it.id == "capture" }
        assertEquals("draft-local-receipt", capture.captureDraft?.id)
        assertEquals(NotmidCaptureVisibility.Public, capture.captureDraft?.visibility)
        assertEquals(NotmidCaptureMediaState.LocalPreview, capture.captureDraft?.mediaState)
        assertEquals("Neon Yard", capture.places.single().title)

        val inbox = destinations.first { it.id == "inbox" }
        assertEquals("tonight-seongsu", inbox.threads.single().id)
        assertEquals("neon-yard", inbox.threads.single().attachedPlaceId)
        assertTrue(inbox.threads.single().chatAccess.canSendMessage)
        assertEquals("msg-tonight-seongsu-1", inbox.threadMessages.first().id)
        assertEquals("min.zip", inbox.threadMessages.first().senderHandle)
        assertEquals(
            "latte-line-was-worth-it",
            (inbox.threadMessages.first().attachment as NotmidMessageAttachment.Clip).clipId,
        )
    }

    @Test
    fun httpFailureThrowsTypedException() {
        val repository = ApiNotmidContentRepository(
            RecordingNotmidNetworkClient(
                QueuedNetworkResponse("""{"error":{"code":"broken"}}""", statusCode = 500),
            ),
        )

        val exception = assertThrows(ApiNotmidContentException.HttpStatus::class.java) {
            runSuspend { repository.destinations() }
        }

        assertEquals(NotmidApiPaths.FEED, exception.path)
        assertEquals(500, exception.statusCode)
        assertTrue(exception.body.contains("broken"))
    }

    @Test
    fun transportFailureThrowsTypedException() {
        val repository = ApiNotmidContentRepository(
            RecordingNotmidNetworkClient(
                QueuedNetworkFailure(
                    NotmidNetworkError(
                        code = NotmidNetworkErrorCode.Transport,
                        message = "offline",
                    ),
                ),
            ),
        )

        val exception = assertThrows(ApiNotmidContentException.Network::class.java) {
            runSuspend { repository.destinations() }
        }

        assertEquals(NotmidApiPaths.FEED, exception.path)
        assertEquals(NotmidNetworkErrorCode.Transport, exception.error.code)
    }

    @Test
    fun malformedJsonThrowsTypedException() {
        val repository = ApiNotmidContentRepository(
            RecordingNotmidNetworkClient(
                QueuedNetworkResponse("""{"clips":[]}"""),
            ),
        )

        val exception = assertThrows(ApiNotmidContentException.MalformedJson::class.java) {
            runSuspend { repository.destinations() }
        }

        assertEquals(NotmidApiPaths.FEED, exception.path)
    }
}

private val feedJson = """
{
  "source": "fixture",
  "generatedAt": "2026-05-17T00:00:00.000Z",
  "clips": [
    {
      "id": "latte-line-was-worth-it",
      "title": "latte line was worth it",
      "caption": "Foam art, fast seats, and the alley playlist is doing work.",
      "creatorHandle": "min.zip",
      "placeId": "neon-yard",
      "moodTags": ["live rn", "date safe"],
      "capturedAtLabel": "12m ago",
      "coverImageUrl": "https://example.test/clip.jpg",
      "metrics": {
        "likes": 1280,
        "saves": 420,
        "comments": 38,
        "distanceLabel": "1.2 km"
      }
    }
  ],
  "places": [
    {
      "id": "neon-yard",
      "name": "Neon Yard",
      "neighborhood": "Seongsu",
      "category": "night coffee",
      "address": "Seongsu-dong, Seoul",
      "lat": 37.5446,
      "lng": 127.0557,
      "openNow": true,
      "score": 92,
      "receiptCount": 184,
      "coverImageUrl": "https://example.test/place.jpg"
    }
  ]
}
""".trimIndent()

private val mapJson = """
{
  "source": "fixture",
  "generatedAt": "2026-05-17T00:00:00.000Z",
  "places": [
    {
      "id": "neon-yard",
      "name": "Neon Yard",
      "neighborhood": "Seongsu",
      "category": "night coffee",
      "address": "Seongsu-dong, Seoul",
      "lat": 37.5446,
      "lng": 127.0557,
      "openNow": true,
      "score": 92,
      "receiptCount": 184,
      "coverImageUrl": "https://example.test/place.jpg"
    }
  ],
  "highlightedClipIds": ["latte-line-was-worth-it"]
}
""".trimIndent()

private val captureDraftJson = """
{
  "source": "fixture",
  "generatedAt": "2026-05-17T00:00:00.000Z",
  "draft": {
    "id": "draft-local-receipt",
    "caption": "Steam on the counter.",
    "placeId": "neon-yard",
    "moodTags": ["line proof", "worth it"],
    "visibility": "public",
    "mediaState": "local-preview"
  },
  "candidatePlaces": [
    {
      "id": "neon-yard",
      "name": "Neon Yard",
      "neighborhood": "Seongsu",
      "category": "night coffee",
      "address": "Seongsu-dong, Seoul",
      "lat": 37.5446,
      "lng": 127.0557,
      "openNow": true,
      "score": 92,
      "receiptCount": 184,
      "coverImageUrl": "https://example.test/place.jpg"
    }
  ]
}
""".trimIndent()

private val inboxJson = """
{
  "source": "fixture",
  "generatedAt": "2026-05-17T00:00:00.000Z",
  "threads": [
    {
      "id": "tonight-seongsu",
      "title": "tonight in seongsu?",
      "preview": "Neon Yard looks not mid. meet after 8?",
      "updatedAtLabel": "now",
      "participantHandles": ["min.zip", "you"],
      "attachedPlaceId": "neon-yard",
      "attachedClipId": "latte-line-was-worth-it",
      "unreadCount": 3
    }
  ]
}
""".trimIndent()

private val threadDetailJson = """
{
  "source": "fixture",
  "generatedAt": "2026-05-17T00:00:00.000Z",
  "thread": {
    "id": "tonight-seongsu",
    "title": "tonight in seongsu?",
    "preview": "Neon Yard looks not mid. meet after 8?",
    "updatedAtLabel": "now",
    "participantHandles": ["min.zip", "you"],
    "attachedPlaceId": "neon-yard",
    "attachedClipId": "latte-line-was-worth-it",
    "unreadCount": 3
  },
  "messages": [
    {
      "id": "msg-tonight-seongsu-1",
      "threadId": "tonight-seongsu",
      "senderHandle": "min.zip",
      "body": "Neon Yard looks current. Is the line still moving?",
      "createdAtLabel": "12:08",
      "mine": false,
      "attachment": {
        "type": "clip",
        "clipId": "latte-line-was-worth-it"
      }
    }
  ],
  "attachedClip": {
    "id": "latte-line-was-worth-it",
    "title": "latte line was worth it",
    "caption": "Foam art, fast seats, and the alley playlist is doing work.",
    "creatorHandle": "min.zip",
    "placeId": "neon-yard",
    "moodTags": ["live rn", "date safe"],
    "capturedAtLabel": "12m ago",
    "coverImageUrl": "https://example.test/clip.jpg",
    "metrics": {
      "likes": 1280,
      "saves": 420,
      "comments": 38,
      "distanceLabel": "1.2 km"
    }
  },
  "attachedPlace": {
    "id": "neon-yard",
    "name": "Neon Yard",
    "neighborhood": "Seongsu",
    "category": "night coffee",
    "address": "Seongsu-dong, Seoul",
    "lat": 37.5446,
    "lng": 127.0557,
    "openNow": true,
    "score": 92,
    "receiptCount": 184,
    "coverImageUrl": "https://example.test/place.jpg"
  }
}
""".trimIndent()
