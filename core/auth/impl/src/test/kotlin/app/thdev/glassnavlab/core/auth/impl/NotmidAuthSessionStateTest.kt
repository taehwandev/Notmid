package app.thdev.glassnavlab.core.auth.impl

import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthSignInRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthProvider
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotmidAuthSessionStateTest {
    @Test
    fun subscriberSeesSignInProfileReceiptAndSignOut() = runBlocking {
        withTimeout(2_000) {
            val gateway = LocalNotmidAuthGateway()
            val observed = mutableListOf<String?>()
            val job = launch(start = CoroutineStart.UNDISPATCHED) {
                gateway.states.take(4).toList().mapTo(observed) { it.session?.user?.displayName }
            }
            gateway.signIn(NotmidAuthSignInRequest(NotmidAuthProvider.Fake))
            yield()
            val session = checkNotNull(gateway.currentState().session)
            gateway.applyProfileUpdate(session, session.user.copy(displayName = "Updated"))
            yield()
            assertEquals("Updated", gateway.currentState().session?.user?.displayName)
            gateway.signOut()
            job.join()
            assertEquals(listOf(null, "Local You", "Updated", null), observed)
        }
    }

    @Test
    fun lateProfileReceiptCannotRestoreSignedOutSession() = runBlocking {
        val gateway = LocalNotmidAuthGateway()
        gateway.signIn(NotmidAuthSignInRequest(NotmidAuthProvider.Fake))
        val session = checkNotNull(gateway.currentState().session)
        gateway.signOut()
        gateway.applyProfileUpdate(session, session.user.copy(displayName = "Late"))
        assertNull(gateway.states.value.session)
    }

    @Test
    fun receiptCannotReplaceAnotherUserOrNewerSession() = runBlocking {
        val gateway = LocalNotmidAuthGateway()
        gateway.signIn(NotmidAuthSignInRequest(NotmidAuthProvider.Fake))
        val initial = gateway.currentState()
        val session = checkNotNull(initial.session)
        gateway.applyProfileUpdate(session, session.user.copy(id = "different"))
        assertEquals(initial, gateway.currentState())
        val owner = NotmidAuthSessionState(initial)
        val refreshed = initial.copy(session = session.copy(accessToken = "replacement-test-token"))
        owner.value = refreshed
        owner.applyProfileUpdate(session, session.user.copy(displayName = "Stale"))
        assertEquals(refreshed, owner.states.value)
    }

    @Test
    fun identicalLocalCredentialsAfterReloginDoNotAcceptOldReceipt() = runBlocking {
        val gateway = LocalNotmidAuthGateway()
        gateway.signIn(NotmidAuthSignInRequest(NotmidAuthProvider.Fake))
        val oldSession = checkNotNull(gateway.currentState().session)
        gateway.signOut()
        gateway.signIn(NotmidAuthSignInRequest(NotmidAuthProvider.Fake))
        gateway.applyProfileUpdate(oldSession, oldSession.user.copy(displayName = "Stale"))
        assertEquals("Local You", gateway.states.value.session?.user?.displayName)
    }
}
