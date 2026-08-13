package app.thdev.glassnavlab.core.designsystem.component.liquidglass

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

@Immutable
data class LiquidGlassNavigationStyle(
    val height: Dp,
    val outerPadding: PaddingValues,
    val contentPadding: PaddingValues,
    val itemHeight: Dp,
    val selectedPillHeight: Dp,
    val selectedPillWidthFraction: Float,
    val actionButtonSize: Dp,
    val actionButtonSpacing: Dp,
    val containerShape: Shape,
    val itemShape: Shape,
    val actionButtonShape: Shape,
    val shadowElevation: Dp,
    val borderWidth: Dp,
    val borderColor: Color,
    val containerSurfaceColor: Color,
    val selectedSurfaceColor: Color,
    val menuSurfaceColor: Color,
    val selectedContainerColor: Color,
    val selectedContentColor: Color,
    val unselectedContentColor: Color,
)
