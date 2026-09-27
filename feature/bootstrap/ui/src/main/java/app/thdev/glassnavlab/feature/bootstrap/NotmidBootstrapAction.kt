package app.thdev.glassnavlab.feature.bootstrap

internal sealed interface NotmidBootstrapAction {
    data object ReloadContent : NotmidBootstrapAction
}
