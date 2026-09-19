package app.thdev.glassnavlab.feature.feed.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(ViewModelComponent::class)
internal object FeedViewModelModule {
    @Provides
    @FeedIoDispatcher
    fun provideDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
