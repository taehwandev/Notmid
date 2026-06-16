package app.thdev.glassnavlab.core.base.deeplink

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

@Composable
fun PendingDeepLinkEffect(
    deepLinkKey: Any?,
    uri: String?,
    onDeepLink: (String) -> Unit,
) {
    val currentOnDeepLink by rememberUpdatedState(onDeepLink)

    LaunchedEffect(deepLinkKey, uri) {
        uri
            ?.takeIf(String::isNotBlank)
            ?.let(currentOnDeepLink)
    }
}
