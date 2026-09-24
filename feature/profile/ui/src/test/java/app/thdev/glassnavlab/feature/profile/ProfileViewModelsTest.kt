package app.thdev.glassnavlab.feature.profile

import androidx.lifecycle.SavedStateHandle
import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthGateway
import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthResult
import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthSignInRequest
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentSnapshot
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentUpdates
import app.thdev.glassnavlab.core.notice.api.effect.MutableNoticeEffectDelegate
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteExecutor
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteResult
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteRequest
import app.thdev.glassnavlab.core.model.notmid.*
import app.thdev.glassnavlab.core.navigation.notmid.NotmidRouteEvent
import app.thdev.glassnavlab.core.navigation.runtime.RouteEvent
import app.thdev.glassnavlab.core.navigation.runtime.RouteEventSink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import app.thdev.glassnavlab.core.notice.api.effect.NoticeEffect
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelsTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun profileCombinesSharedSourcesAndOwnsSettingsRoute() = runTest(dispatcher) {
        val auth = Auth()
        val source = object : NotmidContentUpdates {
            override val snapshot = MutableStateFlow<NotmidContentSnapshot>(NotmidContentSnapshot.Ready(listOf(destination)))
        }
        val routes = mutableListOf<RouteEvent>()
        val vm = ProfileViewModel(auth, source, RouteEventSink { routes.add(it) })
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        runCurrent()
        assertEquals("profile", vm.state.value.destination?.id)
        val session = checkNotNull(auth.currentState().session)
        auth.applyProfileUpdate(session, user.copy(displayName = "Updated"))
        runCurrent()
        assertEquals("Updated", vm.state.value.auth.session?.user?.displayName)
        vm.onAction(ProfileAction.OpenSettings)
        assertEquals(listOf(NotmidRouteEvent.SettingsRequested), routes)
        auth.signOut()
        runCurrent()
        assertNull(vm.state.value.auth.session)
    }

    @Test
    fun settingsRestoresOnlyMatchingUserAndDispatchesValidatedEdits() = runTest(dispatcher) {
        val auth = Auth()
        val saved = SavedStateHandle(mapOf("userId" to user.id, "name" to "Restored", "neighborhood" to "Seoul"))
        val writes = RecordingExecutor()
        val requests = writes.requests
        val vm = ProfileSettingsViewModel(saved, auth, writes, MutableNoticeEffectDelegate())
        runCurrent()
        assertEquals("Restored", vm.state.value.displayName)
        vm.onAction(ProfileSettingsAction.DisplayNameChanged("  "))
        vm.onAction(ProfileSettingsAction.Save)
        runCurrent()
        assertTrue(requests.isEmpty())
        vm.onAction(ProfileSettingsAction.DisplayNameChanged("Edited"))
        vm.onAction(ProfileSettingsAction.NeighborhoodChanged("Busan"))
        vm.onAction(ProfileSettingsAction.Save)
        runCurrent()
        assertEquals(listOf(NotmidProtectedWriteRequest.UpdateProfileSettings(NotmidProfileSettingsUpdateRequest("Edited", "Busan"))), requests)
        assertEquals("Edited", saved.get<String>("name"))
    }

    @Test
    fun latestLogoutBlocksSaveBeforeCollectorRunsAndClearsDraft() = runTest(dispatcher) {
        val auth = Auth()
        val writes = RecordingExecutor()
        val requests = writes.requests
        val saved = SavedStateHandle()
        val vm = ProfileSettingsViewModel(saved, auth, writes, MutableNoticeEffectDelegate())
        vm.onAction(ProfileSettingsAction.DisplayNameChanged("Private draft"))
        auth.signOut()
        vm.onAction(ProfileSettingsAction.Save)
        runCurrent()
        assertTrue(requests.isEmpty())
        assertEquals("", vm.state.value.displayName)
        assertNull(saved.get<String>("userId"))
        assertFalse(vm.state.value.canSave)
    }

    @Test
    fun anotherUsersSavedFieldsAreNotRestored() = runTest(dispatcher) {
        val vm = ProfileSettingsViewModel(
            SavedStateHandle(mapOf("userId" to "other", "name" to "Other draft")), Auth(), RecordingExecutor(), MutableNoticeEffectDelegate(),
        )
        runCurrent()
        assertEquals(user.displayName, vm.state.value.displayName)
    }

    @Test
    fun saveOwnsReceiptAndNoticeAndRejectsDuplicateActions() = runTest(dispatcher) {
        val result = CompletableDeferred<NotmidProtectedWriteResult>()
        var calls = 0
        val auth = Auth()
        val notices = MutableNoticeEffectDelegate()
        val effects = mutableListOf<NoticeEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { notices.effects.collect { effects.add(it) } }
        val executor = object : NotmidProtectedWriteExecutor {
            override suspend fun execute(authState: NotmidAuthState, request: NotmidProtectedWriteRequest): NotmidProtectedWriteResult {
                calls++
                return result.await()
            }
        }
        val vm = ProfileSettingsViewModel(SavedStateHandle(), auth, executor, notices)
        vm.onAction(ProfileSettingsAction.Save)
        vm.onAction(ProfileSettingsAction.Save)
        runCurrent()
        assertEquals(1, calls)
        assertTrue(vm.state.value.isSaving)
        assertFalse(vm.state.value.canSave)
        result.complete(NotmidProtectedWriteResult.ProfileUpdated(user.copy(displayName = "Server name")))
        runCurrent()
        assertEquals("Server name", auth.currentState().session?.user?.displayName)
        assertEquals("Profile settings saved.", vm.state.value.statusMessage)
        assertFalse(vm.state.value.isSaving)
        assertEquals(1, effects.size)
        assertTrue(effects.single() is NoticeEffect.ShowNotice)
    }

    @Test
    fun lateReceiptAfterLogoutDoesNotRestoreSessionOrShowNotice() = runTest(dispatcher) {
        val result = CompletableDeferred<NotmidProtectedWriteResult>()
        val auth = Auth()
        val notices = MutableNoticeEffectDelegate()
        val effects = mutableListOf<NoticeEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { notices.effects.collect { effects.add(it) } }
        val executor = object : NotmidProtectedWriteExecutor {
            override suspend fun execute(authState: NotmidAuthState, request: NotmidProtectedWriteRequest) = result.await()
        }
        val vm = ProfileSettingsViewModel(SavedStateHandle(), auth, executor, notices)
        vm.onAction(ProfileSettingsAction.Save)
        runCurrent()
        auth.signOut()
        runCurrent()
        result.complete(NotmidProtectedWriteResult.ProfileUpdated(user))
        runCurrent()
        assertNull(auth.currentState().session)
        assertNull(vm.state.value.statusMessage)
        assertEquals("", vm.state.value.displayName)
        assertFalse(vm.state.value.isSaving)
        assertTrue(effects.isEmpty())
    }

    @Test
    fun busyAndCancellationAreSilentButFailureProducesNotice() = runTest(dispatcher) {
        val notices = MutableNoticeEffectDelegate()
        val effects = mutableListOf<NoticeEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { notices.effects.collect { effects.add(it) } }
        var attempt = 0
        val executor = object : NotmidProtectedWriteExecutor {
            override suspend fun execute(authState: NotmidAuthState, request: NotmidProtectedWriteRequest): NotmidProtectedWriteResult = when (attempt++) {
                0 -> NotmidProtectedWriteResult.Busy
                1 -> throw CancellationException("cancel")
                else -> error("private transport details")
            }
        }
        val vm = ProfileSettingsViewModel(SavedStateHandle(), Auth(), executor, notices)
        repeat(2) {
            vm.onAction(ProfileSettingsAction.Save)
            runCurrent()
            assertFalse(vm.state.value.isSaving)
            assertNull(vm.state.value.statusMessage)
            assertTrue(effects.isEmpty())
        }
        vm.onAction(ProfileSettingsAction.Save)
        runCurrent()
        assertFalse(vm.state.value.isSaving)
        assertEquals("This action failed. Try again.", vm.state.value.statusMessage)
        assertEquals(1, effects.size)
    }

    private class RecordingExecutor : NotmidProtectedWriteExecutor {
        val requests = mutableListOf<NotmidProtectedWriteRequest>()
        override suspend fun execute(authState: NotmidAuthState, request: NotmidProtectedWriteRequest): NotmidProtectedWriteResult {
            requests.add(request)
            val update = (request as NotmidProtectedWriteRequest.UpdateProfileSettings).request
            return NotmidProtectedWriteResult.ProfileUpdated(user.copy(displayName = update.displayName, homeNeighborhood = update.homeNeighborhood))
        }
    }

    private class Auth : NotmidAuthGateway {
        override val states = MutableStateFlow(initialAuth)
        override fun currentState() = states.value
        override fun signOut() = states.value.copy(session = null).also { states.value = it }
        override suspend fun signIn(request: NotmidAuthSignInRequest): NotmidAuthResult = error("No sign-in expected")
        override fun applyProfileUpdate(expectedSession: NotmidAuthSession, user: NotmidAuthUser): NotmidAuthState {
            states.value = states.value.copy(session = expectedSession.copy(user = user))
            return states.value
        }
    }

    companion object {
        private val user = NotmidAuthUser("user", "handle", "Name", "Seoul", "", emptyList())
        private val initialAuth = NotmidAuthState(NotmidAuthMode.Fake, NotmidAuthSession("test-token", NotmidAuthProvider.Fake, "", user), emptyList())
        private val destination = NotmidDestination("profile", "Profile", "Saved", NotmidNavigationIcon.Inbox, emptyList(), emptyList())
    }
}
