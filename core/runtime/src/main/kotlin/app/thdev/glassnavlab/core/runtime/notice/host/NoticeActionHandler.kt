package app.thdev.glassnavlab.core.runtime.notice.host

import app.thdev.glassnavlab.core.notice.api.model.NoticeAction

internal fun NoticeAction.perform(onActionDeepLink: (String) -> Unit) {
    when (this) {
        is NoticeAction.OpenDeepLink -> {
            deepLink
                .takeIf(String::isNotBlank)
                ?.let(onActionDeepLink)
        }
    }
}
