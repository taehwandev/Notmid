package app.thdev.glassnavlab.feature.capture

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.thdev.glassnavlab.core.designsystem.component.NotmidSectionHeader
import app.thdev.glassnavlab.core.designsystem.theme.NotmidColorTokens
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidCaptureMediaState
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidCaptureVisibility
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidDestination

@Composable
fun CaptureScreen(
    destination: NotmidDestination,
    listState: LazyListState,
    isPublishing: Boolean = false,
    publishStatusMessage: String? = null,
    onPublish: (
        draftId: String,
        caption: String,
        placeId: String,
        moodTags: List<String>,
        visibility: String,
    ) -> Unit = { _, _, _, _, _ -> },
) {
    val draft = destination.captureDraft
    var caption by rememberSaveable(draft?.id) {
        mutableStateOf(draft?.caption.orEmpty())
    }
    var selectedTags by rememberSaveable(draft?.id) {
        mutableStateOf(draft?.moodTags?.toSet() ?: emptySet())
    }
    var publicReceipt by rememberSaveable(draft?.id) {
        mutableStateOf(draft?.visibility?.let { it == NotmidCaptureVisibility.Public } ?: true)
    }
    var draftStatus by rememberSaveable(draft?.id) {
        mutableStateOf(draft?.statusLabel ?: "Draft saved locally")
    }
    val context = LocalContext.current
    val cameraController = rememberCaptureCameraController()
    var cameraMode by rememberSaveable {
        mutableStateOf(CaptureCameraMode.Camera)
    }
    var cameraLens by rememberSaveable {
        mutableStateOf(CaptureCameraLens.Back)
    }
    var cameraPermissionGranted by rememberSaveable {
        mutableStateOf(context.isCaptureCameraPermissionGranted())
    }
    var cameraStatus by rememberSaveable {
        mutableStateOf(
            if (cameraPermissionGranted) {
                CaptureCameraStatus.Starting
            } else {
                CaptureCameraStatus.PermissionRequired
            },
        )
    }
    var capturedMediaName by rememberSaveable(draft?.id) {
        mutableStateOf<String?>(null)
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            cameraPermissionGranted = granted
            cameraStatus = if (granted) {
                CaptureCameraStatus.Starting
            } else {
                CaptureCameraStatus.PermissionRequired
            }
        },
    )
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val granted = context.isCaptureCameraPermissionGranted()
        cameraPermissionGranted = granted
        cameraStatus = when {
            !granted -> CaptureCameraStatus.PermissionRequired
            cameraStatus == CaptureCameraStatus.PermissionRequired -> CaptureCameraStatus.Starting
            else -> cameraStatus
        }
    }

    val attachedPlace = draft?.placeId?.let { placeId ->
        destination.places.firstOrNull { it.id == placeId }
    } ?: destination.places.firstOrNull()
    val mediaAttached = capturedMediaName != null ||
        draft?.mediaState?.let { it != NotmidCaptureMediaState.Empty } == true
    val readyToPublish = mediaAttached &&
        caption.isNotBlank() &&
        selectedTags.isNotEmpty() &&
        attachedPlace != null
    val cameraState = CaptureCameraUiState(
        mode = cameraMode,
        lens = cameraLens,
        permissionGranted = cameraPermissionGranted,
        status = cameraStatus,
    )

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
                onCameraModeChange = { mode ->
                    cameraMode = mode
                    cameraStatus = when {
                        mode == CaptureCameraMode.Upload -> cameraStatus
                        cameraPermissionGranted -> CaptureCameraStatus.Starting
                        else -> CaptureCameraStatus.PermissionRequired
                    }
                },
                onCameraLensChange = { lens ->
                    cameraLens = lens
                },
                onRequestCameraPermission = {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                },
                onCameraStatusChange = { status ->
                    cameraStatus = status
                },
                onCapturePhoto = {
                    cameraController.captureStill(
                        context = context,
                        onCaptured = { media ->
                            capturedMediaName = media.fileName
                            draftStatus = "Photo captured locally"
                            cameraStatus = CaptureCameraStatus.Previewing
                        },
                        onError = {
                            draftStatus = "Camera capture failed"
                            cameraStatus = CaptureCameraStatus.Failed
                        },
                    )
                },
            )
        }

        item(key = "capture-composer-${destination.id}") {
            CaptureComposerPanel(
                draft = draft,
                caption = caption,
                onCaptionChange = { caption = it },
                selectedTags = selectedTags,
                onTagSelected = { tag ->
                    selectedTags = if (tag in selectedTags) {
                        selectedTags - tag
                    } else {
                        selectedTags + tag
                    }
                },
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
                onPublicReceiptChange = { publicReceipt = it },
                draftStatus = publishStatusMessage ?: draftStatus,
                onSaveDraft = { draftStatus = "Draft updated just now" },
                onPublish = {
                    val currentDraft = draft
                    val currentPlace = attachedPlace
                    if (
                        currentDraft != null &&
                        currentPlace != null &&
                        caption.isNotBlank() &&
                        selectedTags.isNotEmpty()
                    ) {
                        draftStatus = "Publishing receipt..."
                        onPublish(
                            currentDraft.id,
                            caption,
                            currentPlace.id,
                            selectedTags.toList(),
                            if (publicReceipt) "public" else "private",
                        )
                    } else {
                        draftStatus = "Attach a place, caption, and at least one tag"
                    }
                },
            )
        }
    }
}
