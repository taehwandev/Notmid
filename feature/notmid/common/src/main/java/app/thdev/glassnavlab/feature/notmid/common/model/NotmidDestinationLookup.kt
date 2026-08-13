package app.thdev.glassnavlab.feature.notmid.common.model

fun destinationFor(
    destinations: List<NotmidDestination>,
    selectedItemId: String,
): NotmidDestination {
    return destinations.firstOrNull { it.id == selectedItemId } ?: destinations.first()
}
