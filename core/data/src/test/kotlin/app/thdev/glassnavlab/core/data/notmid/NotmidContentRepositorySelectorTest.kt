package app.thdev.glassnavlab.core.data.notmid

import app.thdev.glassnavlab.core.domain.notmid.NotmidContentRepository
import app.thdev.glassnavlab.core.model.notmid.NotmidDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class NotmidContentRepositorySelectorTest {
    @Test
    fun selectStaticCreatesOnlyStaticRepository() {
        val staticRepository = FakeRepository()
        val apiRepository = FakeRepository()
        var staticCreated = 0
        var apiCreated = 0
        val selector = NotmidContentRepositorySelector(
            staticRepositoryFactory = {
                staticCreated += 1
                staticRepository
            },
            apiRepositoryFactory = {
                apiCreated += 1
                apiRepository
            },
        )

        val selected = selector.select(NotmidContentSource.Static)

        assertSame(staticRepository, selected)
        assertEquals(1, staticCreated)
        assertEquals(0, apiCreated)
    }

    @Test
    fun selectApiCreatesOnlyApiRepository() {
        val staticRepository = FakeRepository()
        val apiRepository = FakeRepository()
        var staticCreated = 0
        var apiCreated = 0
        val selector = NotmidContentRepositorySelector(
            staticRepositoryFactory = {
                staticCreated += 1
                staticRepository
            },
            apiRepositoryFactory = {
                apiCreated += 1
                apiRepository
            },
        )

        val selected = selector.select(NotmidContentSource.Api)

        assertSame(apiRepository, selected)
        assertEquals(0, staticCreated)
        assertEquals(1, apiCreated)
    }

    @Test
    fun sourceParserAcceptsLocalAndRemoteAliases() {
        assertEquals(NotmidContentSource.Static, NotmidContentSource.from("static"))
        assertEquals(NotmidContentSource.Static, NotmidContentSource.from("fake"))
        assertEquals(NotmidContentSource.Static, NotmidContentSource.from("fixture"))
        assertEquals(NotmidContentSource.Api, NotmidContentSource.from("api"))
        assertEquals(NotmidContentSource.Api, NotmidContentSource.from("remote"))
    }

    @Test
    fun sourceParserRejectsUnknownValues() {
        assertThrows(IllegalStateException::class.java) {
            NotmidContentSource.from("memory")
        }
    }
}

private class FakeRepository : NotmidContentRepository {
    override suspend fun destinations(): List<NotmidDestination> = emptyList()
}
