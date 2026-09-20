package app.thdev.glassnavlab.feature.map

import androidx.compose.runtime.Immutable
import app.thdev.glassnavlab.core.model.notmid.NotmidGeoPoint
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidPlace

internal val DefaultMapCategories = listOf("All", "Cafe", "Work", "Night", "Exhibit", "Walk")

@Immutable
internal data class MapPinUi(val place: NotmidPlace, val category: String, val xFraction: Float, val yFraction: Float)

internal fun NotmidPlace.toMapPin(index: Int): MapPinUi {
    val coordinates = coordinate?.toMapFractions()
        ?: MapPinCoordinates[index % MapPinCoordinates.size]
    return MapPinUi(
        place = this,
        category = category.ifBlank {
            DefaultMapCategories[(index % (DefaultMapCategories.size - 1)) + 1]
        },
        xFraction = coordinates.first,
        yFraction = coordinates.second,
    )
}

private val MapPinCoordinates = listOf(
    0.26f to 0.24f,
    0.62f to 0.34f,
    0.42f to 0.64f,
    0.78f to 0.70f,
    0.18f to 0.76f,
    0.74f to 0.18f,
)

private fun NotmidGeoPoint.toMapFractions(): Pair<Float, Float> {
    val minLatitude = 37.50
    val maxLatitude = 37.59
    val minLongitude = 126.88
    val maxLongitude = 127.08
    val x = ((longitude - minLongitude) / (maxLongitude - minLongitude)).toFloat()
    val y = (1f - ((latitude - minLatitude) / (maxLatitude - minLatitude)).toFloat())

    return x.coerceIn(0.12f, 0.86f) to y.coerceIn(0.12f, 0.86f)
}
