package app.thdev.glassnavlab

internal sealed interface NotmidBootstrapAction {
    data object ReloadContent : NotmidBootstrapAction
}
