package app.thdev.glassnavlab.feature.profile.components.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.thdev.glassnavlab.core.designsystem.component.NotmidGlassSurface
import app.thdev.glassnavlab.core.designsystem.component.NotmidText
import app.thdev.glassnavlab.core.designsystem.component.NotmidTextVariant
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme

@Composable
internal fun ProfileEmptyState(
    title: String,
    description: String,
) {
    NotmidGlassSurface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp),
        shape = NotmidTheme.shapes.sheet,
        backgroundColor = NotmidTheme.colors.glassLightStrong,
        contentPadding = PaddingValues(NotmidTheme.spacing.xl),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(min = 156.dp),
            verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.sm, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            NotmidText(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                variant = NotmidTextVariant.Title,
                textAlign = TextAlign.Center,
            )
            NotmidText(
                text = description,
                modifier = Modifier.fillMaxWidth(),
                variant = NotmidTextVariant.Body,
                color = NotmidTheme.colors.contentMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}
