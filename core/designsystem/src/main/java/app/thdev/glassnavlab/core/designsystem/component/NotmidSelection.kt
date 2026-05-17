package app.thdev.glassnavlab.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme

@Composable
fun NotmidCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = CheckboxDefaults.colors(
            checkedColor = NotmidTheme.colors.surfaceInverse,
            uncheckedColor = NotmidTheme.colors.contentMuted,
            checkmarkColor = NotmidTheme.colors.contentOnMedia,
            disabledCheckedColor = NotmidTheme.colors.contentSubtle.copy(alpha = 0.32f),
            disabledUncheckedColor = NotmidTheme.colors.contentSubtle.copy(alpha = 0.24f),
        ),
    )
}

@Composable
fun NotmidSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = NotmidTheme.colors.contentOnMedia,
            checkedTrackColor = NotmidTheme.colors.surfaceInverse,
            uncheckedThumbColor = NotmidTheme.colors.surfaceRaised,
            uncheckedTrackColor = NotmidTheme.colors.contentSubtle.copy(alpha = 0.34f),
            uncheckedBorderColor = NotmidTheme.colors.line,
        ),
    )
}

@Composable
fun NotmidSelectionRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.padding(vertical = NotmidTheme.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.xxs),
        ) {
            NotmidText(
                text = title,
                variant = NotmidTextVariant.Label,
            )
            if (subtitle != null) {
                NotmidText(
                    text = subtitle,
                    variant = NotmidTextVariant.Caption,
                    color = NotmidTheme.colors.contentMuted,
                )
            }
        }
        trailing()
    }
}
