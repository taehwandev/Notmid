package app.thdev.glassnavlab.core.runtime.notice.host

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
import app.thdev.glassnavlab.core.notice.api.effect.NoticeEffect
import app.thdev.glassnavlab.core.notice.api.model.NoticePresentation
import app.thdev.glassnavlab.core.notice.api.model.NoticeRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Composable
fun NoticeHost(
    effects: Flow<NoticeEffect>,
    modifier: Modifier = Modifier,
    onActionDeepLink: (String) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val currentOnActionDeepLink by rememberUpdatedState(onActionDeepLink)
    var activeAlertNotice by remember { mutableStateOf<NoticeRequest?>(null) }

    NoticeEffectLifecycleCollector(
        effects = effects,
        onEffect = { effect ->
            when (effect) {
                is NoticeEffect.ShowNotice -> {
                    val notice = effect.notice
                    when (notice.presentation) {
                        NoticePresentation.Toast -> {
                            Toast.makeText(
                                context,
                                notice.message,
                                Toast.LENGTH_SHORT,
                            ).show()
                        }

                        NoticePresentation.Snackbar -> {
                            snackbarScope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = notice.message,
                                    actionLabel = notice.action?.label,
                                    withDismissAction = notice.action != null,
                                    duration = notice.snackbarDuration(),
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    notice.action?.perform(currentOnActionDeepLink)
                                }
                            }
                        }

                        NoticePresentation.Alert -> {
                            activeAlertNotice = notice
                        }

                        NoticePresentation.Inline,
                        NoticePresentation.FullPage,
                        -> Unit
                    }
                }

                is NoticeEffect.NavigateDeepLink -> {
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

    activeAlertNotice?.let { alertNotice ->
        NoticeAlertDialog(
            notice = alertNotice,
            onConfirm = {
                alertNotice.action?.perform(currentOnActionDeepLink)
                activeAlertNotice = null
            },
            onDismiss = {
                activeAlertNotice = null
            },
        )
    }
}

private fun NoticeRequest.snackbarDuration(): SnackbarDuration {
    return if (action == null) {
        SnackbarDuration.Short
    } else {
        SnackbarDuration.Long
    }
}
