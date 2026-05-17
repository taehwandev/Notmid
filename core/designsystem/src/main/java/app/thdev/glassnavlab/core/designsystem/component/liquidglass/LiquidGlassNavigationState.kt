package app.thdev.glassnavlab.core.designsystem.component.liquidglass

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable

@Stable
class LiquidGlassNavigationState(initialSelectedItemId: String) {
    var selectedItemId by mutableStateOf(initialSelectedItemId)
        private set

    fun select(itemId: String) {
        selectedItemId = itemId
    }

    companion object {
        val Saver: Saver<LiquidGlassNavigationState, String> = Saver(
            save = { it.selectedItemId },
            restore = { LiquidGlassNavigationState(it) },
        )
    }
}

@Composable
fun rememberLiquidGlassNavigationState(
    initialSelectedItemId: String,
): LiquidGlassNavigationState {
    return rememberSaveable(
        initialSelectedItemId,
        saver = LiquidGlassNavigationState.Saver,
    ) {
        LiquidGlassNavigationState(initialSelectedItemId)
    }
}
