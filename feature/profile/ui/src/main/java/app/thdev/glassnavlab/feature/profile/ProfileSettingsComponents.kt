package app.thdev.glassnavlab.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.glassnavlab.core.designsystem.component.NotmidCard
import app.thdev.glassnavlab.core.designsystem.component.NotmidHorizontalDivider
import app.thdev.glassnavlab.core.designsystem.component.NotmidText
import app.thdev.glassnavlab.core.designsystem.component.NotmidTextVariant
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthMode
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthProvider

@Composable
internal fun SettingsSection(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    NotmidCard {
        Column(verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.md)) {
            Column(verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.xxs)) {
                NotmidText(
                    text = title,
                    variant = NotmidTextVariant.Headline,
                )
                NotmidText(
                    text = subtitle,
                    color = NotmidTheme.colors.contentMuted,
                    variant = NotmidTextVariant.BodySmall,
                )
            }
            NotmidHorizontalDivider()
            content()
        }
    }
}

@Composable
internal fun SettingValueRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.md),
    ) {
        NotmidText(
            text = label,
            modifier = Modifier.width(116.dp),
            color = NotmidTheme.colors.contentMuted,
            variant = NotmidTextVariant.Label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        NotmidText(
            text = value,
            modifier = Modifier.weight(1f),
            variant = NotmidTextVariant.BodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal val NotmidAuthMode.label: String
    get() = when (this) {
        NotmidAuthMode.Fake -> "fake mode"
        NotmidAuthMode.Firebase -> "firebase mode"
        NotmidAuthMode.Disabled -> "auth disabled"
    }

internal val NotmidAuthProvider.label: String
    get() = when (this) {
        NotmidAuthProvider.Fake -> "fake local"
        NotmidAuthProvider.Anonymous -> "anonymous"
        NotmidAuthProvider.Google -> "google"
    }
