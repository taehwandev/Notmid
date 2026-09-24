package app.thdev.glassnavlab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.thdev.glassnavlab.di.IoDispatcher
import app.thdev.glassnavlab.core.data.notmid.NotmidContentSource
import app.thdev.glassnavlab.core.domain.notmid.GetNotmidDestinationsUseCase
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentUpdates
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentSnapshot
import app.thdev.glassnavlab.core.notice.api.effect.NoticeEffectDelegate
import app.thdev.glassnavlab.core.notice.api.effect.NoticeEffectViewModel
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
import javax.inject.Inject

@HiltViewModel
internal class NotmidAppViewModel @Inject constructor(
    private val contentSource: NotmidContentSource,
    private val getDestinations: GetNotmidDestinationsUseCase,
    private val contentUpdates: NotmidContentUpdates,
    private val uiEffects: NoticeEffectDelegate,
    @param:IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
) : ViewModel(), NoticeEffectViewModel by uiEffects {

    private val mutableState = MutableStateFlow(
        NotmidAppUiState(
            contentSource = contentSource,
        ),
    )
    private var contentJob: Job? = null

    val state: StateFlow<NotmidAppUiState> = mutableState.asStateFlow()

    init {
        contentUpdates.snapshot.onEach { snapshot ->
            if (snapshot is NotmidContentSnapshot.Ready) {
                mutableState.update { it.copy(content = notmidContentReadyOrError(contentSource, snapshot.destinations)) }
            }
        }.launchIn(viewModelScope)
        reloadContent()
    }

    fun onAction(action: NotmidAppAction) {
        when (action) {
            NotmidAppAction.ReloadContent -> reloadContent()
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
