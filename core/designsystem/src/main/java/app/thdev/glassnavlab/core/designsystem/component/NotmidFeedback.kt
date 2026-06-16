package app.thdev.glassnavlab.core.designsystem.component

import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme

@Composable
fun NotmidSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
    ) { snackbarData ->
        Snackbar(
            snackbarData = snackbarData,
            shape = NotmidTheme.shapes.card,
            containerColor = NotmidTheme.colors.surfaceInverse,
            contentColor = NotmidTheme.colors.contentOnMedia,
            actionColor = NotmidTheme.colors.signal,
            dismissActionContentColor = NotmidTheme.colors.contentOnMedia.copy(alpha = 0.78f),
        )
    }
}

@Composable
fun NotmidFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = NotmidTheme.colors.surfaceInverse,
    contentColor: Color = NotmidTheme.colors.contentOnMedia,
    content: @Composable () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = NotmidTheme.shapes.pill,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = NotmidTheme.elevation.floating,
            pressedElevation = NotmidTheme.elevation.sheet,
            focusedElevation = NotmidTheme.elevation.floating,
            hoveredElevation = NotmidTheme.elevation.floating,
        ),
        content = content,
    )
}
