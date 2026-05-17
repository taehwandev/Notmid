package app.thdev.glassnavlab.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme

@Composable
fun NotmidSectionHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.sm),
    ) {
        if (eyebrow != null) {
            NotmidText(
                text = eyebrow,
                color = NotmidTheme.colors.contentMuted,
                variant = NotmidTextVariant.Label,
            )
        }
        NotmidText(
            text = title,
            color = NotmidTheme.colors.content,
            variant = NotmidTextVariant.Display,
        )
        NotmidText(
            text = subtitle,
            color = NotmidTheme.colors.contentMuted,
            variant = NotmidTextVariant.Body,
        )
    }
}
