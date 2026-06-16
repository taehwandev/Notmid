package app.thdev.glassnavlab.coreapp.feedback.host

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.thdev.glassnavlab.core.designsystem.component.NotmidSnackbarHost
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme
import app.thdev.glassnavlab.core.feedback.api.effect.FeedbackEffect
import app.thdev.glassnavlab.core.feedback.api.model.FeedbackPresentation
import app.thdev.glassnavlab.core.feedback.api.model.FeedbackRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Composable
fun FeedbackHost(
    effects: Flow<FeedbackEffect>,
    modifier: Modifier = Modifier,
    onActionDeepLink: (String) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val currentOnActionDeepLink by rememberUpdatedState(onActionDeepLink)
    var activeAlertFeedback by remember { mutableStateOf<FeedbackRequest?>(null) }

    FeedbackEffectLifecycleCollector(
        effects = effects,
        onEffect = { effect ->
            when (effect) {
                is FeedbackEffect.ShowFeedback -> {
                    val feedback = effect.feedback
                    when (feedback.presentation) {
                        FeedbackPresentation.Toast -> {
                            Toast.makeText(
                                context,
                                feedback.message,
                                Toast.LENGTH_SHORT,
                            ).show()
                        }

                        FeedbackPresentation.Snackbar -> {
                            snackbarScope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = feedback.message,
                                    actionLabel = feedback.action?.label,
                                    withDismissAction = feedback.action != null,
                                    duration = feedback.snackbarDuration(),
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    feedback.action?.perform(currentOnActionDeepLink)
                                }
                            }
                        }

                        FeedbackPresentation.Alert -> {
                            activeAlertFeedback = feedback
                        }

                        FeedbackPresentation.Inline,
                        FeedbackPresentation.FullPage,
                        -> Unit
                    }
                }

                is FeedbackEffect.NavigateDeepLink -> {
                    effect.deepLink
                        .takeIf(String::isNotBlank)
                        ?.let(currentOnActionDeepLink)
                }
            }
        },
    )

    Box(modifier = modifier) {
        content()
        NotmidSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(
                    horizontal = NotmidTheme.spacing.lg,
                    vertical = NotmidTheme.spacing.xxl,
                ),
        )
    }

    activeAlertFeedback?.let { alertFeedback ->
        FeedbackAlertDialog(
            feedback = alertFeedback,
            onConfirm = {
                alertFeedback.action?.perform(currentOnActionDeepLink)
                activeAlertFeedback = null
            },
            onDismiss = {
                activeAlertFeedback = null
            },
        )
    }
}

private fun FeedbackRequest.snackbarDuration(): SnackbarDuration {
    return if (action == null) {
        SnackbarDuration.Short
    } else {
        SnackbarDuration.Long
    }
}
