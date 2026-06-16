package app.thdev.glassnavlab.core.auth.notmid

import app.thdev.glassnavlab.core.model.notmid.NotmidAuthMode
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthRequiredAction
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState

internal val DefaultNotmidAuthRequiredActions = listOf(
    NotmidAuthRequiredAction.Capture,
    NotmidAuthRequiredAction.Save,
    NotmidAuthRequiredAction.Chat,
    NotmidAuthRequiredAction.ProfileEdit,
    NotmidAuthRequiredAction.Moderation,
)

internal fun signedOutState(mode: NotmidAuthMode): NotmidAuthState {
    return NotmidAuthState(
        mode = mode,
        session = null,
        requiredActions = DefaultNotmidAuthRequiredActions,
    )
}

internal val NotmidAuthIntent.defaultPath: String
    get() = when (this) {
        NotmidAuthIntent.Browse -> "/notmid"
        NotmidAuthIntent.Capture -> "/notmid/capture"
        NotmidAuthIntent.Chat -> "/notmid/inbox"
        NotmidAuthIntent.Profile -> "/notmid/profile"
    }

internal fun NotmidAuthSignInRequest.nextPath(): String {
    return returnToPath?.takeIf(::isSafeNotmidPath) ?: intent.defaultPath
}

internal fun isSafeNotmidPath(path: String): Boolean {
    return path.startsWith("/notmid") && !path.startsWith("//")
}
