package app.thdev.glassnavlab.feature.inbox.components.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.thdev.glassnavlab.core.designsystem.component.NotmidGlassSurface
import app.thdev.glassnavlab.core.designsystem.component.NotmidText
import app.thdev.glassnavlab.core.designsystem.component.NotmidTextVariant
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme
import app.thdev.glassnavlab.feature.inbox.R

@Composable
internal fun InboxEmptyState(
    inboxIsEmpty: Boolean,
    searching: Boolean,
) {
    val title = when {
        inboxIsEmpty -> R.string.inbox_empty_title
        searching -> R.string.inbox_search_empty_title
        else -> R.string.inbox_filter_empty_title
    }
    val description = when {
        inboxIsEmpty -> R.string.inbox_empty_description
        searching -> R.string.inbox_search_empty_description
        else -> R.string.inbox_filter_empty_description
    }
    NotmidGlassSurface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp),
        shape = NotmidTheme.shapes.sheet,
        backgroundColor = NotmidTheme.colors.glassLightStrong,
        contentPadding = PaddingValues(NotmidTheme.spacing.xl),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp),
            verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.sm, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            NotmidText(
                text = stringResource(title),
                modifier = Modifier.fillMaxWidth(),
                variant = NotmidTextVariant.Title,
                textAlign = TextAlign.Center,
            )
            NotmidText(
                text = stringResource(description),
                modifier = Modifier.fillMaxWidth(),
                variant = NotmidTextVariant.Body,
                color = NotmidTheme.colors.contentMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}
