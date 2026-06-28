package app.thdev.glassnavlab.di

import android.content.Context
import app.thdev.glassnavlab.auth.AndroidCredentialManagerGoogleIdTokenProvider
import app.thdev.glassnavlab.config.NotmidRuntimeConfig
import app.thdev.glassnavlab.config.notmidRuntimeConfigFromBuildConfig
import app.thdev.glassnavlab.core.auth.notmid.ApiVerifiedNotmidAuthGateway
import app.thdev.glassnavlab.core.auth.notmid.FirebaseAuthRestConfig
import app.thdev.glassnavlab.core.auth.notmid.FirebaseAuthRestIdTokenProvider
import app.thdev.glassnavlab.core.auth.notmid.FirebaseIdTokenProvider
import app.thdev.glassnavlab.core.auth.notmid.GoogleIdTokenProvider
import app.thdev.glassnavlab.core.auth.notmid.LocalNotmidAuthGateway
import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthGateway
import app.thdev.glassnavlab.core.auth.notmid.UnavailableFirebaseIdTokenProvider
import app.thdev.glassnavlab.core.data.notmid.ApiNotmidContentRepository
import app.thdev.glassnavlab.core.data.notmid.ApiNotmidProtectedWriteRepository
import app.thdev.glassnavlab.core.data.notmid.NotmidContentSource
import app.thdev.glassnavlab.core.data.notmid.StaticNotmidContentRepository
import app.thdev.glassnavlab.core.data.notmid.StaticNotmidProtectedWriteRepository
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentRepository
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteRepository
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthMode
import app.thdev.glassnavlab.core.network.notmid.NotmidApiConfig
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkClient
import app.thdev.glassnavlab.core.network.notmid.OkHttpNotmidNetworkClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NotmidRuntimeModule {
    @Provides
    @Singleton
    fun provideNotmidRuntimeConfig(): NotmidRuntimeConfig {
        return notmidRuntimeConfigFromBuildConfig()
    }

    @Provides
    fun provideNotmidContentSource(config: NotmidRuntimeConfig): NotmidContentSource {
        return config.contentSource
    }

    @Provides
    fun provideNotmidAuthMode(config: NotmidRuntimeConfig): NotmidAuthMode {
        return config.authMode
    }

    @Provides
    @NotmidApi
    fun provideNotmidApiConfig(config: NotmidRuntimeConfig): NotmidApiConfig {
        return config.apiConfig
    }

    @Provides
    @FirebaseIdentityToolkit
    fun provideFirebaseIdentityToolkitApiConfig(config: NotmidRuntimeConfig): NotmidApiConfig {
        return config.firebaseIdentityToolkitApiConfig
    }

    @Provides
    fun provideFirebaseAuthRestConfig(config: NotmidRuntimeConfig): FirebaseAuthRestConfig {
        return config.firebaseAuthConfig
    }

    @Provides
    @GoogleServerClientId
    fun provideGoogleServerClientId(config: NotmidRuntimeConfig): String {
        return config.googleServerClientId
    }

    @Provides
    @Singleton
    @NotmidApi
    fun provideNotmidNetworkClient(
        @NotmidApi config: NotmidApiConfig,
    ): NotmidNetworkClient {
        return OkHttpNotmidNetworkClient(config)
    }

    @Provides
    @Singleton
    @FirebaseIdentityToolkit
    fun provideFirebaseIdentityToolkitNetworkClient(
        @FirebaseIdentityToolkit config: NotmidApiConfig,
    ): NotmidNetworkClient {
        return OkHttpNotmidNetworkClient(config)
    }

    @Provides
    @Singleton
    fun provideGoogleIdTokenProvider(
        @ApplicationContext context: Context,
        @GoogleServerClientId serverClientId: String,
    ): GoogleIdTokenProvider {
        return AndroidCredentialManagerGoogleIdTokenProvider(
            context = context,
            serverClientId = serverClientId,
        )
    }

    @Provides
    @Singleton
    fun provideFirebaseIdTokenProvider(
        @FirebaseIdentityToolkit client: NotmidNetworkClient,
        config: FirebaseAuthRestConfig,
        googleIdTokenProvider: GoogleIdTokenProvider,
    ): FirebaseIdTokenProvider {
        return if (config.isConfigured) {
            FirebaseAuthRestIdTokenProvider(
                client = client,
                config = config,
                googleIdTokenProvider = googleIdTokenProvider,
            )
        } else {
            UnavailableFirebaseIdTokenProvider()
        }
    }

    @Provides
    @Singleton
    fun provideNotmidAuthGateway(
        authMode: NotmidAuthMode,
        @NotmidApi client: NotmidNetworkClient,
        firebaseIdTokenProvider: FirebaseIdTokenProvider,
    ): NotmidAuthGateway {
        return when (authMode) {
            NotmidAuthMode.Firebase -> ApiVerifiedNotmidAuthGateway(
                client = client,
                idTokenProvider = firebaseIdTokenProvider,
            )

            NotmidAuthMode.Fake,
            NotmidAuthMode.Disabled,
            -> LocalNotmidAuthGateway(mode = authMode)
        }
    }

    @Provides
    @Singleton
    fun provideNotmidContentRepository(
        source: NotmidContentSource,
        @NotmidApi client: NotmidNetworkClient,
    ): NotmidContentRepository {
        return when (source) {
            NotmidContentSource.Static -> StaticNotmidContentRepository()
            NotmidContentSource.Api -> ApiNotmidContentRepository(client)
        }
    }

    @Provides
    @Singleton
    fun provideNotmidProtectedWriteRepository(
        source: NotmidContentSource,
        @NotmidApi client: NotmidNetworkClient,
        contentRepository: NotmidContentRepository,
    ): NotmidProtectedWriteRepository {
        return when (source) {
            NotmidContentSource.Static -> StaticNotmidProtectedWriteRepository(contentRepository)
            NotmidContentSource.Api -> ApiNotmidProtectedWriteRepository(client)
        }
    }
}
