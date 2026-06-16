package app.thdev.glassnavlab.core.data.notmid

import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteFailure
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteException
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthMode
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthProvider
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthSession
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthUser
import app.thdev.glassnavlab.core.model.notmid.NotmidCaptureModerationStatus
import app.thdev.glassnavlab.core.model.notmid.NotmidCapturePublishRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidCaptureVisibility
import app.thdev.glassnavlab.core.model.notmid.NotmidChatInviteDecision
import app.thdev.glassnavlab.core.model.notmid.NotmidProfileSettingsUpdateRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidSendThreadMessageRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidStartThreadRequest
import app.thdev.glassnavlab.core.network.notmid.NotmidApiPaths
import app.thdev.glassnavlab.core.network.notmid.NotmidHttpMethod
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkClient
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkError
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkErrorCode
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkException
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkRequest
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiNotmidProtectedWriteRepositoryTest {
    @Test
    fun publishCaptureSendsBearerTokenAndParsesReceipt() {
        val client = RecordingNotmidNetworkClient(
            responses = mapOf(NotmidApiPaths.CAPTURE_PUBLISH to success(publishResponseJson)),
        )
        val repository = ApiNotmidProtectedWriteRepository(client)

        val receipt = runSuspend {
            repository.publishCapture(
                authState = signedInAuthState,
                request = NotmidCapturePublishRequest(
                    draftId = "draft-1",
                    caption = "Steam on the counter",
                    placeId = "neon-yard",
                    moodTags = listOf("line proof", "worth it"),
                    visibility = NotmidCaptureVisibility.Friends,
                ),
            )
        }

        assertEquals("receipt-draft-1", receipt.clip.id)
        assertEquals(NotmidCaptureModerationStatus.Queued, receipt.moderationStatus)

        val request = client.requests.single()
        assertEquals(NotmidHttpMethod.Post, request.method)
        assertEquals(NotmidApiPaths.CAPTURE_PUBLISH, request.path)
        assertEquals("Bearer android-access-token", request.headers["authorization"])
        assertTrue(request.body.orEmpty().contains("\"visibility\":\"friends\""))
    }

    @Test
    fun saveClipUsesEncodedPathAndBearerToken() {
        val path = NotmidApiPaths.clipSave("clip/one")
        val client = RecordingNotmidNetworkClient(responses = mapOf(path to success(saveClipResponseJson)))
        val repository = ApiNotmidProtectedWriteRepository(client)

        val receipt = runSuspend { repository.saveClip(signedInAuthState, "clip/one") }

        assertTrue(receipt.saved)
        assertEquals("clip/one", receipt.clip.id)
        assertEquals(path, client.requests.single().path)
        assertEquals("Bearer android-access-token", client.requests.single().headers["authorization"])
    }

    @Test
    fun sendThreadMessagePostsBodyAndParsesMessage() {
        val path = NotmidApiPaths.threadMessages("thread one")
        val client = RecordingNotmidNetworkClient(
            responses = mapOf(path to success(sendMessageResponseJson)),
        )
        val repository = ApiNotmidProtectedWriteRepository(client)

        val receipt = runSuspend {
            repository.sendThreadMessage(
                authState = signedInAuthState,
                threadId = "thread one",
                request = NotmidSendThreadMessageRequest(body = "Still has seats?"),
            )
        }

        assertEquals("msg-1", receipt.message.id)
        assertEquals("Still has seats?", client.requests.single().body?.let(::bodyValue))
    }

    @Test
    fun startThreadPostsBodyAndParsesThreadReceipt() {
        val client = RecordingNotmidNetworkClient(
            responses = mapOf(NotmidApiPaths.INBOX_THREADS to success(startThreadResponseJson)),
        )
        val repository = ApiNotmidProtectedWriteRepository(client)

        val receipt = runSuspend {
            repository.startThread(
                authState = signedInAuthState,
                request = NotmidStartThreadRequest(
                    participantHandle = "min.zip",
                    body = "Can we chat about this clip?",
                    attachedClipId = "clip-thread",
                    attachedPlaceId = "no-dead-end-share",
                ),
            )
        }

        assertEquals("thread-min-zip-you-local-clip-thread", receipt.thread.id)
        assertEquals("msg-thread-min-zip-you-local-clip-thread-start", receipt.message?.id)

        val request = client.requests.single()
        assertEquals(NotmidHttpMethod.Post, request.method)
        assertEquals(NotmidApiPaths.INBOX_THREADS, request.path)
        assertTrue(request.body.orEmpty().contains("\"participantHandle\":\"min.zip\""))
        assertEquals("Can we chat about this clip?", request.body?.let(::bodyValue))
    }

    @Test
    fun acceptThreadInvitePostsPathAndParsesThread() {
        val path = NotmidApiPaths.threadInviteAccept("thread one")
        val client = RecordingNotmidNetworkClient(
            responses = mapOf(path to success(chatInviteResponseJson)),
        )
        val repository = ApiNotmidProtectedWriteRepository(client)

        val receipt = runSuspend {
            repository.respondThreadInvite(
                authState = signedInAuthState,
                threadId = "thread one",
                decision = NotmidChatInviteDecision.Accept,
            )
        }

        assertEquals("thread one", receipt.thread.id)
        assertTrue(receipt.thread.chatAccess.canSendMessage)
        assertEquals(path, client.requests.single().path)
        assertEquals("{}", client.requests.single().body)
    }

    @Test
    fun updateProfileSettingsPatchesBodyAndUpdatesUser() {
        val client = RecordingNotmidNetworkClient(
            responses = mapOf(NotmidApiPaths.PROFILE_SETTINGS to success(profileSettingsResponseJson)),
        )
        val repository = ApiNotmidProtectedWriteRepository(client)

        val receipt = runSuspend {
            repository.updateProfileSettings(
                authState = signedInAuthState,
                request = NotmidProfileSettingsUpdateRequest(
                    displayName = "Min Zip",
                    homeNeighborhood = "Seongsu",
                ),
            )
        }

        assertTrue(receipt.updated)
        assertEquals("Min Zip", receipt.settings.user.displayName)

        val request = client.requests.single()
        assertEquals(NotmidHttpMethod.Patch, request.method)
        assertEquals(NotmidApiPaths.PROFILE_SETTINGS, request.path)
        assertTrue(request.body.orEmpty().contains("\"homeNeighborhood\":\"Seongsu\""))
    }

    @Test
    fun missingAuthDoesNotCallNetwork() {
        val client = RecordingNotmidNetworkClient(responses = emptyMap())
        val repository = ApiNotmidProtectedWriteRepository(client)

        val exception = assertThrows(NotmidProtectedWriteException::class.java) {
            runSuspend { repository.saveClip(signedOutAuthState, "clip-1") }
        }

        assertTrue(exception.failure is NotmidProtectedWriteFailure.MissingAuth)
        assertTrue(client.requests.isEmpty())
    }

    @Test
    fun httpErrorReturnsTypedFailure() {
        val client = RecordingNotmidNetworkClient(
            responses = mapOf(
                NotmidApiPaths.CAPTURE_PUBLISH to success(
                    body = """{"error":{"code":"auth_required"}}""",
                    statusCode = 401,
                ),
            ),
        )
        val repository = ApiNotmidProtectedWriteRepository(client)

        val exception = assertThrows(NotmidProtectedWriteException::class.java) {
            runSuspend { repository.publishCapture(signedInAuthState, captureRequest) }
        }

        val failure = exception.failure
        assertTrue(failure is NotmidProtectedWriteFailure.HttpStatus)
        assertEquals(401, (failure as NotmidProtectedWriteFailure.HttpStatus).statusCode)
    }

    @Test
    fun httpBusinessErrorPreservesServerCodeAndMessage() {
        val path = NotmidApiPaths.threadMessages("thread one")
        val client = RecordingNotmidNetworkClient(
            responses = mapOf(
                path to success(
                    body = """
                    {
                      "error": {
                        "code": "chat_invite_required",
                        "message": "Accept the chat request before sending a message."
                      }
                    }
                    """.trimIndent(),
                    statusCode = 403,
                ),
            ),
        )
        val repository = ApiNotmidProtectedWriteRepository(client)

        val exception = assertThrows(NotmidProtectedWriteException::class.java) {
            runSuspend {
                repository.sendThreadMessage(
                    authState = signedInAuthState,
                    threadId = "thread one",
                    request = NotmidSendThreadMessageRequest(body = "hello"),
                )
            }
        }

        val failure = exception.failure
        assertTrue(failure is NotmidProtectedWriteFailure.InvalidRequest)
        failure as NotmidProtectedWriteFailure.InvalidRequest
        assertEquals("chat_invite_required", failure.code)
        assertEquals("Accept the chat request before sending a message.", failure.message)
    }

    @Test
    fun transportErrorReturnsTypedFailure() {
        val client = RecordingNotmidNetworkClient(
            responses = mapOf(
                NotmidApiPaths.CAPTURE_PUBLISH to networkFailure(
                    NotmidNetworkError(
                        code = NotmidNetworkErrorCode.Transport,
                        message = "offline",
                    ),
                ),
            ),
        )
        val repository = ApiNotmidProtectedWriteRepository(client)

        val exception = assertThrows(NotmidProtectedWriteException::class.java) {
            runSuspend { repository.publishCapture(signedInAuthState, captureRequest) }
        }

        val failure = exception.failure
        assertTrue(failure is NotmidProtectedWriteFailure.Network)
        assertEquals("offline", (failure as NotmidProtectedWriteFailure.Network).message)
    }

    @Test
    fun malformedSuccessReturnsTypedFailure() {
        val client = RecordingNotmidNetworkClient(
            responses = mapOf(NotmidApiPaths.CAPTURE_PUBLISH to success("""{"clip":{}}""")),
        )
        val repository = ApiNotmidProtectedWriteRepository(client)

        val exception = assertThrows(NotmidProtectedWriteException::class.java) {
            runSuspend { repository.publishCapture(signedInAuthState, captureRequest) }
        }

        val failure = exception.failure
        assertTrue(failure is NotmidProtectedWriteFailure.MalformedResponse)
    }
}

