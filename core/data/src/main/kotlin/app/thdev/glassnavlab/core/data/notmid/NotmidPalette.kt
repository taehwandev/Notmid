package app.thdev.glassnavlab.core.data.notmid

import app.thdev.glassnavlab.core.model.notmid.NotmidColor

/**
 * Deterministic visual values derived from a stable id.
 *
 * The notmid API does not send palettes or playback progress, so both the
 * content and protected-write repositories derive them from the same id to keep
 * a clip or place looking identical wherever it is hydrated.
 */
internal fun paletteForStableId(id: String): List<NotmidColor> {
    val hash = id.fold(0) { acc, char -> (acc * 31 + char.code).and(0x00FFFFFF) }
    val primary = 0xFF000000L or hash.toLong()
    val secondary = 0xFF000000L or hash.rotateColor(8).toLong()
    val tertiary = 0xFF000000L or hash.rotateColor(16).toLong()
    return listOf(
        NotmidColor(primary.ensureVisibleColor()),
        NotmidColor(secondary.ensureVisibleColor()),
        NotmidColor(tertiary.ensureVisibleColor()),
    )
}

internal fun stableProgressFor(id: String): Float {
    val bucket = id.fold(17) { acc, char -> acc + char.code }.mod(70)
    return (bucket + 20) / 100f
}

private fun Int.rotateColor(bits: Int): Int {
    return ((this shl bits) or (this ushr (24 - bits))).and(0x00FFFFFF)
}

private fun Long.ensureVisibleColor(): Long {
    val rgb = this and 0x00FFFFFF
    return if (rgb < 0x202020) {
        this or 0x004A4A4A
    } else {
        this
    }
}
