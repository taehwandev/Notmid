package app.thdev.glassnavlab.core.designsystem.component.liquidglass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

@Composable
fun LiquidGlassBackdropHost(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFFF3F1EC),
    content: @Composable BoxScope.() -> Unit,
    floatingContent: @Composable BoxScope.(backdrop: Backdrop) -> Unit,
) {
    val backdrop = rememberLayerBackdrop {
        drawRect(backgroundColor)
        drawContent()
    }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
            content = content,
        )

        floatingContent(backdrop)
    }
}
