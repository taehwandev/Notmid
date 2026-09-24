package app.thdev.glassnavlab

import app.thdev.glassnavlab.feature.notmid.notice.toSuccessNotice
import app.thdev.glassnavlab.feature.notmid.notice.toProtectedActionNotice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.thdev.glassnavlab.di.IoDispatcher
import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthGateway
import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthIntent
import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthResult
import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthSignInRequest
import app.thdev.glassnavlab.core.data.notmid.NotmidContentSource
import app.thdev.glassnavlab.core.domain.notmid.GetNotmidDestinationsUseCase
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentUpdates
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentSnapshot
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteExecutor
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteRequest
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteResult
import app.thdev.glassnavlab.core.notice.api.effect.NoticeEffect
import app.thdev.glassnavlab.core.notice.api.effect.NoticeEffectDelegate
import app.thdev.glassnavlab.core.notice.api.effect.NoticeEffectViewModel
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
import app.thdev.glassnavlab.core.navigation.runtime.RouteEventSink
import app.thdev.glassnavlab.core.navigation.notmid.NotmidRouteEvent
import app.thdev.glassnavlab.core.navigation.notmid.NotmidDestinationIds
import javax.inject.Inject

@HiltViewModel
internal class NotmidAppViewModel internal constructor(
    private val contentSource: NotmidContentSource,
    private val getDestinations: GetNotmidDestinationsUseCase,
    private val contentUpdates: NotmidContentUpdates,
    private val protectedWriteExecutor: NotmidProtectedWriteExecutor,
    private val authGateway: NotmidAuthGateway,
    private val actionDelegate: NotmidActionDelegate<NotmidAppAction>,
    private val uiEffects: NoticeEffectDelegate,
    @param:IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
    private val protectedWriteActions: NotmidActionDelegate<NotmidProtectedWriteRequest>,
    private val routeEvents: RouteEventSink,
) : ViewModel(), NoticeEffectViewModel by uiEffects {
    @Inject
    constructor(
        contentSource: NotmidContentSource,
        getDestinations: GetNotmidDestinationsUseCase,
        contentUpdates: NotmidContentUpdates,
        protectedWriteExecutor: NotmidProtectedWriteExecutor,
        authGateway: NotmidAuthGateway,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        uiEffects: NoticeEffectDelegate,
        protectedWriteActions: NotmidActionDelegate<NotmidProtectedWriteRequest>,
        routeEvents: RouteEventSink,
    ) : this(
        contentSource = contentSource,
        getDestinations = getDestinations,
        contentUpdates = contentUpdates,
        protectedWriteExecutor = protectedWriteExecutor,
        authGateway = authGateway,
        actionDelegate = ChannelNotmidActionDelegate(),
        uiEffects = uiEffects,
        ioDispatcher = ioDispatcher,
        protectedWriteActions = protectedWriteActions,
        routeEvents = routeEvents,
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
        authGateway.states.onEach { authState ->
            mutableState.update { it.copy(authState = authState) }
        }.launchIn(viewModelScope)
        contentUpdates.snapshot.onEach { snapshot ->
            if (snapshot is NotmidContentSnapshot.Ready) {
                mutableState.update { it.copy(content = notmidContentReadyOrError(contentSource, snapshot.destinations)) }
            }
        }.launchIn(viewModelScope)
        protectedWriteActions.actions.onEach(::enqueueProtectedAction).launchIn(viewModelScope)
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
            NotmidAppAction.ContinuePrimaryAuth -> continueAuth(primaryAuthProvider())
            is NotmidAppAction.RouteRequested -> routeEvents.onRouteEvent(action.event)
            is NotmidAppAction.ContinueAuth -> continueAuth(action.provider)
            NotmidAppAction.BrowseSignedOut -> {
                clearAuthError()
                routeEvents.onRouteEvent(NotmidRouteEvent.DestinationSelected(NotmidDestinationIds.FEED))
            }
            is NotmidAppAction.PublishCapture -> enqueueProtectedAction(
                NotmidProtectedWriteRequest.PublishCapture(action.request),
            )

            is NotmidAppAction.SaveClip -> enqueueProtectedAction(
                NotmidProtectedWriteRequest.SaveClip(action.clipId),
            )

            is NotmidAppAction.SendThreadMessage -> enqueueProtectedAction(
                NotmidProtectedWriteRequest.SendThreadMessage(
                    threadId = action.threadId,
                    request = action.request,
                ),
            )

            is NotmidAppAction.StartThread -> enqueueProtectedAction(
                NotmidProtectedWriteRequest.StartThread(action.request),
            )

            is NotmidAppAction.RespondThreadInvite -> enqueueProtectedAction(
                NotmidProtectedWriteRequest.RespondThreadInvite(
                    threadId = action.threadId,
                    decision = action.decision,
                ),
            )

        }
    }

    private fun primaryAuthProvider(): NotmidAuthProvider {
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
                val current = contentUpdates.snapshot.value
                state.copy(content = if (current is NotmidContentSnapshot.Ready) {
                    notmidContentReadyOrError(contentSource, current.destinations)
                } else contentState)
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

    private fun enqueueProtectedAction(action: NotmidProtectedWriteRequest) {
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

            val requestAuthState = authGateway.currentState()
            var followUpEffect: NoticeEffect? = null
            val notice = runCatchingPreservingCancellation {
                val result = withContext(ioDispatcher) {
                    protectedWriteExecutor.execute(requestAuthState, action)
                }
                when (result) {
                    NotmidProtectedWriteResult.Completed -> Unit
                    NotmidProtectedWriteResult.Busy -> {
                        return@runCatchingPreservingCancellation null
                    }
                    is NotmidProtectedWriteResult.MessageSent -> Unit
                    is NotmidProtectedWriteResult.ThreadStarted -> {
                        followUpEffect = NoticeEffect.NavigateDeepLink(
                            notmidChatThreadDeepLink(result.thread.id),
                        )
                    }
                    is NotmidProtectedWriteResult.ThreadUpdated -> Unit
                    is NotmidProtectedWriteResult.ProfileUpdated -> Unit
                }
                action.writeAction.toSuccessNotice()
            }.getOrElse { throwable ->
                throwable.toProtectedActionNotice(action.writeAction)
            }

            mutableState.update { state ->
                state.copy(
                    authState = authGateway.currentState(),
                    protectedActionInFlight = null,
                    protectedActionNotice = notice,
                )
            }
            notice?.let { emitEffect(it.effect) }
            followUpEffect?.let(::emitEffect)
        }
    }

    private fun emitEffect(effect: NoticeEffect) {
        uiEffects.emit(effect)
    }

    override fun onCleared() {
        actionDelegate.close()
        protectedWriteActions.close()
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
