package app.thdev.glassnavlab.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme

@Composable
fun NotmidSurface(
    modifier: Modifier = Modifier,
    shape: Shape = NotmidTheme.shapes.card,
    color: Color = NotmidTheme.colors.surfaceRaised,
    contentColor: Color = NotmidTheme.colors.content,
    tonalElevation: Dp = NotmidTheme.elevation.none,
    shadowElevation: Dp = NotmidTheme.elevation.none,
    border: BorderStroke? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        border = border,
        content = content,
    )
}

@Composable
fun NotmidCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(NotmidTheme.spacing.lg),
    containerColor: Color = NotmidTheme.colors.surfaceRaised,
    contentColor: Color = NotmidTheme.colors.content,
    border: BorderStroke? = BorderStroke(NotmidCardBorderWidth, NotmidTheme.colors.line),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = NotmidTheme.shapes.card,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        border = border,
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}

@Composable
fun NotmidHorizontalDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = DividerDefaults.Thickness,
    color: Color = NotmidTheme.colors.line,
) {
    HorizontalDivider(
        modifier = modifier,
        thickness = thickness,
        color = color,
    )
}

private val NotmidCardBorderWidth = 1.dp
