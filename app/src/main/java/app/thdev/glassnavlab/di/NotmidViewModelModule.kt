package app.thdev.glassnavlab.di

import app.thdev.glassnavlab.core.domain.notmid.GetNotmidDestinationsUseCase
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentRepository
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteExecutor
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteRepository
import app.thdev.glassnavlab.core.data.notmid.RepositoryNotmidProtectedWriteExecutor
import app.thdev.glassnavlab.core.data.notmid.ObservableNotmidContentRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(ViewModelComponent::class)
object NotmidViewModelModule {
    @Provides
    fun provideProtectedWriteExecutor(
        repository: NotmidProtectedWriteRepository,
        content: ObservableNotmidContentRepository,
    ): NotmidProtectedWriteExecutor = RepositoryNotmidProtectedWriteExecutor(repository, content)

    @Provides
    fun provideGetNotmidDestinationsUseCase(
        repository: NotmidContentRepository,
    ): GetNotmidDestinationsUseCase {
        return GetNotmidDestinationsUseCase(repository)
    }

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher {
        return Dispatchers.IO
    }
}
