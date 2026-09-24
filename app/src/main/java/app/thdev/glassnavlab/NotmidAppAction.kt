package app.thdev.glassnavlab

internal sealed interface NotmidAppAction {
    data object ReloadContent : NotmidAppAction
}
