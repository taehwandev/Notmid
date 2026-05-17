package app.thdev.glassnavlab.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme

@Immutable
data class NotmidActionRailItem(
    val id: String,
    val label: String,
    val value: String? = null,
    val selected: Boolean = false,
    val icon: @Composable (contentColor: Color) -> Unit,
    val onClick: () -> Unit,
)

@Composable
fun NotmidActionRail(
    items: List<NotmidActionRailItem>,
    modifier: Modifier = Modifier,
    darkOnMedia: Boolean = true,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items.forEach { item ->
            NotmidActionRailButton(
                item = item,
                darkOnMedia = darkOnMedia,
            )
        }
    }
}

@Composable
private fun NotmidActionRailButton(
    item: NotmidActionRailItem,
    darkOnMedia: Boolean,
) {
    val selectedColor = if (item.selected) NotmidTheme.colors.clip else NotmidTheme.colors.contentOnMedia
    val contentColor = if (darkOnMedia) selectedColor else NotmidTheme.colors.content
    val backgroundColor = if (darkOnMedia) {
        NotmidTheme.colors.glassDark
    } else {
        NotmidTheme.colors.glassLight
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.xs),
    ) {
        NotmidGlassSurface(
            modifier = Modifier
                .size(48.dp)
                .clickable(onClick = item.onClick),
            shape = NotmidTheme.shapes.pill,
            backgroundColor = backgroundColor,
            contentPadding = PaddingValues(NotmidTheme.spacing.md),
        ) {
            item.icon(contentColor)
        }
        NotmidText(
            text = item.value ?: item.label,
            color = contentColor,
            variant = NotmidTextVariant.Caption,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
