package app.thdev.glassnavlab.feature.capture

import androidx.lifecycle.SavedStateHandle
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentSnapshot
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentUpdates
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteRequest
import app.thdev.glassnavlab.core.model.notmid.ChannelNotmidActionDelegate
import app.thdev.glassnavlab.core.model.notmid.NotmidCaptureDraft
import app.thdev.glassnavlab.core.model.notmid.NotmidCaptureMediaState
import app.thdev.glassnavlab.core.model.notmid.NotmidCaptureVisibility
import app.thdev.glassnavlab.core.model.notmid.NotmidDestination
import app.thdev.glassnavlab.core.model.notmid.NotmidNavigationIcon
import app.thdev.glassnavlab.core.model.notmid.NotmidPlace
import kotlinx.coroutines.Dispatchers
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
        val vm = CaptureViewModel(saved, source, ChannelNotmidActionDelegate(), CapturePlatformRequests())
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
        val writes = ChannelNotmidActionDelegate<NotmidProtectedWriteRequest>()
        val requests = mutableListOf<NotmidProtectedWriteRequest>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { writes.actions.collect { requests.add(it) } }
        val vm = CaptureViewModel(SavedStateHandle(), source, writes, CapturePlatformRequests())
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
        writes.close()
    }

    @Test
    fun platformCommandsFollowViewModelPermissionAndResults() = runTest(dispatcher) {
        val platform = CapturePlatformRequests()
        val commands = mutableListOf<CapturePlatformRequests.Request>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { platform.requests.collect { commands.add(it) } }
        val vm = CaptureViewModel(SavedStateHandle(), Source(), ChannelNotmidActionDelegate(), platform)
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

    private class Source : NotmidContentUpdates {
        override val snapshot = MutableStateFlow<NotmidContentSnapshot>(NotmidContentSnapshot.Ready(listOf(destination)))
    }

    companion object {
        private val draft = NotmidCaptureDraft("draft", "Caption", "place", listOf("food"), NotmidCaptureVisibility.Public, NotmidCaptureMediaState.Empty, "Draft", "", "", "")
        private val place = NotmidPlace("Place", "Description", "Metric", emptyList(), 100, id = "place")
        private val destination = NotmidDestination("capture", "Capture", "Proof", NotmidNavigationIcon.Inbox, emptyList(), listOf(place), captureDraft = draft)
    }
}
