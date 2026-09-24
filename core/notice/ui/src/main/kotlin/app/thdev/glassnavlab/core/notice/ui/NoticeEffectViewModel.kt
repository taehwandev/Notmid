package app.thdev.glassnavlab.core.notice.ui

import kotlinx.coroutines.flow.Flow
import app.thdev.glassnavlab.core.notice.api.effect.NoticeEffect

interface NoticeEffectViewModel {
    val effects: Flow<NoticeEffect>
}
