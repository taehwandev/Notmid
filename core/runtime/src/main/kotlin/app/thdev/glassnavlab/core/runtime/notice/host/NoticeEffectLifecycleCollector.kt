package app.thdev.glassnavlab.core.runtime.notice.host

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.LifecycleStartEffect
import app.thdev.glassnavlab.core.notice.api.effect.NoticeEffect
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Composable
internal fun NoticeEffectLifecycleCollector(
    effects: Flow<NoticeEffect>,
    onEffect: (NoticeEffect) -> Unit,
) {
    val collectionScope = rememberCoroutineScope()
    val currentOnEffect by rememberUpdatedState(onEffect)

    LifecycleStartEffect(effects) {
        var collectionJob: Job? = collectionScope.launch {
            effects.collect { effect ->
                currentOnEffect(effect)
            }
        }

        onStopOrDispose {
            collectionJob?.cancel()
            collectionJob = null
        }
    }
}
