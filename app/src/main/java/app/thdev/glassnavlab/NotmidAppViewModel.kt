package app.thdev.glassnavlab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.thdev.glassnavlab.di.IoDispatcher
import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthGateway
import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthIntent
import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthResult
import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthSignInRequest
import app.thdev.glassnavlab.core.data.notmid.NotmidContentSource
import app.thdev.glassnavlab.core.domain.notmid.GetNotmidDestinationsUseCase
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteRepository
import app.thdev.glassnavlab.core.notice.api.effect.NoticeEffect
import app.thdev.glassnavlab.core.notice.api.effect.NoticeEffectDelegate
import app.thdev.glassnavlab.core.notice.api.effect.NoticeEffectViewModel
import app.thdev.glassnavlab.core.notice.api.effect.MutableNoticeEffectDelegate
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthMode
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthProvider
import app.thdev.glassnavlab.core.model.notmid.ChannelNotmidActionDelegate
import app.thdev.glassnavlab.core.model.notmid.NotmidActionDelegate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import javax.inject.Inject

@HiltViewModel
internal class NotmidAppViewModel internal constructor(
    private val contentSource: NotmidContentSource,
    private val getDestinations: GetNotmidDestinationsUseCase,
    private val protectedWriteRepository: NotmidProtectedWriteRepository,
    private val authGateway: NotmidAuthGateway,
    private val actionDelegate: NotmidActionDelegate<NotmidAppAction>,
    private val uiEffects: NoticeEffectDelegate,
    @param:IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
) : ViewModel(), NoticeEffectViewModel by uiEffects {
    @Inject
    constructor(
        contentSource: NotmidContentSource,
        getDestinations: GetNotmidDestinationsUseCase,
        protectedWriteRepository: NotmidProtectedWriteRepository,
        authGateway: NotmidAuthGateway,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ) : this(
        contentSource = contentSource,
        getDestinations = getDestinations,
        protectedWriteRepository = protectedWriteRepository,
        authGateway = authGateway,
        actionDelegate = ChannelNotmidActionDelegate(),
        uiEffects = MutableNoticeEffectDelegate(),
        ioDispatcher = ioDispatcher,
    )

    private val mutableState = MutableStateFlow(
        NotmidAppUiState(
            contentSource = contentSource,
            authState = authGateway.currentState(),
        ),
    )
    private var contentJob: Job? = null
    private var authJob: Job? = null
    private var protectedActionJob: Job? = null

    val state: StateFlow<NotmidAppUiState> = mutableState.asStateFlow()

    init {
        actionDelegate
            .actions
            .onEach(::handleAction)
            .launchIn(viewModelScope)
        reloadContent()
    }

    fun onAction(action: NotmidAppAction) {
        if (actionDelegate.tryDispatch(action)) {
            return
        }

        viewModelScope.launch {
            runCatchingPreservingCancellation {
                actionDelegate.dispatch(action)
            }
        }
    }

    private fun handleAction(action: NotmidAppAction) {
        when (action) {
            NotmidAppAction.ReloadContent -> reloadContent()
            is NotmidAppAction.ContinueAuth -> continueAuth(action.provider)
            NotmidAppAction.BrowseSignedOut -> clearAuthError()
            is NotmidAppAction.PublishCapture -> enqueueProtectedAction(
                PendingNotmidProtectedAction.PublishCapture(action.request),
            )

            is NotmidAppAction.SaveClip -> enqueueProtectedAction(
                PendingNotmidProtectedAction.SaveClip(action.clipId),
            )

            is NotmidAppAction.SendThreadMessage -> enqueueProtectedAction(
                PendingNotmidProtectedAction.SendThreadMessage(
                    threadId = action.threadId,
                    request = action.request,
                ),
            )

            is NotmidAppAction.StartThread -> enqueueProtectedAction(
                PendingNotmidProtectedAction.StartThread(action.request),
            )

            is NotmidAppAction.RespondThreadInvite -> enqueueProtectedAction(
                PendingNotmidProtectedAction.RespondThreadInvite(
                    threadId = action.threadId,
                    decision = action.decision,
                ),
            )

            is NotmidAppAction.UpdateProfileSettings -> enqueueProtectedAction(
                PendingNotmidProtectedAction.UpdateProfileSettings(action.request),
            )
        }
    }

    fun primaryAuthProvider(): NotmidAuthProvider {
        return when (mutableState.value.authState.mode) {
            NotmidAuthMode.Firebase -> NotmidAuthProvider.Anonymous

            NotmidAuthMode.Fake,
            NotmidAuthMode.Disabled,
            -> NotmidAuthProvider.Fake
        }
    }

    private fun reloadContent() {
        contentJob?.cancel()
        contentJob = viewModelScope.launch {
            mutableState.update { state ->
                state.copy(content = NotmidContentUiState.Loading)
            }

            val contentState = withContext(ioDispatcher) {
                runCatchingPreservingCancellation {
                    getDestinations()
                }.fold(
                    onSuccess = { destinations ->
                        notmidContentReadyOrError(
                            source = contentSource,
                            destinations = destinations,
                        )
                    },
                    onFailure = { throwable ->
                        notmidContentError(
                            source = contentSource,
                            throwable = throwable,
                        )
                    },
                )
            }

            mutableState.update { state ->
                state.copy(content = contentState)
            }
        }
    }

    private fun continueAuth(provider: NotmidAuthProvider) {
        if (authJob?.isActive == true || mutableState.value.isAuthenticating) {
            return
        }

        authJob = viewModelScope.launch {
            mutableState.update { state ->
                state.copy(
                    isAuthenticating = true,
                    authErrorMessage = null,
                )
            }

            val signInResult = withContext(ioDispatcher) {
                runCatchingPreservingCancellation {
                    authGateway.signIn(
                        NotmidAuthSignInRequest(
                            provider = provider,
                            intent = NotmidAuthIntent.Browse,
                        ),
                    )
                }
            }

            signInResult.onSuccess { result ->
                when (result) {
                    is NotmidAuthResult.Success -> {
                        mutableState.update { state ->
                            state.copy(
                                authState = result.state,
                                authErrorMessage = null,
                            )
                        }
                    }

                    is NotmidAuthResult.Rejected -> {
                        mutableState.update { state ->
                            state.copy(
                                authState = result.state,
                                authErrorMessage = result.message,
                            )
                        }
                    }
                }
            }.onFailure {
                mutableState.update { state ->
                    state.copy(
                        authState = authGateway.currentState(),
                        authErrorMessage = "Sign-in failed. Try again.",
                    )
                }
            }

            mutableState.update { state ->
                state.copy(isAuthenticating = false)
            }
        }
    }

    private fun clearAuthError() {
        mutableState.update { state ->
            state.copy(authErrorMessage = null)
        }
    }

    private fun enqueueProtectedAction(action: PendingNotmidProtectedAction) {
        if (
            protectedActionJob?.isActive == true ||
            mutableState.value.protectedActionInFlight != null
        ) {
            return
        }

        protectedActionJob = viewModelScope.launch {
            mutableState.update { state ->
                state.copy(
                    protectedActionInFlight = action.writeAction,
                    protectedActionNotice = null,
                )
            }

            var updatedAuthState = mutableState.value.authState
            var contentUpdate: (NotmidContentUiState) -> NotmidContentUiState = { content ->
                content
            }
            var followUpEffect: NoticeEffect? = null
            val notice = runCatchingPreservingCancellation {
                when (action) {
                    is PendingNotmidProtectedAction.PublishCapture -> {
                        withContext(ioDispatcher) {
                            protectedWriteRepository.publishCapture(
                                authState = updatedAuthState,
                                request = action.request,
                            )
                        }
                        action.writeAction.toSuccessNotice()
                    }

                    is PendingNotmidProtectedAction.SaveClip -> {
                        withContext(ioDispatcher) {
                            protectedWriteRepository.saveClip(
                                authState = updatedAuthState,
                                clipId = action.clipId,
                            )
                        }
                        action.writeAction.toSuccessNotice()
                    }

                    is PendingNotmidProtectedAction.SendThreadMessage -> {
                        val receipt = withContext(ioDispatcher) {
                            protectedWriteRepository.sendThreadMessage(
                                authState = updatedAuthState,
                                threadId = action.threadId,
                                request = action.request,
                            )
                        }
                        contentUpdate = { content ->
                            content.withThreadMessage(receipt.message)
                        }
                        action.writeAction.toSuccessNotice()
                    }

                    is PendingNotmidProtectedAction.StartThread -> {
                        val receipt = withContext(ioDispatcher) {
                            protectedWriteRepository.startThread(
                                authState = updatedAuthState,
                                request = action.request,
                            )
                        }
                        contentUpdate = { content ->
                            val contentWithThread = content.withThread(receipt.thread)
                            receipt.message?.let(contentWithThread::withThreadMessage)
                                ?: contentWithThread
                        }
                        followUpEffect = NoticeEffect.NavigateDeepLink(
                            notmidChatThreadDeepLink(receipt.thread.id),
                        )
                        action.writeAction.toSuccessNotice()
                    }

                    is PendingNotmidProtectedAction.RespondThreadInvite -> {
                        val receipt = withContext(ioDispatcher) {
                            protectedWriteRepository.respondThreadInvite(
                                authState = updatedAuthState,
                                threadId = action.threadId,
                                decision = action.decision,
                            )
                        }
                        contentUpdate = { content ->
                            content.withThread(receipt.thread)
                        }
                        action.writeAction.toSuccessNotice()
                    }

                    is PendingNotmidProtectedAction.UpdateProfileSettings -> {
                        val receipt = withContext(ioDispatcher) {
                            protectedWriteRepository.updateProfileSettings(
                                authState = updatedAuthState,
                                request = action.request,
                            )
                        }
                        updatedAuthState = updatedAuthState.copy(
                            session = updatedAuthState.session?.copy(user = receipt.settings.user),
                        )
                        action.writeAction.toSuccessNotice()
                    }
                }
            }.getOrElse { throwable ->
                throwable.toProtectedActionNotice(action.writeAction)
            }

            mutableState.update { state ->
                state.copy(
                    authState = updatedAuthState,
                    content = contentUpdate(state.content),
                    protectedActionInFlight = null,
                    protectedActionNotice = notice,
                )
            }
            emitEffect(notice.effect)
            followUpEffect?.let(::emitEffect)
        }
    }

    private fun emitEffect(effect: NoticeEffect) {
        uiEffects.emit(effect)
    }

    override fun onCleared() {
        actionDelegate.close()
        super.onCleared()
    }
}

private fun notmidChatThreadDeepLink(threadId: String): String {
    return "https://thdev.app/notmid/inbox/chats/${threadId.urlPathSegment()}"
}

private fun String.urlPathSegment(): String {
    return URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")
}

private suspend fun <T> runCatchingPreservingCancellation(
    block: suspend () -> T,
): Result<T> {
    return try {
        Result.success(block())
    } catch (exception: CancellationException) {
        throw exception
    } catch (throwable: Throwable) {
        Result.failure(throwable)
    }
}
