package app.thdev.glassnavlab.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.tooling.preview.Preview
import app.thdev.glassnavlab.core.designsystem.theme.notmidTheme
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthMode
import androidx.compose.ui.Modifier
import app.thdev.glassnavlab.core.designsystem.component.NotmidSectionHeader
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme

@Composable
internal fun ProfileSettingsContent(
    state: ProfileSettingsUiState,
    routeLabel: String,
    listState: LazyListState,
    isSaving: Boolean,
    statusMessage: String?,
    onAction: (ProfileSettingsAction) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = ProfileContentPadding,
        verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.lg),
    ) {
        item(key = "settings-header-profile") {
            NotmidSectionHeader(
                title = "Settings",
                subtitle = "Manage your account",
                eyebrow = "account",
            )
        }

        item(key = "settings-account-profile") {
            ProfileSettingsAccount(state, isSaving, statusMessage, onAction)
        }
    }
}

@Preview
@Composable
private fun SignedOutSettingsPreview() {
    notmidTheme {
        ProfileSettingsContent(
            ProfileSettingsUiState(NotmidAuthState(NotmidAuthMode.Disabled, null, emptyList()), "", ""),
            "profile > settings", rememberLazyListState(), false, null,
        ) {}
    }
}
