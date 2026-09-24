package app.thdev.glassnavlab.feature.profile

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.thdev.glassnavlab.core.designsystem.component.NotmidButton
import app.thdev.glassnavlab.core.designsystem.component.NotmidText
import app.thdev.glassnavlab.core.designsystem.component.NotmidTextField
import app.thdev.glassnavlab.core.designsystem.component.NotmidTextVariant
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme

@Composable
internal fun ProfileSettingsAccount(
    state: ProfileSettingsUiState,
    isSaving: Boolean,
    statusMessage: String?,
    onAction: (ProfileSettingsAction) -> Unit,
) {
    val authState = state.auth
    val currentUser = authState.session?.user
    val displayName = state.displayName
    val homeNeighborhood = state.homeNeighborhood
    val canSaveProfile = state.canSave && !isSaving
    SettingsSection(
        title = "Account",
        subtitle = "Current fake user contract that future Firebase Auth will replace.",
    ) {
        SettingValueRow(
            label = "Handle",
            value = authState.session?.user?.handle ?: "signed out",
        )
        NotmidTextField(
            value = displayName,
            onValueChange = { onAction(ProfileSettingsAction.DisplayNameChanged(it)) },
            label = "Display name",
            placeholder = "Name shown on receipts",
            supportingText = "${displayName.length}/80",
            enabled = currentUser != null && !isSaving,
        )
        NotmidTextField(
            value = homeNeighborhood,
            onValueChange = { onAction(ProfileSettingsAction.NeighborhoodChanged(it)) },
            label = "Neighborhood",
            placeholder = "Home neighborhood",
            supportingText = "${homeNeighborhood.length}/80",
            enabled = currentUser != null && !isSaving,
        )
        statusMessage?.let { message ->
            NotmidText(
                text = message,
                variant = NotmidTextVariant.Caption,
                color = NotmidTheme.colors.contentMuted,
            )
        }
        NotmidButton(
            text = if (isSaving) "Saving" else "Save profile",
            onClick = { onAction(ProfileSettingsAction.Save) },
            enabled = canSaveProfile,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
