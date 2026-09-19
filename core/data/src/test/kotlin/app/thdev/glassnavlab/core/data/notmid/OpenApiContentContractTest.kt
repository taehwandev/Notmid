package app.thdev.glassnavlab.core.data.notmid

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class OpenApiContentContractTest {
    @Test
    fun schemaGeneratedResponsesHydrateEveryContentDestination() {
        val client = OpenApiContractClient()
        val destinations = runSuspend { ApiNotmidContentRepository(client).destinations() }

        assertEquals(listOf("feed", "map", "capture", "inbox", "profile"), destinations.map { it.id })
        assertEquals("contract-value", destinations[0].clips.single().id)
        assertEquals("contract-value", destinations[1].places.single().id)
        assertEquals("contract-value", destinations[2].captureDraft?.id)
        assertEquals("contract-value", destinations[3].threads.single().id)
        assertEquals("contract-value", destinations[3].threadMessages.single().id)
        assertEquals(listOf("getFeed", "getMap", "getCaptureDraft", "getInboxThreads", "getInboxThreadDetail"), client.operations)
    }

    @Test
    fun everyDeclaredDraftEnumValueDecodesThroughRepository() {
        val contract = OpenApiContractFixture()
        val properties = contract.schema("NotmidCaptureDraft").getValue("properties").jsonObject
        listOf("visibility", "mediaState").forEach { field ->
            contract.resolve(properties.getValue(field).jsonObject).getValue("enum").jsonArray.forEach { value ->
                val client = OpenApiContractClient(contract) { operation, response ->
                    if (operation == "getCaptureDraft") response.replacing("draft", field, value = value) else response
                }
                runSuspend { ApiNotmidContentRepository(client).destinations() }
            }
        }
    }

    @Test
    fun missingAndroidRequiredDraftFieldAndUnknownEnumsBecomeMalformedContent() {
        // Requiredness is an Android decoder expectation, not asserted by the incomplete snapshot.
        listOf("id" to null, "visibility" to JsonPrimitive("future"), "mediaState" to JsonPrimitive("future"))
            .forEach { (field, value) ->
                val client = OpenApiContractClient { operation, response ->
                    if (operation == "getCaptureDraft") response.replacing("draft", field, value = value) else response
                }
                assertThrows(ApiNotmidContentException.MalformedJson::class.java) {
                    runSuspend { ApiNotmidContentRepository(client).destinations() }
                }
            }
    }
}
