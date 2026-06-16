package app.thdev.glassnavlab.core.auth.notmid

import app.thdev.glassnavlab.core.model.notmid.NotmidAuthMode
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalNotmidAuthGatewayTest {
    @Test
    fun fakeModeCreatesLocalSession() {
        val gateway = LocalNotmidAuthGateway(mode = NotmidAuthMode.Fake)

        val result = runSuspend {
            gateway.signIn(
                NotmidAuthSignInRequest(
                    provider = NotmidAuthProvider.Fake,
                    intent = NotmidAuthIntent.Capture,
                ),
            )
        }

        assertTrue(result is NotmidAuthResult.Success)
        val success = result as NotmidAuthResult.Success
        assertEquals("/notmid/capture", success.nextPath)
        assertEquals(NotmidAuthProvider.Fake, success.state.session?.provider)
        assertTrue(gateway.currentState().isAuthenticated)
    }

    @Test
    fun fakeModeKeepsReturnPathInsideNotmid() {
        val gateway = LocalNotmidAuthGateway(mode = NotmidAuthMode.Fake)

        val result = runSuspend {
            gateway.signIn(
                NotmidAuthSignInRequest(
                    provider = NotmidAuthProvider.Fake,
                    intent = NotmidAuthIntent.Browse,
                    returnToPath = "https://example.com/notmid",
                ),
            )
        }

        assertTrue(result is NotmidAuthResult.Success)
        assertEquals("/notmid", (result as NotmidAuthResult.Success).nextPath)
    }

    @Test
    fun disabledModeRejectsLocalSignIn() {
        val gateway = LocalNotmidAuthGateway(mode = NotmidAuthMode.Disabled)

        val result = runSuspend {
            gateway.signIn(
                NotmidAuthSignInRequest(provider = NotmidAuthProvider.Fake),
            )
        }

        assertTrue(result is NotmidAuthResult.Rejected)
        val rejected = result as NotmidAuthResult.Rejected
        assertEquals("auth_disabled", rejected.code)
        assertFalse(rejected.state.isAuthenticated)
        assertFalse(gateway.currentState().isAuthenticated)
    }

    @Test
    fun firebaseModeRequiresApiVerifiedBoundary() {
        val gateway = LocalNotmidAuthGateway(mode = NotmidAuthMode.Firebase)

        val result = runSuspend {
            gateway.signIn(
                NotmidAuthSignInRequest(provider = NotmidAuthProvider.Google),
            )
        }

        assertTrue(result is NotmidAuthResult.Rejected)
        assertEquals(
            "firebase_api_verifier_required",
            (result as NotmidAuthResult.Rejected).code,
        )
        assertFalse(gateway.currentState().isAuthenticated)
    }

    @Test
    fun signOutClearsCurrentSession() {
        val gateway = LocalNotmidAuthGateway(mode = NotmidAuthMode.Fake)
        runSuspend {
            gateway.signIn(NotmidAuthSignInRequest(provider = NotmidAuthProvider.Fake))
        }

        val signedOut = gateway.signOut()

        assertFalse(signedOut.isAuthenticated)
        assertFalse(gateway.currentState().isAuthenticated)
    }
}
