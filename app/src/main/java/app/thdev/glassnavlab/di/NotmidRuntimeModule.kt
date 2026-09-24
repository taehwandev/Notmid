package app.thdev.glassnavlab.di

import android.content.Context
import app.thdev.glassnavlab.core.auth.android.AndroidCredentialManagerGoogleIdTokenProvider
import app.thdev.glassnavlab.config.NotmidRuntimeConfig
import app.thdev.glassnavlab.config.notmidRuntimeConfigFromBuildConfig
import app.thdev.glassnavlab.core.auth.impl.ApiVerifiedNotmidAuthGateway
import app.thdev.glassnavlab.core.auth.impl.FirebaseAuthRestConfig
import app.thdev.glassnavlab.core.auth.impl.FirebaseAuthRestIdTokenProvider
import app.thdev.glassnavlab.core.auth.impl.LocalNotmidAuthGateway
import app.thdev.glassnavlab.core.auth.impl.UnavailableFirebaseIdTokenProvider
import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthGateway
import app.thdev.glassnavlab.core.auth.token.FirebaseIdTokenProvider
import app.thdev.glassnavlab.core.auth.token.GoogleIdTokenProvider
import app.thdev.glassnavlab.core.data.api.notmid.NotmidContentSource
import app.thdev.glassnavlab.core.data.impl.notmid.ObservableNotmidContentRepository
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentRepository
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentUpdates
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthMode
import app.thdev.glassnavlab.core.network.impl.OkHttpNotmidNetworkClient
import app.thdev.glassnavlab.core.network.notmid.NotmidApiConfig
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkClient
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
    fun provideNotmidContentRepository(
        repository: ObservableNotmidContentRepository,
    ): NotmidContentRepository = repository

    @Provides
    fun provideContentUpdates(
        repository: ObservableNotmidContentRepository,
    ): NotmidContentUpdates = repository

}
