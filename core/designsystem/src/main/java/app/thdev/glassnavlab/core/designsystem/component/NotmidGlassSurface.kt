package app.thdev.glassnavlab.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme

@Composable
fun NotmidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = NotmidTheme.shapes.cardLarge,
    backgroundColor: Color = NotmidTheme.colors.glassLight,
    borderColor: Color = NotmidTheme.colors.glassStroke,
    contentPadding: PaddingValues = PaddingValues(),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(
                border = BorderStroke(width = NotmidGlassBorderWidth, color = borderColor),
                shape = shape,
            )
            .padding(contentPadding),
        content = content,
    )
}

private val NotmidGlassBorderWidth = 1.dp
