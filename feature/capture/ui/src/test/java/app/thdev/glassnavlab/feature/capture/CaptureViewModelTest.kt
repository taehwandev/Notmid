package app.thdev.glassnavlab.feature.capture

import androidx.lifecycle.SavedStateHandle
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentSnapshot
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentUpdates
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteRequest
import app.thdev.glassnavlab.core.auth.notmid.*
import app.thdev.glassnavlab.core.model.notmid.*
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteExecutor
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteResult
import app.thdev.glassnavlab.core.notice.api.effect.MutableNoticeEffectDelegate
import app.thdev.glassnavlab.core.model.notmid.NotmidCaptureDraft
import app.thdev.glassnavlab.core.model.notmid.NotmidCaptureMediaState
import app.thdev.glassnavlab.core.model.notmid.NotmidCaptureVisibility
import app.thdev.glassnavlab.core.model.notmid.NotmidDestination
import app.thdev.glassnavlab.core.model.notmid.NotmidNavigationIcon
import app.thdev.glassnavlab.core.model.notmid.NotmidPlace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import app.thdev.glassnavlab.core.notice.api.effect.NoticeEffect
import app.thdev.glassnavlab.core.notice.api.model.NoticePresentation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CaptureViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun permissionResultBeforeContentDoesNotEraseRestoredDraft() = runTest(dispatcher) {
        val source = Source()
        val saved = SavedStateHandle(mapOf("draftId" to "draft", "caption" to "Restored", "tags" to arrayListOf("night"), "public" to false))
        val vm = CaptureViewModel(saved, source, Executor(), CapturePlatformRequests(), Auth(), MutableNoticeEffectDelegate())
        vm.onAction(CaptureAction.PermissionChanged(true))
        runCurrent()
        assertEquals("Restored", vm.state.value.caption)
        assertEquals(setOf("night"), vm.state.value.selectedTags)
        assertFalse(vm.state.value.publicReceipt)
        source.snapshot.value = NotmidContentSnapshot.Ready(listOf(destination.copy(title = "Updated")))
        runCurrent()
        assertEquals("Restored", vm.state.value.caption)
        source.snapshot.value = NotmidContentSnapshot.Ready(listOf(destination.copy(captureDraft = draft.copy(id = "new", caption = "New draft"))))
        runCurrent()
        assertEquals("New draft", vm.state.value.caption)
    }

    @Test
    fun publishRequiresMediaAndUsesLatestEditsAndVisibility() = runTest(dispatcher) {
        val source = Source()
        val writes = Executor()
        val requests = writes.requests
        val vm = CaptureViewModel(SavedStateHandle(), source, writes, CapturePlatformRequests(), Auth(), MutableNoticeEffectDelegate())
        runCurrent()
        vm.onAction(CaptureAction.Publish)
        runCurrent()
        assertTrue(requests.isEmpty())
        assertFalse(vm.state.value.readyToPublish)
        vm.onAction(CaptureAction.PhotoCaptured("local.jpg"))
        vm.onAction(CaptureAction.CaptionChanged("Edited"))
        vm.onAction(CaptureAction.VisibilityChanged(false))
        vm.onAction(CaptureAction.TagSelected("night"))
        vm.onAction(CaptureAction.Publish)
        runCurrent()
        val request = (requests.single() as NotmidProtectedWriteRequest.PublishCapture).request
        assertEquals("Edited", request.caption)
        assertEquals("place", request.placeId)
        assertEquals(listOf("food", "night"), request.moodTags)
        assertEquals(NotmidCaptureVisibility.Private, request.visibility)
        source.snapshot.value = NotmidContentSnapshot.Unavailable
        vm.onAction(CaptureAction.Publish)
        runCurrent()
        assertEquals(1, requests.size)
    }

    @Test
    fun platformCommandsFollowViewModelPermissionAndResults() = runTest(dispatcher) {
        val platform = CapturePlatformRequests()
        val commands = mutableListOf<CapturePlatformRequests.Request>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { platform.requests.collect { commands.add(it) } }
        val vm = CaptureViewModel(SavedStateHandle(), Source(), Executor(), platform, Auth(), MutableNoticeEffectDelegate())
        vm.onAction(CaptureAction.TakePhoto)
        vm.onAction(CaptureAction.RequestPermission)
        runCurrent()
        assertEquals(listOf(CapturePlatformRequests.Request.Permission), commands)
        vm.onAction(CaptureAction.PermissionChanged(true))
        vm.onAction(CaptureAction.TakePhoto)
        runCurrent()
        assertEquals(CapturePlatformRequests.Request.Photo, commands.last())
        vm.onAction(CaptureAction.PhotoFailed)
        assertEquals(CaptureCameraStatus.Failed, vm.state.value.camera.status)
        vm.onAction(CaptureAction.PhotoCaptured("proof.jpg"))
        assertEquals("proof.jpg", vm.state.value.capturedMediaName)
        assertEquals(CaptureCameraStatus.Previewing, vm.state.value.camera.status)
        platform.close()
    }

    @Test
    fun publishOwnsProgressSuccessNoticeAndRejectsDuplicateActions() = runTest(dispatcher) {
        val completion = CompletableDeferred<NotmidProtectedWriteResult>()
        val writes = Executor { completion.await() }
        val notices = MutableNoticeEffectDelegate()
        val effects = mutableListOf<NoticeEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { notices.effects.collect { effects.add(it) } }
        val vm = CaptureViewModel(SavedStateHandle(), Source(), writes, CapturePlatformRequests(), Auth(), notices)
        runCurrent()
        vm.onAction(CaptureAction.PhotoCaptured("receipt.jpg"))
        vm.onAction(CaptureAction.Publish)
        vm.onAction(CaptureAction.Publish)
        assertTrue(vm.state.value.isPublishing)
        runCurrent()
        assertEquals(1, writes.requests.size)
        completion.complete(NotmidProtectedWriteResult.Completed)
        runCurrent()
        assertFalse(vm.state.value.isPublishing)
        assertEquals("Receipt queued for moderation.", vm.state.value.publishStatusMessage)
        assertEquals(NoticePresentation.Toast, (effects.single() as NoticeEffect.ShowNotice).notice.presentation)
    }

    @Test
    fun busyAndCancellationReleaseProgressWhileFailureShowsAlert() = runTest(dispatcher) {
        var attempt = 0
        val writes = Executor {
            when (attempt++) {
                0 -> NotmidProtectedWriteResult.Busy
                1 -> throw CancellationException("cancel")
                else -> error("private transport detail")
            }
        }
        val notices = MutableNoticeEffectDelegate()
        val effects = mutableListOf<NoticeEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { notices.effects.collect { effects.add(it) } }
        val vm = CaptureViewModel(SavedStateHandle(), Source(), writes, CapturePlatformRequests(), Auth(), notices)
        runCurrent()
        vm.onAction(CaptureAction.PhotoCaptured("receipt.jpg"))
        repeat(2) {
            vm.onAction(CaptureAction.Publish)
            runCurrent()
            assertFalse(vm.state.value.isPublishing)
            assertNull(vm.state.value.publishStatusMessage)
            assertTrue(effects.isEmpty())
        }
        vm.onAction(CaptureAction.Publish)
        runCurrent()
        assertFalse(vm.state.value.isPublishing)
        assertEquals("This action failed. Try again.", vm.state.value.publishStatusMessage)
        assertEquals(NoticePresentation.Alert, (effects.single() as NoticeEffect.ShowNotice).notice.presentation)
    }

    @Test
    fun lateResultAfterLogoutIsSilentAndDoesNotEraseDraft() = runTest(dispatcher) {
        val completion = CompletableDeferred<NotmidProtectedWriteResult>()
        val auth = Auth()
        val notices = MutableNoticeEffectDelegate()
        val effects = mutableListOf<NoticeEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { notices.effects.collect { effects.add(it) } }
        val vm = CaptureViewModel(SavedStateHandle(), Source(), Executor { completion.await() }, CapturePlatformRequests(), auth, notices)
        runCurrent()
        vm.onAction(CaptureAction.PhotoCaptured("receipt.jpg"))
        vm.onAction(CaptureAction.Publish)
        runCurrent()
        auth.signOut()
        completion.complete(NotmidProtectedWriteResult.Completed)
        runCurrent()
        assertFalse(vm.state.value.isPublishing)
        assertNull(vm.state.value.publishStatusMessage)
        assertTrue(effects.isEmpty())
        assertEquals("Caption", vm.state.value.caption)
    }

    private class Executor(
        private val response: suspend () -> NotmidProtectedWriteResult = { NotmidProtectedWriteResult.Completed },
    ) : NotmidProtectedWriteExecutor {
        val requests = mutableListOf<NotmidProtectedWriteRequest>()
        override suspend fun execute(authState: NotmidAuthState, request: NotmidProtectedWriteRequest): NotmidProtectedWriteResult {
            requests.add(request)
            return response()
        }
    }

    private class Auth : NotmidAuthGateway {
        override val states = MutableStateFlow(NotmidAuthState(NotmidAuthMode.Fake,
            NotmidAuthSession("token", NotmidAuthProvider.Fake, "", NotmidAuthUser("user", "handle", "Name", "Seoul", "", emptyList())), emptyList()))
        override fun currentState() = states.value
        override fun signOut() = states.value.copy(session = null).also { states.value = it }
        override suspend fun signIn(request: NotmidAuthSignInRequest): NotmidAuthResult = error("No sign-in expected")
        override fun applyProfileUpdate(expectedSession: NotmidAuthSession, user: NotmidAuthUser): NotmidAuthState = error("No profile write expected")
    }

    private class Source : NotmidContentUpdates {
        override val snapshot = MutableStateFlow<NotmidContentSnapshot>(NotmidContentSnapshot.Ready(listOf(destination)))
    }

    companion object {
        private val draft = NotmidCaptureDraft("draft", "Caption", "place", listOf("food"), NotmidCaptureVisibility.Public, NotmidCaptureMediaState.Empty, "Draft", "", "", "")
        private val place = NotmidPlace("Place", "Description", "Metric", emptyList(), 100, id = "place")
        private val destination = NotmidDestination("capture", "Capture", "Proof", NotmidNavigationIcon.Inbox, emptyList(), listOf(place), captureDraft = draft)
    }
}
