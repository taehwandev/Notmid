package app.thdev.glassnavlab.core.notice.api.effect

import kotlinx.coroutines.flow.Flow

interface NoticeEffectViewModel {
    val effects: Flow<NoticeEffect>
}
