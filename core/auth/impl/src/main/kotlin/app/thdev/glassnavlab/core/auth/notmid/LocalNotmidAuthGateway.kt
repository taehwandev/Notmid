package app.thdev.glassnavlab.core.auth.notmid

import app.thdev.glassnavlab.core.model.notmid.NotmidAuthMode
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthProvider
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthSession
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthUser

class LocalNotmidAuthGateway(
    private val mode: NotmidAuthMode = NotmidAuthMode.Fake,
) : NotmidAuthGateway {
    private var state: NotmidAuthState = signedOutState(mode)

    override fun currentState(): NotmidAuthState = state

    override suspend fun signIn(request: NotmidAuthSignInRequest): NotmidAuthResult {
        return when (mode) {
            NotmidAuthMode.Fake -> {
                state = signedOutState(mode).copy(
                    session = fakeSession(request.provider),
                )
                NotmidAuthResult.Success(
                    state = state,
                    nextPath = request.nextPath(),
                )
            }

            NotmidAuthMode.Firebase -> NotmidAuthResult.Rejected(
                code = "firebase_api_verifier_required",
                message = "Firebase sign-in must be verified by the notmid API boundary.",
                state = state,
            )

            NotmidAuthMode.Disabled -> NotmidAuthResult.Rejected(
                code = "auth_disabled",
                message = "Authentication is disabled for this runtime.",
                state = state,
            )
        }
    }

    override fun signOut(): NotmidAuthState {
        state = signedOutState(mode)
        return state
    }
}

private val fakeUser = NotmidAuthUser(
    id = "local-you",
    handle = "you.local",
    displayName = "Local You",
    homeNeighborhood = "Seongsu",
    avatarImageUrl = "local-fake-avatar",
    roles = listOf("creator", "local-dev"),
)

private fun fakeSession(provider: NotmidAuthProvider): NotmidAuthSession {
    return NotmidAuthSession(
        accessToken = "notmid-fake-local-dev-token",
        provider = provider,
        expiresAt = "2026-05-24T00:00:00.000Z",
        user = fakeUser,
    )
}
