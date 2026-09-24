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
import app.thdev.glassnavlab.core.designsystem.component.NotmidSelectionRow
import app.thdev.glassnavlab.core.designsystem.component.NotmidSwitch
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthProvider

@Composable
internal fun ProfileSettingsContent(
    state: ProfileSettingsUiState,
    routeLabel: String,
    listState: LazyListState,
    isSaving: Boolean,
    statusMessage: String?,
    onAction: (ProfileSettingsAction) -> Unit,
) {
    val authState = state.auth
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = ProfileContentPadding,
        verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.lg),
    ) {
        item(key = "settings-header-profile") {
            NotmidSectionHeader(
                title = "Settings",
                subtitle = routeLabel,
                eyebrow = authState.mode.label,
            )
        }

        item(key = "settings-account-profile") {
            ProfileSettingsAccount(state, isSaving, statusMessage, onAction)
        }

        item(key = "settings-privacy-profile") {
            SettingsSection(
                title = "Privacy",
                subtitle = "Visible controls now, server policy later.",
            ) {
                NotmidSelectionRow(
                    title = "Saved places",
                    subtitle = "Keep saves private until sharing is explicit.",
                ) {
                    NotmidSwitch(checked = false, onCheckedChange = null)
                }
                NotmidSelectionRow(
                    title = "Chat invites",
                    subtitle = "Allow friends from shared clips and places.",
                ) {
                    NotmidSwitch(checked = true, onCheckedChange = null)
                }
                NotmidSelectionRow(
                    title = "Receipt visibility",
                    subtitle = "Public by default for local fake mode.",
                ) {
                    NotmidSwitch(checked = true, onCheckedChange = null)
                }
            }
        }

        item(key = "settings-auth-profile") {
            SettingsSection(
                title = "Auth mode",
                subtitle = "Features consume auth state, not Firebase SDK details.",
            ) {
                SettingValueRow(label = "Mode", value = authState.mode.label)
                SettingValueRow(
                    label = "Provider",
                    value = authState.session?.provider?.label ?: NotmidAuthProvider.Fake.label,
                )
                SettingValueRow(
                    label = "Protected actions",
                    value = authState.requiredActions.joinToString { it.name },
                )
            }
        }

        item(key = "settings-open-source-profile") {
            SettingsSection(
                title = "Open-source safety",
                subtitle = "Production config belongs in ignored local files or secret stores.",
            ) {
                SettingValueRow(label = "Firebase config", value = "bring your own project")
                SettingValueRow(label = "API base URL", value = "local.properties / env")
                SettingValueRow(label = "Secrets", value = "not committed")
            }
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
