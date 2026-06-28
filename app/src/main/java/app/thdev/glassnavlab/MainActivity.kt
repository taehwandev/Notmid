package app.thdev.glassnavlab

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.thdev.glassnavlab.auth.AndroidCredentialManagerGoogleIdTokenProvider
import app.thdev.glassnavlab.config.NotmidRuntimeConfig
import app.thdev.glassnavlab.core.auth.notmid.ApiVerifiedNotmidAuthGateway
import app.thdev.glassnavlab.core.auth.notmid.FirebaseAuthRestIdTokenProvider
import app.thdev.glassnavlab.core.auth.notmid.FirebaseIdTokenProvider
import app.thdev.glassnavlab.core.auth.notmid.LocalNotmidAuthGateway
import app.thdev.glassnavlab.core.auth.notmid.UnavailableFirebaseIdTokenProvider
import app.thdev.glassnavlab.core.base.activity.BaseActivity
import app.thdev.glassnavlab.core.data.notmid.ApiNotmidContentRepository
import app.thdev.glassnavlab.core.data.notmid.ApiNotmidProtectedWriteRepository
import app.thdev.glassnavlab.core.data.notmid.NotmidContentSource
import app.thdev.glassnavlab.core.data.notmid.NotmidContentRepositorySelector
import app.thdev.glassnavlab.core.data.notmid.StaticNotmidContentRepository
import app.thdev.glassnavlab.core.data.notmid.StaticNotmidProtectedWriteRepository
import app.thdev.glassnavlab.core.designsystem.theme.notmidTheme
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteAction
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteRepository
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthMode
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthProvider
import app.thdev.glassnavlab.core.model.notmid.NotmidCapturePublishRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidCaptureVisibility
import app.thdev.glassnavlab.core.model.notmid.NotmidChatInviteDecision
import app.thdev.glassnavlab.core.model.notmid.NotmidProfileSettingsUpdateRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidSendThreadMessageRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidStartThreadRequest
import app.thdev.glassnavlab.core.network.notmid.OkHttpNotmidNetworkClient
import app.thdev.glassnavlab.core.runtime.router.activity.ActivityRouteLauncher
import app.thdev.glassnavlab.feature.notmid.NotmidShellErrorScreen
import app.thdev.glassnavlab.feature.notmid.NotmidShellLoadingScreen
import app.thdev.glassnavlab.feature.notmid.NotmidShellScreen
import app.thdev.glassnavlab.feature.notmid.api.destination.NotmidDestinationIds
import app.thdev.glassnavlab.feature.notmid.api.event.NotmidRouteEvent
import app.thdev.glassnavlab.feature.notmid.router.notmidRouteStack
import app.thdev.glassnavlab.feature.notmid.router.rememberNotmidAppRouter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseActivity() {
    @Inject
    lateinit var activityRouteLauncher: ActivityRouteLauncher

    @Composable
    override fun Content() {
        val notmidContentSource = NotmidRuntimeConfig.contentSource
        val context = LocalContext.current
        val applicationContext = context.applicationContext
        val notmidNetworkClient = remember {
            OkHttpNotmidNetworkClient(NotmidRuntimeConfig.apiConfig)
        }
        val notmidContentRepositorySelector = remember {
            NotmidContentRepositorySelector(
                staticRepositoryFactory = { StaticNotmidContentRepository() },
                apiRepositoryFactory = {
                    ApiNotmidContentRepository(notmidNetworkClient)
                },
            )
        }
        val notmidContentRepository = remember(
            notmidContentSource,
            notmidContentRepositorySelector,
        ) {
            notmidContentRepositorySelector.select(notmidContentSource)
        }
        val notmidProtectedWriteRepository = remember(
            notmidContentSource,
            notmidNetworkClient,
        ) {
            notmidProtectedWriteRepository(
                source = notmidContentSource,
                networkClient = notmidNetworkClient,
            )
        }
        val firebaseIdTokenProvider = remember(applicationContext) {
            val provider: FirebaseIdTokenProvider = if (
                NotmidRuntimeConfig.firebaseAuthConfig.isConfigured
            ) {
                FirebaseAuthRestIdTokenProvider(
                    client = OkHttpNotmidNetworkClient(
                        NotmidRuntimeConfig.firebaseIdentityToolkitApiConfig,
                    ),
                    config = NotmidRuntimeConfig.firebaseAuthConfig,
                    googleIdTokenProvider = AndroidCredentialManagerGoogleIdTokenProvider(
                        context = applicationContext,
                        serverClientId = NotmidRuntimeConfig.googleServerClientId,
                    ),
                )
            } else {
                UnavailableFirebaseIdTokenProvider()
            }
            provider
        }
        val notmidAuthGateway = remember(
            notmidNetworkClient,
            firebaseIdTokenProvider,
        ) {
            when (NotmidRuntimeConfig.authMode) {
                NotmidAuthMode.Firebase -> ApiVerifiedNotmidAuthGateway(
                    client = notmidNetworkClient,
                    idTokenProvider = firebaseIdTokenProvider,
                )

                NotmidAuthMode.Fake,
                NotmidAuthMode.Disabled,
                -> LocalNotmidAuthGateway(mode = NotmidRuntimeConfig.authMode)
            }
        }
        val notmidAppViewModelFactory = remember(
            notmidContentSource,
            notmidContentRepository,
            notmidProtectedWriteRepository,
            notmidAuthGateway,
        ) {
            NotmidAppViewModel.Factory(
                contentSource = notmidContentSource,
                contentRepository = notmidContentRepository,
                protectedWriteRepository = notmidProtectedWriteRepository,
                authGateway = notmidAuthGateway,
            )
        }
        val notmidAppViewModel: NotmidAppViewModel = viewModel(
            factory = notmidAppViewModelFactory,
        )
        val appState by notmidAppViewModel.state.collectAsStateWithLifecycle()
        val appRouter = rememberNotmidAppRouter()

        BaseAppRoot(
            router = appRouter,
            activityRouteLauncher = activityRouteLauncher,
            noticeEffects = notmidAppViewModel.effects,
            theme = { content ->
                notmidTheme(darkTheme = false) {
                    content()
                }
            },
        ) {
            when (val contentState = appState.content) {
                NotmidContentUiState.Loading -> {
                    NotmidShellLoadingScreen(sourceLabel = appState.contentSource.label)
                }

                is NotmidContentUiState.Error -> {
                    NotmidShellErrorScreen(
                        title = contentState.title,
                        message = contentState.message,
                        onRetry = {
                            notmidAppViewModel.onAction(NotmidAppAction.ReloadContent)
                        },
                    )
                }

                is NotmidContentUiState.Ready -> {
                    NotmidShellScreen(
                        destinations = contentState.destinations,
                        authState = appState.authState,
                        authErrorMessage = appState.authErrorMessage,
                        isAuthenticating = appState.isAuthenticating,
                        isPublishingCapture = appState.isPublishingCapture,
                        isSavingClip = appState.isSavingClip,
                        isSendingMessage = appState.isSendingMessage,
                        isStartingChat = appState.isStartingChat,
                        isRespondingChatInvite = appState.isRespondingChatInvite,
                        isSavingProfileSettings = appState.isSavingProfileSettings,
                        capturePublishMessage = appState.messageFor(
                            NotmidProtectedWriteAction.CapturePublish,
                        ),
                        clipSaveMessage = appState.messageFor(
                            NotmidProtectedWriteAction.ClipSave,
                        ),
                        chatMessage = appState.messageFor(
                            NotmidProtectedWriteAction.ChatMessage,
                        ),
                        profileSettingsMessage = appState.messageFor(
                            NotmidProtectedWriteAction.ProfileSettings,
                        ),
                        navigationStack = appRouter.notmidRouteStack(),
                        onRouteEvent = { event -> appRouter.onRouteEvent(event) },
                        onContinueLocalAuth = {
                            notmidAppViewModel.onAction(
                                NotmidAppAction.ContinueAuth(
                                    notmidAppViewModel.primaryAuthProvider(),
                                ),
                            )
                        },
                        onContinueGoogleAuth = {
                            notmidAppViewModel.onAction(
                                NotmidAppAction.ContinueAuth(NotmidAuthProvider.Google),
                            )
                        },
                        onBrowseSignedOut = {
                            notmidAppViewModel.onAction(NotmidAppAction.BrowseSignedOut)
                            appRouter.onRouteEvent(
                                NotmidRouteEvent.DestinationSelected(
                                    NotmidDestinationIds.FEED,
                                ),
                            )
                        },
                        onPublishCapture = {
                            draftId,
                            caption,
                            placeId,
                            moodTags,
                            visibility,
                            ->
                            notmidAppViewModel.onAction(
                                NotmidAppAction.PublishCapture(
                                    NotmidCapturePublishRequest(
                                        draftId = draftId,
                                        caption = caption,
                                        placeId = placeId,
                                        moodTags = moodTags,
                                        visibility = visibility.toNotmidCaptureVisibility(),
                                    ),
                                ),
                            )
                        },
                        onSaveClip = { clipId ->
                            notmidAppViewModel.onAction(
                                NotmidAppAction.SaveClip(clipId),
                            )
                        },
                        onAcceptThreadInvite = { threadId ->
                            notmidAppViewModel.onAction(
                                NotmidAppAction.RespondThreadInvite(
                                    threadId = threadId,
                                    decision = NotmidChatInviteDecision.Accept,
                                ),
                            )
                        },
                        onRejectThreadInvite = { threadId ->
                            notmidAppViewModel.onAction(
                                NotmidAppAction.RespondThreadInvite(
                                    threadId = threadId,
                                    decision = NotmidChatInviteDecision.Reject,
                                ),
                            )
                        },
                        onSendThreadMessage = { threadId, body ->
                            notmidAppViewModel.onAction(
                                NotmidAppAction.SendThreadMessage(
                                    threadId = threadId,
                                    request = NotmidSendThreadMessageRequest(body = body),
                                ),
                            )
                        },
                        onStartThread = {
                            participantHandle,
                            body,
                            attachedClipId,
                            attachedPlaceId,
                            ->
                            notmidAppViewModel.onAction(
                                NotmidAppAction.StartThread(
                                    NotmidStartThreadRequest(
                                        participantHandle = participantHandle,
                                        body = body,
                                        attachedClipId = attachedClipId,
                                        attachedPlaceId = attachedPlaceId,
                                    ),
                                ),
                            )
                        },
                        onUpdateProfileSettings = { displayName, homeNeighborhood ->
                            notmidAppViewModel.onAction(
                                NotmidAppAction.UpdateProfileSettings(
                                    NotmidProfileSettingsUpdateRequest(
                                        displayName = displayName,
                                        homeNeighborhood = homeNeighborhood,
                                    ),
                                ),
                            )
                        },
                    )
                }
            }
        }
    }
}

private fun notmidProtectedWriteRepository(
    source: NotmidContentSource,
    networkClient: OkHttpNotmidNetworkClient,
): NotmidProtectedWriteRepository {
    return when (source) {
        NotmidContentSource.Static -> StaticNotmidProtectedWriteRepository()
        NotmidContentSource.Api -> ApiNotmidProtectedWriteRepository(networkClient)
    }
}

private fun String.toNotmidCaptureVisibility(): NotmidCaptureVisibility {
    return when (this) {
        "friends" -> NotmidCaptureVisibility.Friends
        "private" -> NotmidCaptureVisibility.Private
        else -> NotmidCaptureVisibility.Public
    }
}
