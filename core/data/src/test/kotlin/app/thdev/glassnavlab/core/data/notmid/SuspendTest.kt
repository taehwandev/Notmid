package app.thdev.glassnavlab.core.data.notmid

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

internal fun <T> runSuspend(block: suspend () -> T): T {
    var outcome: Any? = Unset
    block.startCoroutine(
        object : Continuation<T> {
            override val context = EmptyCoroutineContext

            override fun resumeWith(result: Result<T>) {
                outcome = result
            }
        },
    )

    check(outcome !== Unset) { "Suspend test block did not complete synchronously." }
    @Suppress("UNCHECKED_CAST")
    return (outcome as Result<T>).getOrThrow()
}

private data object Unset
