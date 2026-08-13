package app.thdev.glassnavlab.core.model.notmid

/**
 * Derives a stable, URL-safe id from a human title so models can default their
 * own route id without a caller supplying one.
 */
internal fun String.toStableRouteId(): String {
    return trim()
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .ifBlank { "item" }
}
