package app.thdev.glassnavlab.core.network.assertions

import app.thdev.glassnavlab.core.network.notmid.NotmidHttpMethod
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkError
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkErrorCode
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkException
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkRequest
import kotlin.coroutines.startCoroutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class NetworkAssertionsTest {
    @Test
    fun recordingClientReturnsQueuedResponsesAndRecordsRequests() {
        val client = RecordingNotmidNetworkClient(
            QueuedNetworkResponse("""{"ok":true}"""),
        )

        val response = runSuspend {
            client.execute(
                NotmidNetworkRequest(
                    method = NotmidHttpMethod.Post,
                    path = "/v1/test",
                    headers = mapOf("authorization" to "Bearer token"),
                    body = """{"name":"notmid"}""",
                ),
            )
        }

        assertEquals(200, response.statusCode)
        assertEquals("""{"ok":true}""", response.body)
        client.assertNoPendingResults()
        client.requests.single().assertNetworkRequest {
            hasMethod(NotmidHttpMethod.Post)
            hasPath("/v1/test")
            hasHeader("authorization", "Bearer token")
            bodyContains("notmid")
        }
        assertEquals("<redacted>", client.requests.single().redactedHeaders()["authorization"])
    }

    @Test
    fun queuedFailureThrowsNetworkException() {
        val client = RecordingNotmidNetworkClient(
            QueuedNetworkFailure(
                NotmidNetworkError(
                    code = NotmidNetworkErrorCode.Transport,
                    message = "offline",
                ),
            ),
        )

        val exception = assertThrows(NotmidNetworkException::class.java) {
            runSuspend {
                client.execute(
                    NotmidNetworkRequest(
                        method = NotmidHttpMethod.Get,
                        path = "/v1/test",
                    ),
                )
            }
        }

        assertEquals(NotmidNetworkErrorCode.Transport, exception.error.code)
    }

    @Test
    fun apiErrorEnvelopeFixtureBuildsSafeJsonBody() {
        assertEquals(
            """{"error":{"code":"auth_required","message":"Sign in first."}}""",
            ApiErrorEnvelopeFixtures.body(
                code = "auth_required",
                message = "Sign in first.",
            ),
        )
    }
}

private fun <T> runSuspend(block: suspend () -> T): T {
    var outcome: Result<T>? = null
    block.startCoroutine(
        object : kotlin.coroutines.Continuation<T> {
            override val context: kotlin.coroutines.CoroutineContext = kotlin.coroutines.EmptyCoroutineContext

            override fun resumeWith(result: Result<T>) {
                outcome = result
            }
        },
    )
    return outcome?.getOrThrow() ?: error("Coroutine did not complete synchronously.")
}
