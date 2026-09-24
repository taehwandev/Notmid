package app.thdev.glassnavlab.di

import app.thdev.glassnavlab.core.data.api.notmid.NotmidContentSource
import app.thdev.glassnavlab.core.data.assertions.notmid.NotmidContentRepositorySelector
import app.thdev.glassnavlab.core.data.assertions.notmid.StaticNotmidContentRepository
import app.thdev.glassnavlab.core.data.assertions.notmid.StaticNotmidProtectedWriteRepository
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
        source: NotmidContentSource,
        @NotmidApi client: NotmidNetworkClient,
    ): ObservableNotmidContentRepository {
        val repository = NotmidContentRepositorySelector(
            staticRepositoryFactory = ::StaticNotmidContentRepository,
            apiRepositoryFactory = { ApiNotmidContentRepository(client) },
        ).select(source)
        return ObservableNotmidContentRepository(repository)
    }

    @Provides
    @Singleton
    fun provideNotmidProtectedWriteRepository(
        source: NotmidContentSource,
        @NotmidApi client: NotmidNetworkClient,
    ): NotmidProtectedWriteRepository = when (source) {
        // Fixture lookups must not publish a refresh that replaces successful local receipts.
        NotmidContentSource.Static -> StaticNotmidProtectedWriteRepository()
        NotmidContentSource.Api -> ApiNotmidProtectedWriteRepository(client)
    }
}
