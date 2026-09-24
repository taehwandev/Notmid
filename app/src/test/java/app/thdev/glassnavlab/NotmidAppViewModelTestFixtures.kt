package app.thdev.glassnavlab

import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthGateway
import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthResult
import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthSignInRequest
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentRepository
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthMode
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthProvider
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthSession
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthUser
import app.thdev.glassnavlab.core.model.notmid.NotmidClip
import app.thdev.glassnavlab.core.model.notmid.NotmidDestination
import app.thdev.glassnavlab.core.model.notmid.NotmidNavigationIcon
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        kotlinx.coroutines.Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        kotlinx.coroutines.Dispatchers.resetMain()
    }
}

internal class FakeContentRepository(
    private val destinations: List<NotmidDestination>,
) : NotmidContentRepository {
    override suspend fun destinations(): List<NotmidDestination> = destinations
}

internal class FakeAuthGateway(
    initialState: NotmidAuthState,
) : NotmidAuthGateway {
    private val mutableAuth = kotlinx.coroutines.flow.MutableStateFlow(initialState)
    override val states: kotlinx.coroutines.flow.StateFlow<NotmidAuthState> = mutableAuth
    private var state: NotmidAuthState
        get() = mutableAuth.value
        set(value) { mutableAuth.value = value }

    override fun applyProfileUpdate(
        expectedSession: app.thdev.glassnavlab.core.model.notmid.NotmidAuthSession,
        user: app.thdev.glassnavlab.core.model.notmid.NotmidAuthUser,
    ): NotmidAuthState {
        if (state.session === expectedSession && user.id == expectedSession.user.id) {
            state = state.copy(session = expectedSession.copy(user = user))
        }
        return state
    }

    override fun currentState(): NotmidAuthState = state

    override suspend fun signIn(request: NotmidAuthSignInRequest): NotmidAuthResult {
        return NotmidAuthResult.Success(signedInAuthState, "/notmid").also { state = signedInAuthState }
    }

    override fun signOut(): NotmidAuthState {
        state = signedOutAuthState
        return state
    }
}

internal val testUser = NotmidAuthUser(
    id = "user-1",
    handle = "you.local",
    displayName = "Local You",
    homeNeighborhood = "Seongsu",
    avatarImageUrl = "local-fake-avatar",
    roles = listOf("creator"),
)

internal val signedInAuthState = NotmidAuthState(
    mode = NotmidAuthMode.Fake,
    session = NotmidAuthSession(
        accessToken = "test-token",
        provider = NotmidAuthProvider.Fake,
        expiresAt = "2026-05-24T00:00:00.000Z",
        user = testUser,
    ),
    requiredActions = emptyList(),
)

internal val signedOutAuthState = NotmidAuthState(
    mode = NotmidAuthMode.Fake,
    session = null,
    requiredActions = emptyList(),
)

internal val testClip = NotmidClip(
    id = "clip-1",
    title = "Clip",
    description = "A local clip.",
    badge = "Local",
    palette = emptyList(),
)

internal val viewModelTestDestination = NotmidDestination(
    id = "feed",
    title = "Feed",
    subtitle = "Short video receipts.",
    icon = NotmidNavigationIcon.Feed,
    clips = listOf(testClip),
    places = emptyList(),
)
