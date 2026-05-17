package app.thdev.glassnavlab

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.thdev.glassnavlab.core.data.notmid.StaticNotmidAuthRepository
import app.thdev.glassnavlab.core.data.notmid.StaticNotmidContentRepository
import app.thdev.glassnavlab.core.designsystem.theme.notmidTheme
import app.thdev.glassnavlab.core.domain.notmid.GetNotmidAuthStateUseCase
import app.thdev.glassnavlab.core.domain.notmid.GetNotmidDestinationsUseCase
import app.thdev.glassnavlab.core.domain.notmid.SignInToNotmidUseCase
import app.thdev.glassnavlab.feature.notmid.api.NotmidDestinationIds
import app.thdev.glassnavlab.feature.notmid.NotmidShellScreen
import app.thdev.glassnavlab.feature.notmid.api.NotmidRouteEvent
import app.thdev.glassnavlab.navigation.AppActivityRouteLauncher
import app.thdev.glassnavlab.navigation.AppDeepLinkResolver
import app.thdev.glassnavlab.navigation.rememberAppRouter

class MainActivity : ComponentActivity() {
    private var pendingDeepLink by mutableStateOf<PendingDeepLink?>(null)
    private var pendingDeepLinkId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updatePendingDeepLink(intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            val notmidContentRepository = remember { StaticNotmidContentRepository() }
            val notmidAuthRepository = remember { StaticNotmidAuthRepository() }
            val notmidDestinations = remember(notmidContentRepository) {
                GetNotmidDestinationsUseCase(notmidContentRepository)()
            }
            val getNotmidAuthState = remember(notmidAuthRepository) {
                GetNotmidAuthStateUseCase(notmidAuthRepository)
            }
            val signInToNotmid = remember(notmidAuthRepository) {
                SignInToNotmidUseCase(notmidAuthRepository)
            }
            var authState by remember(notmidAuthRepository) {
                mutableStateOf(getNotmidAuthState())
            }
            val appRouter = rememberAppRouter()
            val deepLinkResolver = remember { AppDeepLinkResolver() }
            val context = LocalContext.current

            LaunchedEffect(pendingDeepLink) {
                val uri = pendingDeepLink?.uri ?: return@LaunchedEffect
                deepLinkResolver.resolve(uri)?.let(appRouter::navigate)
            }

            LaunchedEffect(appRouter.pendingActivityRouteRequest) {
                val request = appRouter.pendingActivityRouteRequest ?: return@LaunchedEffect
                if (AppActivityRouteLauncher.launch(context, request.route)) {
                    appRouter.consumeActivityRouteRequest(request.id)
                }
            }

            notmidTheme(darkTheme = false) {
                NotmidShellScreen(
                    destinations = notmidDestinations,
                    authState = authState,
                    navigationStack = appRouter.notmidRouteStack,
                    onRouteEvent = { event -> appRouter.onRouteEvent(event) },
                    onContinueLocalAuth = {
                        authState = signInToNotmid()
                    },
                    onBrowseSignedOut = {
                        appRouter.onRouteEvent(
                            NotmidRouteEvent.DestinationSelected(NotmidDestinationIds.FEED),
                        )
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        updatePendingDeepLink(intent)
    }

    private fun updatePendingDeepLink(intent: Intent?) {
        val uri = intent?.data?.toString() ?: return
        pendingDeepLink = PendingDeepLink(
            uri = uri,
            id = ++pendingDeepLinkId,
        )
    }
}

private data class PendingDeepLink(
    val uri: String,
    val id: Int,
)
