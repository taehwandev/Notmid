package app.thdev.glassnavlab.feature.bootstrap

import app.thdev.glassnavlab.core.domain.notmid.NotmidContentRepository
import app.thdev.glassnavlab.core.model.notmid.NotmidClip
import app.thdev.glassnavlab.core.model.notmid.NotmidDestination
import app.thdev.glassnavlab.core.model.notmid.NotmidNavigationIcon

internal class FakeBootstrapContentRepository(
    private val destinations: List<NotmidDestination>,
) : NotmidContentRepository {
    override suspend fun destinations(): List<NotmidDestination> = destinations
}

internal val bootstrapTestDestination = NotmidDestination(
    id = "feed",
    title = "Feed",
    subtitle = "Short video receipts.",
    icon = NotmidNavigationIcon.Feed,
    clips = listOf(
        NotmidClip(
            id = "clip-1",
            title = "Clip",
            description = "A local clip.",
            badge = "Local",
            palette = emptyList(),
        ),
    ),
    places = emptyList(),
)
