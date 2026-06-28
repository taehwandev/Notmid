package app.thdev.glassnavlab.feature.notmid.di

import app.thdev.glassnavlab.core.router.runtime.RouteEventHandler
import app.thdev.glassnavlab.feature.notmid.router.FeedRouteEventHandler
import app.thdev.glassnavlab.feature.notmid.router.InboxRouteEventHandler
import app.thdev.glassnavlab.feature.notmid.router.MapRouteEventHandler
import app.thdev.glassnavlab.feature.notmid.router.NotmidRouteEventHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(ActivityComponent::class)
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
