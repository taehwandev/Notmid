package app.thdev.glassnavlab.feature.profile

import androidx.lifecycle.SavedStateHandle
import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthGateway
import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthResult
import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthSignInRequest
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentSnapshot
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentUpdates
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteRequest
import app.thdev.glassnavlab.core.model.notmid.*
import app.thdev.glassnavlab.core.navigation.notmid.NotmidRouteEvent
import app.thdev.glassnavlab.core.navigation.runtime.RouteEvent
import app.thdev.glassnavlab.core.navigation.runtime.RouteEventSink
import kotlinx.coroutines.Dispatchers
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
        val writes = ChannelNotmidActionDelegate<NotmidProtectedWriteRequest>()
        val requests = mutableListOf<NotmidProtectedWriteRequest>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { writes.actions.collect { requests.add(it) } }
        val vm = ProfileSettingsViewModel(saved, auth, writes)
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
        writes.close()
    }

    @Test
    fun latestLogoutBlocksSaveBeforeCollectorRunsAndClearsDraft() = runTest(dispatcher) {
        val auth = Auth()
        val writes = ChannelNotmidActionDelegate<NotmidProtectedWriteRequest>()
        val requests = mutableListOf<NotmidProtectedWriteRequest>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { writes.actions.collect { requests.add(it) } }
        val saved = SavedStateHandle()
        val vm = ProfileSettingsViewModel(saved, auth, writes)
        vm.onAction(ProfileSettingsAction.DisplayNameChanged("Private draft"))
        auth.signOut()
        vm.onAction(ProfileSettingsAction.Save)
        runCurrent()
        assertTrue(requests.isEmpty())
        assertEquals("", vm.state.value.displayName)
        assertNull(saved.get<String>("userId"))
        assertFalse(vm.state.value.canSave)
        writes.close()
    }

    @Test
    fun anotherUsersSavedFieldsAreNotRestored() = runTest(dispatcher) {
        val vm = ProfileSettingsViewModel(
            SavedStateHandle(mapOf("userId" to "other", "name" to "Other draft")), Auth(), ChannelNotmidActionDelegate(),
        )
        runCurrent()
        assertEquals(user.displayName, vm.state.value.displayName)
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
