package app.thdev.glassnavlab.di

import app.thdev.glassnavlab.core.navigation.runtime.RouteEventHandler
import app.thdev.glassnavlab.router.FeedRouteEventHandler
import app.thdev.glassnavlab.router.InboxRouteEventHandler
import app.thdev.glassnavlab.router.MapRouteEventHandler
import app.thdev.glassnavlab.router.NotmidRouteEventHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(ActivityRetainedComponent::class)
abstract class NotmidRouteEventHandlerModule {
    @Binds
    @IntoSet
    abstract fun bindNotmidRouteEventHandler(
        impl: NotmidRouteEventHandler,
    ): RouteEventHandler

    @Binds
    @IntoSet
    abstract fun bindFeedRouteEventHandler(
        impl: FeedRouteEventHandler,
    ): RouteEventHandler

    @Binds
    @IntoSet
    abstract fun bindMapRouteEventHandler(
        impl: MapRouteEventHandler,
    ): RouteEventHandler

    @Binds
    @IntoSet
    abstract fun bindInboxRouteEventHandler(
        impl: InboxRouteEventHandler,
    ): RouteEventHandler
}
