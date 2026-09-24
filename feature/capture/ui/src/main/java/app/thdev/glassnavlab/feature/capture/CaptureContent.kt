package app.thdev.glassnavlab.feature.capture

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.thdev.glassnavlab.core.designsystem.theme.notmidTheme
import app.thdev.glassnavlab.core.model.notmid.NotmidNavigationIcon
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidDestination
import app.thdev.glassnavlab.core.designsystem.component.NotmidSectionHeader
import app.thdev.glassnavlab.core.designsystem.theme.NotmidColorTokens
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme

@Composable
internal fun CaptureContent(
    state: CaptureUiState,
    listState: LazyListState,
    cameraController: CaptureCameraController,
    isPublishing: Boolean,
    publishStatusMessage: String?,
    onAction: (CaptureAction) -> Unit,
) {
    val destination = state.destination ?: return
    val draft = destination.captureDraft
    val caption = state.caption
    val selectedTags = state.selectedTags
    val publicReceipt = state.publicReceipt
    val draftStatus = state.draftStatus
    val capturedMediaName = state.capturedMediaName
    val readyToPublish = state.readyToPublish
    val attachedPlace = state.attachedPlace
    val cameraState = state.camera
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NotmidColorTokens.WarmMist),
        state = listState,
        contentPadding = PaddingValues(
            start = NotmidTheme.spacing.screenHorizontal,
            top = NotmidTheme.spacing.screenTop,
            end = NotmidTheme.spacing.screenHorizontal,
            bottom = NotmidTheme.spacing.bottomNavigationPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.lg),
    ) {
        item(key = "capture-header-${destination.id}") {
            NotmidSectionHeader(
                title = "show the receipt",
                subtitle = "Attach a place, add the proof, and publish a short local draft.",
                eyebrow = destination.title,
            )
        }

        item(key = "capture-media-${destination.id}") {
            CaptureMediaSurface(
                destination = destination,
                draft = draft,
                readyToPublish = readyToPublish,
                cameraState = cameraState,
                cameraController = cameraController,
                capturedMediaName = capturedMediaName,
                onCameraModeChange = { onAction(CaptureAction.ModeChanged(it)) },
                onCameraLensChange = { onAction(CaptureAction.LensChanged(it)) },
                onRequestCameraPermission = { onAction(CaptureAction.RequestPermission) },
                onCameraStatusChange = { onAction(CaptureAction.CameraStatusChanged(it)) },
                onCapturePhoto = { onAction(CaptureAction.TakePhoto) },
            )
        }

        item(key = "capture-composer-${destination.id}") {
            CaptureComposerPanel(
                draft = draft,
                caption = caption,
                onCaptionChange = { onAction(CaptureAction.CaptionChanged(it)) },
                selectedTags = selectedTags,
                onTagSelected = { onAction(CaptureAction.TagSelected(it)) },
            )
        }

        item(key = "capture-place-${destination.id}") {
            CapturePlaceAttachment(place = attachedPlace)
        }

        item(key = "capture-publish-${destination.id}") {
            CapturePublishPanel(
                readyToPublish = readyToPublish,
                isPublishing = isPublishing,
                publicReceipt = publicReceipt,
                onPublicReceiptChange = { onAction(CaptureAction.VisibilityChanged(it)) },
                draftStatus = publishStatusMessage ?: draftStatus,
                onSaveDraft = { onAction(CaptureAction.SaveDraft) },
                onPublish = { onAction(CaptureAction.Publish) },
            )
        }
    }
}

@Preview
@Composable
private fun CapturePermissionRequiredPreview() {
    notmidTheme {
        CaptureContent(
            state = CaptureUiState(destination = NotmidDestination(
                "capture", "Capture", "Proof", NotmidNavigationIcon.Inbox,
                emptyList(), emptyList(), emptyList(), null,
            )),
            listState = rememberLazyListState(),
            cameraController = rememberCaptureCameraController(),
            isPublishing = false,
            publishStatusMessage = null,
            onAction = {},
        )
    }
}
