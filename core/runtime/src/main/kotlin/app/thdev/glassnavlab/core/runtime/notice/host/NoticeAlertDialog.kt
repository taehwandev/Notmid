package app.thdev.glassnavlab.core.runtime.notice.host

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme
import app.thdev.glassnavlab.core.notice.api.model.NoticeRequest
import app.thdev.glassnavlab.core.notice.api.model.NoticeTone

@Composable
internal fun NoticeAlertDialog(
    notice: NoticeRequest,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        shape = NotmidTheme.shapes.card,
        containerColor = NotmidTheme.colors.surfaceRaised,
        titleContentColor = NotmidTheme.colors.content,
        textContentColor = NotmidTheme.colors.contentMuted,
        title = {
            Text(text = notice.tone.dialogTitle())
        },
        text = {
            Text(text = notice.message)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = notice.action?.label ?: "OK")
            }
        },
        dismissButton = if (notice.action != null) {
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

private fun NoticeTone.dialogTitle(): String {
    return when (this) {
        NoticeTone.Info -> "Notice"
        NoticeTone.Success -> "Done"
        NoticeTone.Warning -> "Action needed"
        NoticeTone.Error -> "Something went wrong"
    }
}
