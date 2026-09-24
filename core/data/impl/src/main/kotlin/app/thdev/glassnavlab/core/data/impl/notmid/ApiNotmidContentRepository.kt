package app.thdev.glassnavlab.core.data.impl.notmid

import app.thdev.glassnavlab.core.data.api.notmid.ApiNotmidContentException
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentRepository
import app.thdev.glassnavlab.core.model.notmid.NotmidClip
import app.thdev.glassnavlab.core.model.notmid.NotmidDestination
import app.thdev.glassnavlab.core.model.notmid.NotmidNavigationIcon
import app.thdev.glassnavlab.core.model.notmid.NotmidPlace
import app.thdev.glassnavlab.core.network.notmid.NotmidApiPaths
import app.thdev.glassnavlab.core.network.notmid.NotmidHttpMethod
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkClient
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkException
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

class ApiNotmidContentRepository(
    private val client: NotmidNetworkClient,
) : NotmidContentRepository {
    override suspend fun destinations(): List<NotmidDestination> {
        val feed = getContract(NotmidApiPaths.FEED, JsonObject::toFeedResponse)
        val map = getContract(NotmidApiPaths.MAP, JsonObject::toMapResponse)
        val capture = getContract(
            NotmidApiPaths.CAPTURE_DRAFT,
            JsonObject::toCaptureDraftResponse,
        )
        val inbox = getContract(NotmidApiPaths.INBOX_THREADS, JsonObject::toInboxResponse)
        val threadDetails = inbox.threads.map { thread ->
            getContract(
                path = NotmidApiPaths.threadDetail(thread.id),
                transform = JsonObject::toThreadDetailResponse,
            )
        }
        val highlightedClipIds = map.highlightedClipIds.toSet()
        val inboxClips = (feed.clips + threadDetails.mapNotNull { detail ->
            detail.attachedClip
        }).distinctBy(NotmidClip::id)
        val inboxPlaces = (feed.places + threadDetails.mapNotNull { detail ->
            detail.attachedPlace
        }).distinctBy(NotmidPlace::id)
        val inboxThreads = threadDetails.map(ThreadDetailResponse::thread)
            .ifEmpty { inbox.threads }

        return listOf(
            NotmidDestination(
                id = "feed",
                title = "Feed",
                subtitle = "Short video receipts from the notmid API.",
                icon = NotmidNavigationIcon.Feed,
                clips = feed.clips,
                places = feed.places,
            ),
            NotmidDestination(
                id = "map",
                title = "Map",
                subtitle = "Place-first discovery backed by the notmid API.",
                icon = NotmidNavigationIcon.Map,
                clips = feed.clips.filter { it.id in highlightedClipIds },
                places = map.places,
            ),
            NotmidDestination(
                id = "capture",
                title = "Capture",
                subtitle = "Record, attach a place, and publish with API policy checks.",
                icon = NotmidNavigationIcon.Capture,
                clips = emptyList(),
                places = capture.candidatePlaces,
                captureDraft = capture.draft,
            ),
            NotmidDestination(
                id = "inbox",
                title = "Inbox",
                subtitle = "Place-aware threads from the notmid API.",
                icon = NotmidNavigationIcon.Inbox,
                clips = inboxClips,
                places = inboxPlaces,
                threads = inboxThreads,
                threadMessages = threadDetails.flatMap(ThreadDetailResponse::messages),
            ),
            NotmidDestination(
                id = "profile",
                title = "Profile",
                subtitle = "Account, privacy, and creator settings.",
                icon = NotmidNavigationIcon.Profile,
                clips = emptyList(),
                places = emptyList(),
            ),
        )
    }

    private suspend fun <T> getContract(path: String, transform: (JsonObject) -> T): T {
        val json = getJson(path)
        return try {
            transform(json)
        } catch (exception: RuntimeException) {
            throw ApiNotmidContentException.MalformedJson(
                path = path,
                cause = exception,
            )
        }
    }

    private suspend fun getJson(path: String): JsonObject {
        val response = try {
            client.execute(
                NotmidNetworkRequest(
                    method = NotmidHttpMethod.Get,
                    path = path,
                ),
            )
        } catch (exception: NotmidNetworkException) {
            throw ApiNotmidContentException.Network(
                path = path,
                error = exception.error,
            )
        }

        if (!response.isSuccessful) {
            throw ApiNotmidContentException.HttpStatus(
                path = path,
                statusCode = response.statusCode,
                body = response.body,
            )
        }

        return try {
            Json.parseToJsonElement(response.body).jsonObject
        } catch (exception: RuntimeException) {
            throw ApiNotmidContentException.MalformedJson(
                path = path,
                cause = exception,
            )
        }
    }
}
