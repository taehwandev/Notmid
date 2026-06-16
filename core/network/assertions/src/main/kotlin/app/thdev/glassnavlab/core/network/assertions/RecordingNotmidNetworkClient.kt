package app.thdev.glassnavlab.core.network.assertions

import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkClient
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkRequest
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkResponse

class RecordingNotmidNetworkClient(
    vararg results: QueuedNetworkResult,
) : NotmidNetworkClient {
    private val queuedResults = ArrayDeque(results.toList())
    private val recordedRequests = mutableListOf<NotmidNetworkRequest>()

    val requests: List<NotmidNetworkRequest>
        get() = recordedRequests.toList()

    val pendingResultCount: Int
        get() = queuedResults.size

    override suspend fun execute(request: NotmidNetworkRequest): NotmidNetworkResponse {
        recordedRequests += request
        if (queuedResults.isEmpty()) {
            fail("No queued network result for ${request.method} ${request.path}.")
        }
        return queuedResults.removeFirst().responseOrThrow()
    }

    fun assertNoPendingResults() {
        if (queuedResults.isNotEmpty()) {
            fail("Expected all queued network results to be consumed, but ${queuedResults.size} remain.")
        }
    }

    fun assertRequestCount(expected: Int) {
        assertEquals(expected = expected, actual = recordedRequests.size, label = "request count")
    }
}
