package app.thdev.glassnavlab.core.designsystem.component

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme

@Composable
fun NotmidTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = NotmidTheme.typography.body,
        label = label?.let {
            {
                NotmidText(
                    text = it,
                    variant = NotmidTextVariant.Label,
                    color = NotmidTheme.colors.contentMuted,
                )
            }
        },
        placeholder = placeholder?.let {
            {
                NotmidText(
                    text = it,
                    variant = NotmidTextVariant.Body,
                    color = NotmidTheme.colors.contentSubtle,
                )
            }
        },
        supportingText = supportingText?.let {
            {
                NotmidText(
                    text = it,
                    variant = NotmidTextVariant.Caption,
                    color = if (isError) NotmidTheme.colors.danger else NotmidTheme.colors.contentSubtle,
                )
            }
        },
        isError = isError,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = NotmidTheme.shapes.card,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = NotmidTheme.colors.content,
            unfocusedTextColor = NotmidTheme.colors.content,
            disabledTextColor = NotmidTheme.colors.contentSubtle,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = NotmidTheme.colors.surfaceRaised.copy(alpha = 0.64f),
            disabledContainerColor = NotmidTheme.colors.surfaceRaised.copy(alpha = 0.36f),
            cursorColor = NotmidTheme.colors.route,
            errorCursorColor = NotmidTheme.colors.danger,
            focusedBorderColor = NotmidTheme.colors.route,
            unfocusedBorderColor = NotmidTheme.colors.line,
            disabledBorderColor = NotmidTheme.colors.line.copy(alpha = 0.44f),
            errorBorderColor = NotmidTheme.colors.danger,
            focusedLabelColor = NotmidTheme.colors.route,
            unfocusedLabelColor = NotmidTheme.colors.contentMuted,
            errorLabelColor = NotmidTheme.colors.danger,
        ),
    )
}
