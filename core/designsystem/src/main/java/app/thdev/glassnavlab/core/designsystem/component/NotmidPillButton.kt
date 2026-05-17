package app.thdev.glassnavlab.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme

@Composable
fun NotmidPillButton(
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    leadingIcon: (@Composable (contentColor: Color) -> Unit)? = null,
    onClick: () -> Unit,
) {
    val contentColor = when {
        !enabled -> NotmidTheme.colors.contentSubtle.copy(alpha = 0.42f)
        selected -> NotmidTheme.colors.contentOnMedia
        else -> NotmidTheme.colors.content
    }
    val backgroundColor = when {
        !enabled -> NotmidTheme.colors.glassLight.copy(alpha = 0.36f)
        selected -> NotmidTheme.colors.surfaceInverse
        else -> NotmidTheme.colors.glassLight
    }

    NotmidGlassSurface(
        modifier = modifier.then(
            if (enabled) Modifier.clickable(onClick = onClick) else Modifier,
        ),
        shape = NotmidTheme.shapes.pill,
        backgroundColor = backgroundColor,
        borderColor = NotmidTheme.colors.glassStroke,
        contentPadding = PaddingValues(
            horizontal = NotmidTheme.spacing.md,
            vertical = NotmidTheme.spacing.sm,
        ),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leadingIcon?.invoke(contentColor)
            NotmidText(
                text = label,
                color = contentColor,
                variant = NotmidTextVariant.Label,
                maxLines = 1,
            )
        }
    }
}
