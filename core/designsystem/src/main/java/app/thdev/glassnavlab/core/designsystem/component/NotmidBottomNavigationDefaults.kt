package app.thdev.glassnavlab.core.designsystem.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.thdev.glassnavlab.core.designsystem.component.liquidglass.LiquidGlassNavigationDefaults
import app.thdev.glassnavlab.core.designsystem.component.liquidglass.LiquidGlassNavigationStyle
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme

object NotmidBottomNavigationDefaults {
    @Composable
    fun style(): LiquidGlassNavigationStyle {
        return LiquidGlassNavigationDefaults.style(
            height = 62.dp,
            outerPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            itemHeight = 54.dp,
            selectedPillHeight = 48.dp,
            actionButtonSize = 62.dp,
            actionButtonSpacing = 10.dp,
            borderColor = NotmidTheme.colors.glassStroke,
            containerSurfaceColor = NotmidTheme.colors.glassLight.copy(alpha = 0.18f),
            selectedSurfaceColor = Color(0xFFE1E5EA).copy(alpha = 0.72f),
            menuSurfaceColor = NotmidTheme.colors.glassLight,
            selectedContentColor = NotmidTheme.colors.content,
            unselectedContentColor = NotmidTheme.colors.contentMuted.copy(alpha = 0.62f),
        )
    }
}
