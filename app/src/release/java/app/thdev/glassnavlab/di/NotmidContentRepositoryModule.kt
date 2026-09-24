package app.thdev.glassnavlab.di

import app.thdev.glassnavlab.core.data.impl.notmid.ApiNotmidContentRepository
import app.thdev.glassnavlab.core.data.impl.notmid.ApiNotmidProtectedWriteRepository
import app.thdev.glassnavlab.core.data.impl.notmid.ObservableNotmidContentRepository
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteRepository
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NotmidContentRepositoryModule {
    @Provides
    @Singleton
    fun provideObservableContentRepository(
        @NotmidApi client: NotmidNetworkClient,
    ): ObservableNotmidContentRepository = ObservableNotmidContentRepository(
        ApiNotmidContentRepository(client),
    )

    @Provides
    @Singleton
    fun provideNotmidProtectedWriteRepository(
        @NotmidApi client: NotmidNetworkClient,
    ): NotmidProtectedWriteRepository = ApiNotmidProtectedWriteRepository(client)
}