private class RecordingNotmidNetworkClient(
    private val responses: Map<String, Result<NotmidNetworkResponse>>,
) : NotmidNetworkClient {
    val requests = mutableListOf<NotmidNetworkRequest>()

    override suspend fun execute(request: NotmidNetworkRequest): NotmidNetworkResponse {
        requests += request
        val response = responses[request.path] ?: error("Unexpected path: ${request.path}")
        return response.getOrThrow()
    }
}

private fun success(
    body: String,
    statusCode: Int = 200,
): Result<NotmidNetworkResponse> {
    return Result.success(
        NotmidNetworkResponse(
            statusCode = statusCode,
            body = body,
            headers = emptyMap(),
        ),
    )
}

private fun networkFailure(
    error: NotmidNetworkError,
): Result<NotmidNetworkResponse> {
    return Result.failure(NotmidNetworkException(error))
}

private fun bodyValue(body: String): String {
    return Regex("\"body\":\"([^\"]+)\"").find(body)?.groupValues?.get(1).orEmpty()
}

private val signedInAuthState = NotmidAuthState(
    mode = NotmidAuthMode.Firebase,
    session = NotmidAuthSession(
        accessToken = "android-access-token",
        provider = NotmidAuthProvider.Anonymous,
        expiresAt = "2026-05-30T01:00:00.000Z",
        user = NotmidAuthUser(
            id = "firebase:uid-1",
            handle = "min.zip",
            displayName = "Min",
            homeNeighborhood = "Hapjeong",
            avatarImageUrl = "",
            roles = listOf("creator"),
        ),
    ),
    requiredActions = emptyList(),
)

