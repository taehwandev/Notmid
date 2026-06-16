package app.thdev.glassnavlab.coreapp.feedback.host

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme
import app.thdev.glassnavlab.core.feedback.api.model.FeedbackRequest
import app.thdev.glassnavlab.core.feedback.api.model.FeedbackTone

@Composable
internal fun FeedbackAlertDialog(
    feedback: FeedbackRequest,
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

private fun FeedbackTone.dialogTitle(): String {
    return when (this) {
        FeedbackTone.Info -> "Notice"
        FeedbackTone.Success -> "Done"
        FeedbackTone.Warning -> "Action needed"
        FeedbackTone.Error -> "Something went wrong"
    }
}
