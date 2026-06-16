package app.thdev.glassnavlab.core.network.assertions

import app.thdev.glassnavlab.core.network.notmid.NotmidHttpMethod
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkRequest

class NetworkRequestSubject(
    private val request: NotmidNetworkRequest,
) {
    fun hasMethod(expected: NotmidHttpMethod): NetworkRequestSubject {
        assertEquals(expected = expected, actual = request.method, label = "method")
        return this
    }

    fun hasPath(expected: String): NetworkRequestSubject {
        assertEquals(expected = expected, actual = request.path, label = "path")
        return this
    }

    fun hasHeader(
        name: String,
        expected: String,
    ): NetworkRequestSubject {
        assertEquals(expected = expected, actual = request.headers[name], label = "header $name")
        return this
    }

    fun hasNoHeader(name: String): NetworkRequestSubject {
        if (request.headers.containsKey(name)) {
            fail("Expected no header $name.")
        }
        return this
    }

    fun hasBody(expected: String): NetworkRequestSubject {
        assertEquals(expected = expected, actual = request.body, label = "body")
        return this
    }

    fun bodyContains(fragment: String): NetworkRequestSubject {
        val body = request.body.orEmpty()
        if (!body.contains(fragment)) {
            fail("Expected body to contain <$fragment>, but was <$body>.")
        }
        return this
    }
}

fun NotmidNetworkRequest.assertNetworkRequest(block: NetworkRequestSubject.() -> Unit) {
    NetworkRequestSubject(this).block()
}
