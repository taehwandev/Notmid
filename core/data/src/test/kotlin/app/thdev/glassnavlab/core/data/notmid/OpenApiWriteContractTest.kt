package app.thdev.glassnavlab.core.data.notmid

import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteException
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteFailure
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthMode
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthProvider
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthSession
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthUser
import app.thdev.glassnavlab.core.model.notmid.NotmidCapturePublishRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidCaptureVisibility
import app.thdev.glassnavlab.core.model.notmid.NotmidChatInviteDecision
import app.thdev.glassnavlab.core.model.notmid.NotmidMessageAttachment
import app.thdev.glassnavlab.core.model.notmid.NotmidProfileSettingsUpdateRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidSendThreadMessageRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidStartThreadRequest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenApiWriteContractTest {
    @Test
    fun allProtectedOperationsEncodeDeclaredRequestsAndDecodeSchemaGeneratedReceipts() {
        val client = OpenApiContractClient()
        val repository = ApiNotmidProtectedWriteRepository(client)
        runSuspend {
            NotmidCaptureVisibility.entries.forEach { visibility ->
                assertEquals("contract-value", repository.publishCapture(auth, capture.copy(visibility = visibility)).clip.id)
            }
            assertTrue(repository.saveClip(auth, "clip / one").saved)
            listOf(null, NotmidMessageAttachment.Clip("clip"), NotmidMessageAttachment.Place("place"),
                NotmidMessageAttachment.Route("route", listOf("place"))).forEach { attachment ->
                assertEquals("contract-value", repository.sendThreadMessage(auth, "thread / one",
                    NotmidSendThreadMessageRequest("hello \"world\"", attachment)).message.id)
            }
            assertEquals("contract-value", repository.startThread(auth,
                NotmidStartThreadRequest("handle", "hello", "clip", "place")).thread.id)
            NotmidChatInviteDecision.entries.forEach { decision ->
                assertEquals("contract-value", repository.respondThreadInvite(auth, "thread / one", decision).thread.id)
            }
            assertTrue(repository.updateProfileSettings(auth,
                NotmidProfileSettingsUpdateRequest("display", "neighborhood")).updated)
        }
        assertEquals(setOf("publishCapture", "saveClip", "sendInboxThreadMessage", "startInboxThread",
            "acceptInboxThreadInvite", "rejectInboxThreadInvite", "updateProfileSettings"), client.operations.toSet())
    }

    @Test
    fun everyDeclaredModerationAndChatEnumDecodesThroughProtectedRepository() {
        val contract = OpenApiContractFixture()
        val moderation = contract.schema("NotmidCapturePublishResponse").getValue("properties")
            .jsonObject.getValue("moderationStatus").jsonObject.getValue("enum").jsonArray
        moderation.forEach { value ->
            val client = OpenApiContractClient(contract) { _, response -> response.replacing("moderationStatus", value = value) }
            runSuspend { ApiNotmidProtectedWriteRepository(client).publishCapture(auth, capture) }
        }
        mapOf("relationship" to "NotmidChatRelationship", "inviteStatus" to "NotmidChatInviteStatus")
            .forEach { (field, schema) ->
                contract.schema(schema).getValue("enum").jsonArray.forEach { value ->
                    val client = OpenApiContractClient(contract) { _, response ->
                        response.replacing("thread", "chatAccess", field, value = value)
                    }
                    runSuspend { ApiNotmidProtectedWriteRepository(client).respondThreadInvite(auth, "thread", NotmidChatInviteDecision.Accept) }
                }
            }
    }

    @Test
    fun allDeclaredAttachmentVariantsDecodeThroughProtectedRepository() {
        val contract = OpenApiContractFixture()
        contract.schema("NotmidMessageAttachment").getValue("oneOf").jsonArray.forEach { variant ->
            val attachment = contract.sample(variant.jsonObject)
            val client = OpenApiContractClient(contract) { _, response ->
                response.replacing("message", "attachment", value = attachment)
            }
            val result = runSuspend { ApiNotmidProtectedWriteRepository(client)
                .sendThreadMessage(auth, "thread", NotmidSendThreadMessageRequest("body")) }
            val decodedType = when (result.message.attachment) {
                is NotmidMessageAttachment.Clip -> "clip"
                is NotmidMessageAttachment.Place -> "place"
                is NotmidMessageAttachment.Route -> "route"
                null -> "missing"
            }
            assertEquals(attachment.jsonObject.getValue("type").jsonPrimitive.content, decodedType)
        }
    }

    @Test
    fun missingDecoderRequiredFieldsAndUnknownEnumsReturnTypedMalformedFailures() {
        val mutations: List<(JsonObject) -> JsonObject> = listOf(
            { it.replacing("clip", "id", value = null) },
            { it.replacing("clip", "metrics", value = JsonPrimitive("invalid")) },
            { it.replacing("moderationStatus", value = JsonPrimitive("future")) },
        )
        mutations.forEach { mutate ->
            val client = OpenApiContractClient { _, response -> mutate(response) }
            val error = assertThrows(NotmidProtectedWriteException::class.java) {
                runSuspend { ApiNotmidProtectedWriteRepository(client).publishCapture(auth, capture) }
            }
            assertTrue(error.failure is NotmidProtectedWriteFailure.MalformedResponse)
        }
        listOf("relationship", "inviteStatus").forEach { field ->
            val client = OpenApiContractClient { _, response ->
                response.replacing("thread", "chatAccess", field, value = JsonPrimitive("future"))
            }
            val error = assertThrows(NotmidProtectedWriteException::class.java) {
                runSuspend { ApiNotmidProtectedWriteRepository(client).respondThreadInvite(auth, "thread", NotmidChatInviteDecision.Accept) }
            }
            assertTrue(error.failure is NotmidProtectedWriteFailure.MalformedResponse)
        }
    }

    @Test
    fun missingMessageBooleanAndUnknownAttachmentTypeReturnTypedMalformedFailures() {
        val mutations: List<(JsonObject) -> JsonObject> = listOf(
            { it.replacing("message", "mine", value = null) },
            { it.replacing("message", "attachment", "type", value = JsonPrimitive("future")) },
        )
        mutations.forEach { mutate ->
            val client = OpenApiContractClient { _, response -> mutate(response) }
            val error = assertThrows(NotmidProtectedWriteException::class.java) {
                runSuspend { ApiNotmidProtectedWriteRepository(client)
                    .sendThreadMessage(auth, "thread", NotmidSendThreadMessageRequest("body")) }
            }
            assertTrue(error.failure is NotmidProtectedWriteFailure.MalformedResponse)
        }
    }

    private val auth = NotmidAuthState(
        mode = NotmidAuthMode.Firebase,
        session = NotmidAuthSession("contract-token", NotmidAuthProvider.Anonymous, "later",
            NotmidAuthUser("id", "handle", "display", "home", "", listOf("creator"))),
        requiredActions = emptyList(),
    )
    private val capture = NotmidCapturePublishRequest("draft", "caption", "place", listOf("tag"), NotmidCaptureVisibility.Public)
}
