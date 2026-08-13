package app.thdev.glassnavlab.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.thdev.glassnavlab.core.designsystem.component.liquidglass.LiquidGlassBottomNavigation
import app.thdev.glassnavlab.core.designsystem.component.liquidglass.LiquidGlassNavigationAction
import app.thdev.glassnavlab.core.designsystem.component.liquidglass.LiquidGlassNavigationItem
import app.thdev.glassnavlab.core.designsystem.component.liquidglass.LiquidGlassRenderMode
import app.thdev.glassnavlab.core.designsystem.component.liquidglass.rememberLiquidGlassNavigationState
import com.kyant.backdrop.Backdrop

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