private val signedOutAuthState = signedInAuthState.copy(session = null)

private val captureRequest = NotmidCapturePublishRequest(
    draftId = "draft-1",
    caption = "Steam on the counter",
    placeId = "neon-yard",
    moodTags = listOf("line proof"),
    visibility = NotmidCaptureVisibility.Public,
)

private val clipJson = """
{
  "id": "receipt-draft-1",
  "title": "Steam on the counter",
  "caption": "Steam on the counter",
  "creatorHandle": "min.zip",
  "placeId": "neon-yard",
  "moodTags": ["line proof"],
  "capturedAtLabel": "now",
  "coverImageUrl": "https://example.test/clip.jpg",
  "metrics": {
    "likes": 0,
    "saves": 1,
    "comments": 0,
    "distanceLabel": "new"
  }
}
""".trimIndent()

private val publishResponseJson = """
{
  "source": "api",
  "generatedAt": "2026-05-30T00:00:00.000Z",
  "clip": $clipJson,
  "moderationStatus": "queued"
}
""".trimIndent()

private val saveClipResponseJson = """
{
  "source": "api",
  "generatedAt": "2026-05-30T00:00:00.000Z",
  "clip": ${clipJson.replace("receipt-draft-1", "clip/one")},
  "saved": true
}
""".trimIndent()

