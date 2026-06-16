package app.thdev.glassnavlab.core.network.notmid

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OkHttpNotmidNetworkClientTest {
    private lateinit var server: HttpServer
    private lateinit var client: OkHttpNotmidNetworkClient
    private val requests = mutableListOf<RecordedRequest>()

    @Before
    fun setUp() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.start()
        client = OkHttpNotmidNetworkClient(
            config = NotmidApiConfig("http://127.0.0.1:${server.address.port}"),
            connectTimeoutMillis = 1_000,
            readTimeoutMillis = 1_000,
        )
    }

    @After
    fun tearDown() {
        server.stop(0)
    }

    @Test
    fun getSendsAcceptAndCustomHeaders() {
        respond(
            path = NotmidApiPaths.HEALTH,
            statusCode = 200,
            body = """{"ok":true}""",
            headers = mapOf("x-request-id" to "req_test"),
        )

        val response = runSuspend {
            client.execute(
                NotmidNetworkRequest(
                    method = NotmidHttpMethod.Get,
                    path = NotmidApiPaths.HEALTH,
                    headers = mapOf("x-notmid-test" to "yes"),
                ),
            )
        }

        assertEquals(200, response.statusCode)
        assertTrue(response.isSuccessful)
        assertEquals("""{"ok":true}""", response.body)
        assertEquals(listOf("req_test"), response.headerValues("x-request-id"))

        val request = requests.single()
        assertEquals("GET", request.method)
        assertEquals(NotmidApiPaths.HEALTH, request.path)
        assertTrue(request.headerValues("accept").contains("application/json"))
        assertEquals(listOf("yes"), request.headerValues("x-notmid-test"))
    }

    @Test
    fun postSendsJsonBody() {
        respond(
            path = NotmidApiPaths.AUTH_FAKE_SIGN_IN,
            statusCode = 200,
            body = """{"mode":"fake"}""",
        )

        val response = runSuspend {
            client.execute(
                NotmidNetworkRequest(
                    method = NotmidHttpMethod.Post,
                    path = NotmidApiPaths.AUTH_FAKE_SIGN_IN,
                    body = """{"provider":"fake"}""",
                ),
            )
        }

        assertEquals(200, response.statusCode)
        assertEquals("""{"mode":"fake"}""", response.body)

        val request = requests.single()
        assertEquals("POST", request.method)
        assertEquals("""{"provider":"fake"}""", request.body)
        assertTrue(
            request.headerValues("content-type").any { value ->
                value.startsWith("application/json")
            },
        )
    }

    @Test
    fun patchSendsJsonBody() {
        respond(
            path = "/v1/profile/settings",
            statusCode = 200,
            body = """{"updated":true}""",
        )

        val response = runSuspend {
            client.execute(
                NotmidNetworkRequest(
                    method = NotmidHttpMethod.Patch,
                    path = "/v1/profile/settings",
                    body = """{"displayName":"Local You"}""",
                ),
            )
        }

        assertEquals(200, response.statusCode)
        assertEquals("""{"updated":true}""", response.body)

        val request = requests.single()
        assertEquals("PATCH", request.method)
        assertEquals("""{"displayName":"Local You"}""", request.body)
    }

    @Test
    fun errorResponsesKeepStatusAndBody() {
        respond(
            path = "/v1/capture/publish",
            statusCode = 401,
            body = """{"error":{"code":"auth_required"}}""",
        )

        val response = runSuspend {
            client.execute(
                NotmidNetworkRequest(
                    method = NotmidHttpMethod.Post,
                    path = "/v1/capture/publish",
                    body = """{}""",
                ),
            )
        }

        assertEquals(401, response.statusCode)
        assertFalse(response.isSuccessful)
        assertEquals("""{"error":{"code":"auth_required"}}""", response.body)
    }

    @Test
    fun blankPathIsInvalidRequestFailure() {
        val exception = assertThrows(NotmidNetworkException::class.java) {
            runSuspend {
                client.execute(
                    NotmidNetworkRequest(
                        method = NotmidHttpMethod.Get,
                        path = " ",
                    ),
                )
            }
        }

        assertEquals(NotmidNetworkErrorCode.InvalidRequest, exception.error.code)
        assertEquals("Request path must not be blank.", exception.error.message)
        assertTrue(requests.isEmpty())
    }

    @Test
    fun malformedBaseUrlIsInvalidRequestFailure() {
        val malformedClient = OkHttpNotmidNetworkClient(
            config = NotmidApiConfig("https://not mid.example"),
            connectTimeoutMillis = 1,
            readTimeoutMillis = 1,
        )

        val exception = assertThrows(NotmidNetworkException::class.java) {
            runSuspend {
                malformedClient.execute(
                    NotmidNetworkRequest(
                        method = NotmidHttpMethod.Get,
                        path = NotmidApiPaths.HEALTH,
                    ),
                )
            }
        }

        assertEquals(NotmidNetworkErrorCode.InvalidRequest, exception.error.code)
        assertNotNull(exception.error.causeName)
    }

    private fun respond(
        path: String,
        statusCode: Int,
        body: String,
        headers: Map<String, String> = emptyMap(),
    ) {
        server.createContext(path) { exchange ->
            val requestBody = exchange.requestBody.bufferedReader(Charsets.UTF_8).use {
                it.readText()
            }
            requests += RecordedRequest(
                method = exchange.requestMethod,
                path = exchange.requestURI.path,
                headers = exchange.requestHeaders.mapKeys { entry ->
                    entry.key.lowercase()
                },
                body = requestBody,
            )

            headers.forEach { (name, value) ->
                exchange.responseHeaders.add(name, value)
            }
            val responseBody = body.toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(statusCode, responseBody.size.toLong())
            exchange.responseBody.use { outputStream ->
                outputStream.write(responseBody)
            }
        }
    }
}

private data class RecordedRequest(
    val method: String,
    val path: String,
    val headers: Map<String, List<String>>,
    val body: String,
) {
    fun headerValues(name: String): List<String> = headers[name.lowercase()].orEmpty()
}

private fun NotmidNetworkResponse.headerValues(name: String): List<String> {
    return headers.entries.firstOrNull { (key, _) ->
        key.equals(name, ignoreCase = true)
    }?.value.orEmpty()
}
