package app.thdev.glassnavlab.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.glassnavlab.core.designsystem.component.NotmidButton
import app.thdev.glassnavlab.core.designsystem.component.NotmidButtonVariant
import app.thdev.glassnavlab.core.designsystem.component.NotmidCard
import app.thdev.glassnavlab.core.designsystem.component.NotmidGlassSurface
import app.thdev.glassnavlab.core.designsystem.component.NotmidHorizontalDivider
import app.thdev.glassnavlab.core.designsystem.component.NotmidOutlinedButton
import app.thdev.glassnavlab.core.designsystem.component.NotmidPillButton
import app.thdev.glassnavlab.core.designsystem.theme.NotmidColorTokens
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme
import app.thdev.glassnavlab.core.designsystem.component.NotmidText
import app.thdev.glassnavlab.core.designsystem.component.NotmidTextVariant
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthUser
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidDestination

@Composable
internal fun ProfileHeader(
    user: NotmidAuthUser?,
    destination: NotmidDestination,
    authState: NotmidAuthState,
    onSettingsRequested: () -> Unit,
) {
    val displayName = user?.displayName ?: "Local browser"
    val handle = user?.handle ?: "signed.out"
    val roles = user?.roles ?: listOf("browse")

    NotmidGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = NotmidTheme.shapes.sheet,
        backgroundColor = NotmidTheme.colors.glassLight.copy(alpha = 0.82f),
        contentPadding = PaddingValues(NotmidTheme.spacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.md),
            ) {
                ProfileAvatar(handle = handle)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.xs),
                ) {
                    NotmidText(
                        text = "@$handle",
                        color = NotmidTheme.colors.signal,
                        variant = NotmidTextVariant.Label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    NotmidText(
                        text = displayName,
                        variant = NotmidTextVariant.Title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    NotmidText(
                        text = user?.homeNeighborhood ?: destination.subtitle,
                        color = NotmidTheme.colors.contentMuted,
                        variant = NotmidTextVariant.BodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.sm),
            ) {
                roles.forEach { role ->
                    NotmidPillButton(
                        label = role,
                        selected = role == "creator",
                        onClick = {},
                    )
                }
                NotmidPillButton(
                    label = authState.mode.label,
                    selected = false,
                    onClick = {},
                )
            }

            ProfileMetricRow(
                receiptCount = destination.clips.size,
                savedCount = destination.places.size,
                routeCount = 2,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.sm)) {
                NotmidButton(
                    text = "Settings",
                    onClick = onSettingsRequested,
                    variant = NotmidButtonVariant.Primary,
                )
                NotmidOutlinedButton(
                    text = "Share profile",
                    onClick = {},
                )
            }
        }
    }
}

@Composable
private fun ProfileAvatar(handle: String) {
    Box(
        modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        NotmidColorTokens.WarmClip,
                        NotmidColorTokens.NightViolet,
                        NotmidColorTokens.RouteBlue,
                    ),
                ),
            )
            .padding(2.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.24f)),
        )
        NotmidText(
            text = handle.take(1).uppercase(),
            modifier = Modifier
                .padding(top = 20.dp)
                .fillMaxWidth(),
            color = NotmidColorTokens.Cloud,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            variant = NotmidTextVariant.Title,
        )
    }
}

@Composable
private fun ProfileMetricRow(
    receiptCount: Int,
    savedCount: Int,
    routeCount: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.sm),
    ) {
        ProfileMetric(label = "Receipts", value = receiptCount.toString(), modifier = Modifier.weight(1f))
        ProfileMetric(label = "Saved", value = savedCount.toString(), modifier = Modifier.weight(1f))
        ProfileMetric(label = "Routes", value = routeCount.toString(), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ProfileMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .heightIn(min = 64.dp)
            .clip(NotmidTheme.shapes.card)
            .background(NotmidTheme.colors.surfaceRaised.copy(alpha = 0.58f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.xxs),
    ) {
        NotmidText(
            text = label,
            color = NotmidTheme.colors.contentSubtle,
            variant = NotmidTextVariant.Caption,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        NotmidText(
            text = value,
            variant = NotmidTextVariant.Headline,
            maxLines = 1,
        )
    }
}

@Composable
internal fun ProfileTabs() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.sm),
    ) {
        listOf("Clips", "Saved", "Places", "Routes").forEachIndexed { index, label ->
            NotmidPillButton(
                label = label,
                selected = index == 0,
                onClick = {},
            )
        }
    }
}

@Composable
internal fun ProfileSettingsEntry(
    authState: NotmidAuthState,
    onSettingsRequested: () -> Unit,
) {
    NotmidCard {
        Column(verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.md)) {
            NotmidText(
                text = "Account controls",
                variant = NotmidTextVariant.Headline,
            )
            NotmidText(
                text = "Privacy, auth provider state, and open-source config checks live under Profile Settings.",
                color = NotmidTheme.colors.contentMuted,
                variant = NotmidTextVariant.BodySmall,
            )
            NotmidHorizontalDivider()
            SettingValueRow(label = "Mode", value = authState.mode.label)
            SettingValueRow(
                label = "Session",
                value = if (authState.isAuthenticated) "local fake signed in" else "signed out",
            )
            NotmidButton(
                text = "Open settings",
                onClick = onSettingsRequested,
                variant = NotmidButtonVariant.Secondary,
            )
        }
    }
}
