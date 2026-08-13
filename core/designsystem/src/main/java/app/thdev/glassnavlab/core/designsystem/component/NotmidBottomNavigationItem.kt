package app.thdev.glassnavlab.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class NotmidBottomNavigationItem(
    val id: String,
    val label: String,
    val icon: @Composable (selected: Boolean, contentColor: Color) -> Unit,
)
