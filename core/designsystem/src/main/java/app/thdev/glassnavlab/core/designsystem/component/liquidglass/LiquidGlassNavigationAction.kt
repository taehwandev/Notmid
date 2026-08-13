package app.thdev.glassnavlab.core.designsystem.component.liquidglass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class LiquidGlassNavigationAction(
    val contentDescription: String,
    val icon: @Composable (contentColor: Color) -> Unit,
    val selected: Boolean = false,
    val onClick: () -> Unit,
)
