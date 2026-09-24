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
import app.thdev.glassnavlab.core.model.notmid.NotmidSendThreadMessageRequest
import app.thdev.glassnavlab.core.runtime.router.activity.ActivityRouteLauncher
import app.thdev.glassnavlab.feature.notmid.NotmidShellErrorScreen
import app.thdev.glassnavlab.feature.notmid.NotmidShellLoadingScreen
import app.thdev.glassnavlab.feature.notmid.NotmidShellScreen
import app.thdev.glassnavlab.core.runtime.router.runtime.AppRouterRuntime
import app.thdev.glassnavlab.feature.notmid.router.notmidRouteStack
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseActivity() {
    @Inject
    lateinit var activityRouteLauncher: ActivityRouteLauncher

    @Inject
    lateinit var appRouter: AppRouterRuntime

    @Composable
    override fun Content() {
        val notmidAppViewModel: NotmidAppViewModel = viewModel()
        val appState by notmidAppViewModel.state.collectAsStateWithLifecycle()

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
                        capturePublishMessage = appState.messageFor(
                            NotmidProtectedWriteAction.CapturePublish,
                        ),
                        clipSaveMessage = appState.messageFor(
                            NotmidProtectedWriteAction.ClipSave,
                        ),
                        chatMessage = appState.messageFor(
                            NotmidProtectedWriteAction.ChatMessage,
                        ),
                        navigationStack = appRouter.notmidRouteStack(),
                        onContinueLocalAuth = {
                            notmidAppViewModel.onAction(
                                NotmidAppAction.ContinuePrimaryAuth,
                            )
                        },
                        onContinueGoogleAuth = {
                            notmidAppViewModel.onAction(
                                NotmidAppAction.ContinueAuth(NotmidAuthProvider.Google),
                            )
                        },
                        onBrowseSignedOut = {
                            notmidAppViewModel.onAction(NotmidAppAction.BrowseSignedOut)
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
