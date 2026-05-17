package app.thdev.glassnavlab.core.designsystem.component.liquidglass

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class LiquidGlassRenderMode {
    Automatic,
    Agsl,
    Legacy,
}

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

object LiquidGlassNavigationDefaults {
    fun style(
        height: Dp = 62.dp,
        outerPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        itemHeight: Dp = 54.dp,
        selectedPillHeight: Dp = 48.dp,
        selectedPillWidthFraction: Float = 0.96f,
        actionButtonSize: Dp = 62.dp,
        actionButtonSpacing: Dp = 10.dp,
        containerShape: Shape = RoundedCornerShape(percent = 50),
        itemShape: Shape = RoundedCornerShape(percent = 50),
        actionButtonShape: Shape = RoundedCornerShape(percent = 50),
        shadowElevation: Dp = 22.dp,
        borderWidth: Dp = 1.dp,
        borderColor: Color = Color.White.copy(alpha = 0.34f),
        containerSurfaceColor: Color = Color.White.copy(alpha = 0.38f),
        selectedSurfaceColor: Color = Color.White.copy(alpha = 0.68f),
        menuSurfaceColor: Color = Color.White.copy(alpha = 0.64f),
        selectedContainerColor: Color = Color.Transparent,
        selectedContentColor: Color = Color(0xFF111111),
        unselectedContentColor: Color = Color(0xFF2F343A).copy(alpha = 0.52f),
    ): LiquidGlassNavigationStyle {
        return LiquidGlassNavigationStyle(
            height = height,
            outerPadding = outerPadding,
            contentPadding = contentPadding,
            itemHeight = itemHeight,
            selectedPillHeight = selectedPillHeight,
            selectedPillWidthFraction = selectedPillWidthFraction,
            actionButtonSize = actionButtonSize,
            actionButtonSpacing = actionButtonSpacing,
            containerShape = containerShape,
            itemShape = itemShape,
            actionButtonShape = actionButtonShape,
            shadowElevation = shadowElevation,
            borderWidth = borderWidth,
            borderColor = borderColor,
            containerSurfaceColor = containerSurfaceColor,
            selectedSurfaceColor = selectedSurfaceColor,
            menuSurfaceColor = menuSurfaceColor,
            selectedContainerColor = selectedContainerColor,
            selectedContentColor = selectedContentColor,
            unselectedContentColor = unselectedContentColor,
        )
    }
}
