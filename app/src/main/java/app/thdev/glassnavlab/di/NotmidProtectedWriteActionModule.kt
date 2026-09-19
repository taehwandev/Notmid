package app.thdev.glassnavlab.di

import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteRequest
import app.thdev.glassnavlab.core.model.notmid.ChannelNotmidActionDelegate
import app.thdev.glassnavlab.core.model.notmid.NotmidActionDelegate
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped

@Module
@InstallIn(ActivityRetainedComponent::class)
object NotmidProtectedWriteActionModule {
    @Provides
    @ActivityRetainedScoped
    fun provideProtectedWriteActions(): NotmidActionDelegate<NotmidProtectedWriteRequest> =
        ChannelNotmidActionDelegate()
}
