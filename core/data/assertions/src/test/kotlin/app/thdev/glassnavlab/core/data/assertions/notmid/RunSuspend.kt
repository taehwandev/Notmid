package app.thdev.glassnavlab.core.data.assertions.notmid

import kotlinx.coroutines.runBlocking

internal fun <T> runSuspend(block: suspend () -> T): T = runBlocking { block() }
