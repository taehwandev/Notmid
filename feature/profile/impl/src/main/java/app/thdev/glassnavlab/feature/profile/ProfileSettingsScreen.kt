package app.thdev.glassnavlab.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.glassnavlab.core.designsystem.component.NotmidButton
import app.thdev.glassnavlab.core.designsystem.component.NotmidCard
import app.thdev.glassnavlab.core.designsystem.component.NotmidHorizontalDivider
import app.thdev.glassnavlab.core.designsystem.component.NotmidSectionHeader
import app.thdev.glassnavlab.core.designsystem.component.NotmidSelectionRow
import app.thdev.glassnavlab.core.designsystem.component.NotmidSwitch
import app.thdev.glassnavlab.core.designsystem.component.NotmidText
import app.thdev.glassnavlab.core.designsystem.component.NotmidTextField
import app.thdev.glassnavlab.core.designsystem.component.NotmidTextVariant
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthMode
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthProvider
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState
import app.thdev.glassnavlab.feature.notmid.api.NotmidRoute
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidDestination

@Composable
fun ProfileSettingsScreen(
    parentDestination: NotmidDestination,
    authState: NotmidAuthState,
    navigationStack: List<NotmidRoute>,
    listState: LazyListState,
    isSaving: Boolean = false,
    statusMessage: String? = null,
    onSaveProfileSettings: (displayName: String, homeNeighborhood: String) -> Unit = { _, _ -> },
) {
    val routeLabel = remember(navigationStack) {
        navigationStack.joinToString(" > ") { it.webPathSegments.last() }
    }
    val currentUser = authState.session?.user
    var displayName by rememberSaveable(currentUser?.id) {
        mutableStateOf(currentUser?.displayName.orEmpty())
    }
    var homeNeighborhood by rememberSaveable(currentUser?.id) {
        mutableStateOf(currentUser?.homeNeighborhood.orEmpty())
    }
    val canSaveProfile = displayName.isNotBlank() && homeNeighborhood.isNotBlank() && !isSaving

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = ProfileContentPadding,
        verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.lg),
    ) {
        item(key = "settings-header-${parentDestination.id}") {
            NotmidSectionHeader(
                title = "Settings",
                subtitle = routeLabel,
                eyebrow = authState.mode.label,
            )
        }

        item(key = "settings-account-${parentDestination.id}") {
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
                    onValueChange = { displayName = it },
                    label = "Display name",
                    placeholder = "Name shown on receipts",
                    supportingText = "${displayName.length}/80",
                    enabled = currentUser != null && !isSaving,
                )
                NotmidTextField(
                    value = homeNeighborhood,
                    onValueChange = { homeNeighborhood = it },
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
                    onClick = { onSaveProfileSettings(displayName, homeNeighborhood) },
                    enabled = canSaveProfile,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item(key = "settings-privacy-${parentDestination.id}") {
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

        item(key = "settings-auth-${parentDestination.id}") {
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

        item(key = "settings-open-source-${parentDestination.id}") {
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

@Composable
internal fun SettingsSection(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    NotmidCard {
        Column(verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.md)) {
            Column(verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.xxs)) {
                NotmidText(
                    text = title,
                    variant = NotmidTextVariant.Headline,
                )
                NotmidText(
                    text = subtitle,
                    color = NotmidTheme.colors.contentMuted,
                    variant = NotmidTextVariant.BodySmall,
                )
            }
            NotmidHorizontalDivider()
            content()
        }
    }
}

@Composable
internal fun SettingValueRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.md),
    ) {
        NotmidText(
            text = label,
            modifier = Modifier.width(116.dp),
            color = NotmidTheme.colors.contentMuted,
            variant = NotmidTextVariant.Label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        NotmidText(
            text = value,
            modifier = Modifier.weight(1f),
            variant = NotmidTextVariant.BodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal val NotmidAuthMode.label: String
    get() = when (this) {
        NotmidAuthMode.Fake -> "fake mode"
        NotmidAuthMode.Firebase -> "firebase mode"
        NotmidAuthMode.Disabled -> "auth disabled"
    }

internal val NotmidAuthProvider.label: String
    get() = when (this) {
        NotmidAuthProvider.Fake -> "fake local"
        NotmidAuthProvider.Anonymous -> "anonymous"
        NotmidAuthProvider.Google -> "google"
    }
