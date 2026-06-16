package app.thdev.glassnavlab.core.network.notmid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class NotmidApiConfigTest {
    @Test
    fun urlForNormalizesBaseAndPathSlashes() {
        val config = NotmidApiConfig(baseUrl = "https://thdev.app/api/")

        assertEquals(
            "https://thdev.app/api/v1/feed",
            config.urlFor("/v1/feed"),
        )
        assertEquals(
            "https://thdev.app/api/health",
            config.urlFor("health"),
        )
    }

    @Test
    fun blankBaseUrlIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            NotmidApiConfig(baseUrl = " ")
        }
    }
}
