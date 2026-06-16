package app.thdev.glassnavlab.core.designsystem.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.thdev.glassnavlab.core.designsystem.component.liquidglass.LiquidGlassBottomNavigation
import app.thdev.glassnavlab.core.designsystem.component.liquidglass.LiquidGlassNavigationAction
import app.thdev.glassnavlab.core.designsystem.component.liquidglass.LiquidGlassNavigationDefaults
import app.thdev.glassnavlab.core.designsystem.component.liquidglass.LiquidGlassNavigationItem
import app.thdev.glassnavlab.core.designsystem.component.liquidglass.LiquidGlassNavigationStyle
import app.thdev.glassnavlab.core.designsystem.component.liquidglass.LiquidGlassRenderMode
import app.thdev.glassnavlab.core.designsystem.component.liquidglass.rememberLiquidGlassNavigationState
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme
import com.kyant.backdrop.Backdrop

@Immutable
data class NotmidBottomNavigationItem(
    val id: String,
    val label: String,
    val icon: @Composable (selected: Boolean, contentColor: Color) -> Unit,
)

@Composable
fun NotmidBottomNavigation(
    items: List<NotmidBottomNavigationItem>,
    selectedItemId: String,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    adaptiveBackgroundColor: Color = Color.Unspecified,
    renderMode: LiquidGlassRenderMode = LiquidGlassRenderMode.Automatic,
    trailingAction: LiquidGlassNavigationAction? = null,
    onItemSelected: (NotmidBottomNavigationItem) -> Unit = {},
) {
    if (items.isEmpty()) return

    val navigationItems = remember(items) {
        items.map { item ->
            LiquidGlassNavigationItem(
                id = item.id,
                label = item.label,
                icon = item.icon,
            )
        }
    }
    val state = rememberLiquidGlassNavigationState(
        initialSelectedItemId = selectedItemId,
    )
    LaunchedEffect(selectedItemId) {
        if (state.selectedItemId != selectedItemId) {
            state.select(selectedItemId)
        }
    }

    LiquidGlassBottomNavigation(
        items = navigationItems,
        backdrop = backdrop,
        modifier = modifier,
        state = state,
        style = NotmidBottomNavigationDefaults.style(),
        renderMode = renderMode,
        adaptiveBackgroundColor = adaptiveBackgroundColor,
        trailingAction = trailingAction,
        onItemSelected = { item ->
            items.firstOrNull { it.id == item.id }?.let(onItemSelected)
        },
    )
}

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
