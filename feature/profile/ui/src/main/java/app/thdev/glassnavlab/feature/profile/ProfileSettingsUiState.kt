package app.thdev.glassnavlab.feature.profile

import androidx.compose.runtime.Immutable
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState

@Immutable
internal data class ProfileSettingsUiState(
    val auth: NotmidAuthState,
    val displayName: String,
    val homeNeighborhood: String,
) {
    val canSave: Boolean get() = auth.isAuthenticated && displayName.isNotBlank() && homeNeighborhood.isNotBlank()
}
