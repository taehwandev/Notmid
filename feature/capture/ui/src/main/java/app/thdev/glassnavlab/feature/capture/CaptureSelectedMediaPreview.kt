package app.thdev.glassnavlab.feature.capture

import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun CaptureSelectedMediaPreview(uriString: String, modifier: Modifier = Modifier) {
    val resolver = LocalContext.current.contentResolver
    val bitmap = produceState<android.graphics.Bitmap?>(null, uriString, resolver) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val source = ImageDecoder.createSource(resolver, Uri.parse(uriString))
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    val width = info.size.width
                    val height = info.size.height
                    val scale = maxOf(1, maxOf(width, height) / 1024)
                    decoder.setTargetSize(maxOf(1, width / scale), maxOf(1, height / scale))
                }
            }.getOrNull()
        }
    }.value
    Box(modifier) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Selected receipt image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
