package app.thdev.glassnavlab.core.designsystem.component

import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleStartEffect
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme
import app.thdev.glassnavlab.core.model.notmid.NotmidFeedbackPresentation
import app.thdev.glassnavlab.core.model.notmid.NotmidFeedbackTone
import app.thdev.glassnavlab.core.model.notmid.NotmidUiEffect
import app.thdev.glassnavlab.core.model.notmid.NotmidUiFeedback
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Composable
fun NotmidFeedbackEffectHandler(
    effects: Flow<NotmidUiEffect>,
    modifier: Modifier = Modifier,
    onActionDeepLink: (String) -> Unit = {},
) {
    val context = LocalContext.current
    var activeAlertFeedback by remember { mutableStateOf<NotmidUiFeedback?>(null) }

    NotmidUiEffectLifecycleCollector(
        effects = effects,
        onEffect = { effect ->
            when (effect) {
                is NotmidUiEffect.ShowFeedback -> {
                    val feedback = effect.feedback
                    when (feedback.presentation) {
                        NotmidFeedbackPresentation.Toast -> {
                            Toast.makeText(context, feedback.message, Toast.LENGTH_SHORT).show()
                        }

                        NotmidFeedbackPresentation.Alert -> {
                            activeAlertFeedback = feedback
                        }

                        NotmidFeedbackPresentation.Inline,
                        NotmidFeedbackPresentation.FullPage,
                        -> Unit
                    }
                }

                is NotmidUiEffect.NavigateDeepLink -> {
                    effect.deepLink
                        .takeIf(String::isNotBlank)
                        ?.let(onActionDeepLink)
                }
            }
        },
    )

    activeAlertFeedback?.let { alertFeedback ->
        NotmidFeedbackAlertDialog(
            feedback = alertFeedback,
            onConfirm = {
                alertFeedback.action?.deepLink
                    ?.takeIf(String::isNotBlank)
                    ?.let(onActionDeepLink)
                activeAlertFeedback = null
            },
            onDismiss = {
                activeAlertFeedback = null
            },
            modifier = modifier,
        )
    }
}

/**
 * Collects one-shot UI effects only while the current lifecycle is STARTED.
 *
 * Flow collection needs paired start/stop cleanup, so this uses
 * LifecycleStartEffect instead of LifecycleEventEffect.
 */
@Composable
fun NotmidUiEffectLifecycleCollector(
    effects: Flow<NotmidUiEffect>,
    onEffect: (NotmidUiEffect) -> Unit,
) {
    val collectionScope = rememberCoroutineScope()
    val currentOnEffect by rememberUpdatedState(onEffect)

    LifecycleStartEffect(effects) {
        var collectionJob: Job? = collectionScope.launch {
            effects.collect { effect ->
                currentOnEffect(effect)
            }
        }

        onStopOrDispose {
            collectionJob?.cancel()
            collectionJob = null
        }
    }
}

@Composable
private fun NotmidFeedbackAlertDialog(
    feedback: NotmidUiFeedback,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Text(text = feedback.tone.dialogTitle())
        },
        text = {
            Text(text = feedback.message)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = feedback.action?.label ?: "OK")
            }
        },
        dismissButton = if (feedback.action != null) {
            {
                TextButton(onClick = onDismiss) {
                    Text(text = "Cancel")
                }
            }
        } else {
            null
        },
    )
}

private fun NotmidFeedbackTone.dialogTitle(): String {
    return when (this) {
        NotmidFeedbackTone.Info -> "Notice"
        NotmidFeedbackTone.Success -> "Done"
        NotmidFeedbackTone.Warning -> "Action needed"
        NotmidFeedbackTone.Error -> "Something went wrong"
    }
}

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