private val sendMessageResponseJson = """
{
  "source": "api",
  "generatedAt": "2026-05-30T00:00:00.000Z",
  "message": {
    "id": "msg-1",
    "threadId": "thread one",
    "senderHandle": "min.zip",
    "body": "Still has seats?",
    "createdAtLabel": "now",
    "mine": true
  }
}
""".trimIndent()

private val startThreadResponseJson = """
{
  "source": "api",
  "generatedAt": "2026-05-30T00:00:00.000Z",
  "thread": {
    "id": "thread-min-zip-you-local-clip-thread",
    "title": "chat with min.zip",
    "preview": "Can we chat about this clip?",
    "updatedAtLabel": "now",
    "participantHandles": ["min.zip", "you.local"],
    "attachedPlaceId": "no-dead-end-share",
    "attachedClipId": "clip-thread",
    "unreadCount": 0,
    "chatAccess": {
      "relationship": "friend",
      "inviteStatus": "accepted",
      "canSendMessage": true,
      "canAcceptInvite": false,
      "canRejectInvite": false,
      "reasonLabel": "Friends can chat immediately."
    }
  },
  "message": {
    "id": "msg-thread-min-zip-you-local-clip-thread-start",
    "threadId": "thread-min-zip-you-local-clip-thread",
    "senderHandle": "you.local",
    "body": "Can we chat about this clip?",
    "createdAtLabel": "now",
    "mine": true,
    "attachment": {
      "type": "clip",
      "clipId": "clip-thread"
    }
  }
}
""".trimIndent()

private val chatInviteResponseJson = """
{
  "source": "api",
  "generatedAt": "2026-05-30T00:00:00.000Z",
  "thread": {
    "id": "thread one",
    "title": "rain route",
    "preview": "Chat request accepted. You can message now.",
    "updatedAtLabel": "now",
    "participantHandles": ["receipt.han", "you"],
    "attachedPlaceId": "neon-yard",
    "attachedClipId": "latte-line-was-worth-it",
    "unreadCount": 0,
    "chatAccess": {
      "relationship": "non-friend",
      "inviteStatus": "accepted",
      "canSendMessage": true,
      "canAcceptInvite": false,
      "canRejectInvite": false,
      "reasonLabel": "Chat request accepted."
    }
  }
}
""".trimIndent()

private val profileSettingsResponseJson = """
{
  "source": "api",
  "generatedAt": "2026-05-30T00:00:00.000Z",
  "settings": {
    "user": {
      "id": "firebase:uid-1",
      "handle": "min.zip",
      "displayName": "Min Zip",
      "homeNeighborhood": "Seongsu",
      "avatarImageUrl": "",
      "roles": ["creator"]
    },
    "privacy": {
      "savedPlacesVisibility": "private",
      "chatInvites": "shared-clips-and-places",
      "defaultReceiptVisibility": "public"
    }
  },
  "updated": true
}
""".trimIndent()
