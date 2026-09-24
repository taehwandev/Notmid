package app.thdev.glassnavlab.feature.profile

import androidx.compose.runtime.Immutable
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState

@Immutable
internal data class ProfileSettingsUiState(
    val auth: NotmidAuthState,
    val displayName: String,
    val homeNeighborhood: String,
    val isSaving: Boolean = false,
    val statusMessage: String? = null,
) {
    val canSave: Boolean get() = !isSaving && auth.isAuthenticated && displayName.isNotBlank() && homeNeighborhood.isNotBlank()
}
