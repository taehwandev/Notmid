package app.thdev.glassnavlab.feature.webview.di

import app.thdev.glassnavlab.core.runtime.router.activity.ActivityRouteLaunchHandler
import app.thdev.glassnavlab.feature.webview.WebViewActivityRouteLaunchHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(ActivityComponent::class)
abstract class WebViewActivityRouteModule {
    @Binds
    @IntoSet
    abstract fun bindWebViewActivityRouteLaunchHandler(
        impl: WebViewActivityRouteLaunchHandler,
    ): ActivityRouteLaunchHandler
}
