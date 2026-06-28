package app.thdev.glassnavlab

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.thdev.glassnavlab.core.base.activity.BaseActivity
import app.thdev.glassnavlab.core.designsystem.theme.notmidTheme
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteAction
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthProvider
import app.thdev.glassnavlab.core.model.notmid.NotmidCapturePublishRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidCaptureVisibility
import app.thdev.glassnavlab.core.model.notmid.NotmidChatInviteDecision
import app.thdev.glassnavlab.core.model.notmid.NotmidProfileSettingsUpdateRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidSendThreadMessageRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidStartThreadRequest
import app.thdev.glassnavlab.core.runtime.router.activity.ActivityRouteLauncher
import app.thdev.glassnavlab.feature.notmid.NotmidShellErrorScreen
import app.thdev.glassnavlab.feature.notmid.NotmidShellLoadingScreen
import app.thdev.glassnavlab.feature.notmid.NotmidShellScreen
import app.thdev.glassnavlab.feature.notmid.api.destination.NotmidDestinationIds
import app.thdev.glassnavlab.feature.notmid.api.event.NotmidRouteEvent
import app.thdev.glassnavlab.feature.notmid.router.NotmidAppRouterFactory
import app.thdev.glassnavlab.feature.notmid.router.notmidRouteStack
import app.thdev.glassnavlab.feature.notmid.router.rememberNotmidAppRouter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseActivity() {
    @Inject
    lateinit var activityRouteLauncher: ActivityRouteLauncher

    @Inject
    lateinit var notmidAppRouterFactory: NotmidAppRouterFactory

    @Composable
    override fun Content() {
        val notmidAppViewModel: NotmidAppViewModel = viewModel()
        val appState by notmidAppViewModel.state.collectAsStateWithLifecycle()
        val appRouter = rememberNotmidAppRouter(notmidAppRouterFactory)

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

private fun String.toNotmidCaptureVisibility(): NotmidCaptureVisibility {
    return when (this) {
        "friends" -> NotmidCaptureVisibility.Friends
        "private" -> NotmidCaptureVisibility.Private
        else -> NotmidCaptureVisibility.Public
    }
}
