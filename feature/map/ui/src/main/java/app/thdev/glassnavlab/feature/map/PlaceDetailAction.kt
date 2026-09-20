package app.thdev.glassnavlab.feature.map

internal sealed interface PlaceDetailAction {
    data object Retry : PlaceDetailAction
}
