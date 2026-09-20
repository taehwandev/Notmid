package app.thdev.glassnavlab.feature.map

internal sealed interface MapAction {
    data class CategorySelected(val category: String) : MapAction
    data class PinSelected(val placeId: String) : MapAction
    data object OpenSelectedPlace : MapAction
    data object Retry : MapAction
}
