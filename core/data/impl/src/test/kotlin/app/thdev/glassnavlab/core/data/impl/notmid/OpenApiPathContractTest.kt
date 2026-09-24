package app.thdev.glassnavlab.core.data.impl.notmid

import app.thdev.glassnavlab.core.network.notmid.NotmidApiPaths
import app.thdev.glassnavlab.core.network.notmid.NotmidHttpMethod
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkRequest
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class OpenApiPathContractTest {
    @Test
    fun allAndroidEndpointsResolveToPinnedOperations() {
        val contract = OpenApiContractFixture()
        val id = "value / encoded"
        val gets = mapOf(
            NotmidApiPaths.HEALTH to "getHealth",
            NotmidApiPaths.AUTH_STATUS to "getAuthStatus",
            NotmidApiPaths.CAPTURE_DRAFT to "getCaptureDraft",
            NotmidApiPaths.FEED to "getFeed",
            NotmidApiPaths.MAP to "getMap",
            NotmidApiPaths.INBOX_THREADS to "getInboxThreads",
            NotmidApiPaths.PROFILE_SETTINGS to "getProfileSettings",
            NotmidApiPaths.clip(id) to "getClip",
            NotmidApiPaths.place(id) to "getPlace",
            NotmidApiPaths.thread(id) to "getInboxThread",
            NotmidApiPaths.threadDetail(id) to "getInboxThreadDetail",
        )
        val posts = mapOf(
            NotmidApiPaths.AUTH_FAKE_SIGN_IN to "fakeSignIn",
            NotmidApiPaths.CAPTURE_PUBLISH to "publishCapture",
            NotmidApiPaths.INBOX_THREADS to "startInboxThread",
            NotmidApiPaths.clipSave(id) to "saveClip",
            NotmidApiPaths.threadMessages(id) to "sendInboxThreadMessage",
            NotmidApiPaths.threadInviteAccept(id) to "acceptInboxThreadInvite",
            NotmidApiPaths.threadInviteReject(id) to "rejectInboxThreadInvite",
        )
        mapOf(NotmidHttpMethod.Get to gets, NotmidHttpMethod.Post to posts,
            NotmidHttpMethod.Patch to mapOf(NotmidApiPaths.PROFILE_SETTINGS to "updateProfileSettings"))
            .forEach { (method, paths) -> paths.forEach { (path, expected) ->
                assertEquals(expected, contract.operation(NotmidNetworkRequest(method, path))
                    .getValue("operationId").jsonPrimitive.content)
            } }
    }

    @Test
    fun optionalBearerAllowsGuestsWhileProtectedOperationsRequireToken() {
        val contract = OpenApiContractFixture()
        val guest = NotmidNetworkRequest(NotmidHttpMethod.Get, NotmidApiPaths.AUTH_STATUS)
        contract.validateRequest(guest, contract.operation(guest))
        val authenticated = guest.copy(headers = mapOf("authorization" to "Bearer contract-token"))
        contract.validateRequest(authenticated, contract.operation(authenticated))
        val protectedRequest = guest.copy(path = NotmidApiPaths.PROFILE_SETTINGS)
        assertThrows(IllegalStateException::class.java) {
            contract.validateRequest(protectedRequest, contract.operation(protectedRequest))
        }
        val protectedAuthenticated = protectedRequest.copy(headers = authenticated.headers)
        contract.validateRequest(protectedAuthenticated, contract.operation(protectedAuthenticated))
        val invalidToken = guest.copy(headers = mapOf("authorization" to "invalid"))
        assertThrows(IllegalStateException::class.java) {
            contract.validateRequest(invalidToken, contract.operation(invalidToken))
        }
    }

    @Test
    fun contractHarnessRejectsUnknownPathsMethodsAndMalformedClientFields() {
        val contract = OpenApiContractFixture()
        assertThrows(IllegalStateException::class.java) {
            contract.operation(NotmidNetworkRequest(NotmidHttpMethod.Get, "/missing"))
        }
        assertThrows(IllegalStateException::class.java) {
            contract.operation(NotmidNetworkRequest(NotmidHttpMethod.Post, NotmidApiPaths.FEED))
        }
        listOf("{\"displayName\":17}", "{\"misspelled\":\"name\"}").forEach { body ->
            val request = NotmidNetworkRequest(NotmidHttpMethod.Patch, NotmidApiPaths.PROFILE_SETTINGS,
                mapOf("authorization" to "Bearer contract-token"), body)
            assertThrows(IllegalStateException::class.java) { contract.validateRequest(request, contract.operation(request)) }
        }
    }
}
