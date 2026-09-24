package app.thdev.glassnavlab.di

import app.thdev.glassnavlab.core.data.impl.notmid.ObservableNotmidContentRepository
import app.thdev.glassnavlab.core.data.impl.notmid.RepositoryNotmidProtectedWriteExecutor
import app.thdev.glassnavlab.core.data.impl.notmid.SingleFlightNotmidProtectedWriteExecutor
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteExecutor
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteRepository
import app.thdev.glassnavlab.core.notice.api.effect.MutableNoticeEffectDelegate
import app.thdev.glassnavlab.core.notice.api.effect.NoticeEffectDelegate
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped

@Module
@InstallIn(ActivityRetainedComponent::class)
object NotmidWriteRuntimeModule {
    @Provides
    @ActivityRetainedScoped
    fun provideExecutor(repository: NotmidProtectedWriteRepository, content: ObservableNotmidContentRepository): NotmidProtectedWriteExecutor =
        SingleFlightNotmidProtectedWriteExecutor(RepositoryNotmidProtectedWriteExecutor(repository, content))

    @Provides
    @ActivityRetainedScoped
    fun provideNotices(): NoticeEffectDelegate = MutableNoticeEffectDelegate()
}
