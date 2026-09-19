package app.thdev.glassnavlab.di

import app.thdev.glassnavlab.core.navigation.runtime.RouteEventSink
import app.thdev.glassnavlab.core.runtime.router.runtime.AppRouterRuntime
import app.thdev.glassnavlab.feature.notmid.router.NotmidAppRouterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped

@Module
@InstallIn(ActivityRetainedComponent::class)
object NotmidNavigationModule {
    @Provides
    @ActivityRetainedScoped
    fun provideRouter(factory: NotmidAppRouterFactory): AppRouterRuntime = factory.createRuntime()

    @Provides
    fun provideRouteEventSink(router: AppRouterRuntime): RouteEventSink = router
}
