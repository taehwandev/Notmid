package app.thdev.glassnavlab.di

import app.thdev.glassnavlab.core.domain.notmid.GetNotmidDestinationsUseCase
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentRepository
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
