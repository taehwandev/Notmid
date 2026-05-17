package app.thdev.glassnavlab.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme

enum class NotmidButtonVariant {
    Primary,
    Secondary,
    Danger,
}

@Composable
fun NotmidButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: NotmidButtonVariant = NotmidButtonVariant.Primary,
    leadingIcon: (@Composable (contentColor: Color) -> Unit)? = null,
    trailingIcon: (@Composable (contentColor: Color) -> Unit)? = null,
) {
    NotmidButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        variant = variant,
    ) {
        NotmidButtonContent(
            text = text,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
        )
    }
}

@Composable
fun NotmidButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: NotmidButtonVariant = NotmidButtonVariant.Primary,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = NotmidTheme.shapes.pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = variant.containerColor(),
            contentColor = variant.contentColor(),
            disabledContainerColor = NotmidTheme.colors.surfaceRaised.copy(alpha = 0.48f),
            disabledContentColor = NotmidTheme.colors.contentSubtle.copy(alpha = 0.48f),
        ),
        contentPadding = contentPadding,
        content = content,
    )
}

@Composable
fun NotmidOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable (contentColor: Color) -> Unit)? = null,
    trailingIcon: (@Composable (contentColor: Color) -> Unit)? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = NotmidTheme.shapes.pill,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = NotmidTheme.colors.content,
            disabledContentColor = NotmidTheme.colors.contentSubtle.copy(alpha = 0.48f),
        ),
        border = BorderStroke(
            width = NotmidOutlinedButtonBorderWidth,
            color = if (enabled) {
                NotmidTheme.colors.line
            } else {
                NotmidTheme.colors.line.copy(alpha = 0.42f)
            },
        ),
    ) {
        NotmidButtonContent(
            text = text,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
        )
    }
}

private val NotmidOutlinedButtonBorderWidth = 1.dp

@Composable
fun NotmidTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable (contentColor: Color) -> Unit)? = null,
    trailingIcon: (@Composable (contentColor: Color) -> Unit)? = null,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = NotmidTheme.shapes.pill,
        colors = ButtonDefaults.textButtonColors(
            contentColor = NotmidTheme.colors.content,
            disabledContentColor = NotmidTheme.colors.contentSubtle.copy(alpha = 0.48f),
        ),
    ) {
        NotmidButtonContent(
            text = text,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
        )
    }
}

@Composable
fun NotmidIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    content: @Composable (contentColor: Color) -> Unit,
) {
    val contentColor = if (selected) {
        NotmidTheme.colors.contentOnMedia
    } else {
        NotmidTheme.colors.content
    }
    IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (selected) NotmidTheme.colors.surfaceInverse else Color.Transparent,
            contentColor = contentColor,
            disabledContentColor = NotmidTheme.colors.contentSubtle.copy(alpha = 0.48f),
        ),
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content(contentColor)
        }
    }
}

@Composable
private fun NotmidButtonContent(
    text: String,
    leadingIcon: (@Composable (contentColor: Color) -> Unit)?,
    trailingIcon: (@Composable (contentColor: Color) -> Unit)?,
) {
    val contentColor = LocalContentColor.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leadingIcon?.invoke(contentColor)
        NotmidText(
            text = text,
            color = contentColor,
            variant = NotmidTextVariant.Label,
            maxLines = 1,
        )
        trailingIcon?.invoke(contentColor)
    }
}

@Composable
private fun NotmidButtonVariant.containerColor(): Color {
    return when (this) {
        NotmidButtonVariant.Primary -> NotmidTheme.colors.surfaceInverse
        NotmidButtonVariant.Secondary -> NotmidTheme.colors.route
        NotmidButtonVariant.Danger -> NotmidTheme.colors.danger
    }
}

@Composable
private fun NotmidButtonVariant.contentColor(): Color {
    return when (this) {
        NotmidButtonVariant.Primary -> NotmidTheme.colors.contentOnMedia
        NotmidButtonVariant.Secondary -> NotmidTheme.colors.contentOnMedia
        NotmidButtonVariant.Danger -> NotmidTheme.colors.contentOnMedia
    }
}
