package app.thdev.glassnavlab.feature.map.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(ViewModelComponent::class)
internal object MapViewModelModule {
    @Provides
    @MapIoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
