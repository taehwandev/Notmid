package app.thdev.glassnavlab.core.data.assertions.notmid

import app.thdev.glassnavlab.core.domain.notmid.NotmidAuthRepository
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthMode
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthProvider
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthRequiredAction
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthSession
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthUser

class StaticNotmidAuthRepository : NotmidAuthRepository {
    private var state: NotmidAuthState = SignedOutState

    override fun authState(): NotmidAuthState = state

    override fun signInLocal(): NotmidAuthState {
        state = FakeSignedInState
        return state
    }
}

private val RequiredActions = listOf(
    NotmidAuthRequiredAction.Capture,
    NotmidAuthRequiredAction.Save,
    NotmidAuthRequiredAction.Chat,
    NotmidAuthRequiredAction.ProfileEdit,
    NotmidAuthRequiredAction.Moderation,
)

private val FakeUser = NotmidAuthUser(
    id = "local-you",
    handle = "you.local",
    displayName = "Local You",
    homeNeighborhood = "Seongsu",
    avatarImageUrl = "local-fake-avatar",
    roles = listOf("creator", "local-dev"),
)

private val FakeSession = NotmidAuthSession(
    accessToken = "notmid-fake-local-dev-token",
    provider = NotmidAuthProvider.Fake,
    expiresAt = "2026-05-24T00:00:00.000Z",
    user = FakeUser,
)

private val SignedOutState = NotmidAuthState(
    mode = NotmidAuthMode.Fake,
    session = null,
    requiredActions = RequiredActions,
)

private val FakeSignedInState = SignedOutState.copy(session = FakeSession)
