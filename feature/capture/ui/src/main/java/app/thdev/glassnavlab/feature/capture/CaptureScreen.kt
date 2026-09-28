package app.thdev.glassnavlab.feature.capture

import android.Manifest
import android.content.Intent
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun CaptureScreen() {
    val viewModel: CaptureViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val cameraController = rememberCaptureCameraController()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.onAction(CaptureAction.PermissionChanged(it)) }
    val mediaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            val fileName = runCatching {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0) else null
                    }
            }.getOrNull() ?: "Selected image"
            viewModel.onAction(CaptureAction.MediaSelected(uri.toString(), fileName))
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onAction(CaptureAction.PermissionChanged(context.isCaptureCameraPermissionGranted()))
    }
    LaunchedEffect(viewModel, lifecycle, context, cameraController, permissionLauncher, mediaLauncher) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.platformRequests.collect { request ->
                when (request) {
                    CapturePlatformRequests.Request.Permission -> permissionLauncher.launch(Manifest.permission.CAMERA)
                    CapturePlatformRequests.Request.Photo -> cameraController.captureStill(
                        context = context,
                        onCaptured = { viewModel.onAction(CaptureAction.PhotoCaptured(it.fileName, it.uriString)) },
                        onError = { viewModel.onAction(CaptureAction.PhotoFailed) },
                    )
                    CapturePlatformRequests.Request.PickMedia -> mediaLauncher.launch(arrayOf("image/*"))
                }
            }
        }
    }
    CaptureContent(state, listState, cameraController, viewModel::onAction)
}
