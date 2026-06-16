package app.thdev.glassnavlab.core.network.notmid

import org.junit.Assert.assertEquals
import org.junit.Test

class NotmidApiPathsTest {
    @Test
    fun dynamicPathSegmentsAreEncoded() {
        assertEquals(
            "/v1/clips/cafe%20queue%2Fcheck",
            NotmidApiPaths.clip("cafe queue/check"),
        )
        assertEquals(
            "/v1/clips/cafe%20queue%2Fcheck/save",
            NotmidApiPaths.clipSave("cafe queue/check"),
        )
        assertEquals(
            "/v1/places/millo%20roasters",
            NotmidApiPaths.place("millo roasters"),
        )
        assertEquals(
            "/v1/inbox/threads/thread%20one/detail",
            NotmidApiPaths.threadDetail("thread one"),
        )
        assertEquals(
            "/v1/inbox/threads/thread%2Fone/messages",
            NotmidApiPaths.threadMessages("thread/one"),
        )
        assertEquals(
            "/v1/inbox/threads/thread%2Fone/invite/accept",
            NotmidApiPaths.threadInviteAccept("thread/one"),
        )
        assertEquals(
            "/v1/inbox/threads/thread%2Fone/invite/reject",
            NotmidApiPaths.threadInviteReject("thread/one"),
        )
    }
}
