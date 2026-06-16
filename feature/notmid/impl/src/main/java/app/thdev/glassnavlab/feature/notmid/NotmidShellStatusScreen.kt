package app.thdev.glassnavlab.feature.notmid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.thdev.glassnavlab.core.designsystem.component.NotmidButton
import app.thdev.glassnavlab.core.designsystem.component.NotmidGlassSurface
import app.thdev.glassnavlab.core.designsystem.component.NotmidText
import app.thdev.glassnavlab.core.designsystem.component.NotmidTextVariant
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidBackgroundColor

@Composable
fun NotmidShellLoadingScreen(
    sourceLabel: String,
    modifier: Modifier = Modifier,
) {
    NotmidShellStatusScreen(
        title = "Loading notmid",
        message = "$sourceLabel is syncing.",
        modifier = modifier,
        action = {
            CircularProgressIndicator(
                color = NotmidTheme.colors.route,
                strokeWidth = 3.dp,
            )
        },
    )
}

@Composable
fun NotmidShellErrorScreen(
    title: String,
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NotmidShellStatusScreen(
        title = title,
        message = message,
        modifier = modifier,
        action = {
            NotmidButton(
                text = "Retry",
                onClick = onRetry,
            )
        },
    )
}

@Composable
private fun NotmidShellStatusScreen(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NotmidBackgroundColor)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        NotmidGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 380.dp),
            contentPadding = PaddingValues(24.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                NotmidText(
                    text = title,
                    variant = NotmidTextVariant.Title,
                    textAlign = TextAlign.Center,
                )
                NotmidText(
                    text = message,
                    variant = NotmidTextVariant.BodySmall,
                    color = NotmidTheme.colors.contentMuted,
                    textAlign = TextAlign.Center,
                )
                action()
            }
        }
    }
}
