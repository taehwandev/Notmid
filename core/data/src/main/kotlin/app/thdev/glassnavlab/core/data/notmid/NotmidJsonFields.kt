package app.thdev.glassnavlab.core.data.notmid

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Field accessors shared by every notmid API JSON mapper in this module.
 *
 * A missing or mistyped required field throws, so the calling repository can
 * translate one [RuntimeException] into its own malformed-response failure.
 */
internal fun JsonObject.requiredObject(name: String): JsonObject {
    return this[name]?.jsonObject ?: error("Missing object field: $name")
}

internal fun JsonObject.optionalObject(name: String): JsonObject? {
    return this[name]?.jsonObject
}

internal fun JsonObject.requiredArray(name: String): JsonArray {
    return this[name]?.jsonArray ?: error("Missing array field: $name")
}

internal fun JsonObject.requiredString(name: String): String {
    return this[name]?.jsonPrimitive?.contentOrNull ?: error("Missing string field: $name")
}

internal fun JsonObject.optionalString(name: String): String? {
    return this[name]?.jsonPrimitive?.contentOrNull
}

internal fun JsonObject.requiredInt(name: String): Int {
    return this[name]?.jsonPrimitive?.intOrNull ?: error("Missing int field: $name")
}

internal fun JsonObject.requiredDouble(name: String): Double {
    return this[name]?.jsonPrimitive?.doubleOrNull ?: error("Missing double field: $name")
}

internal fun JsonObject.requiredBoolean(name: String): Boolean {
    return this[name]?.jsonPrimitive?.booleanOrNull ?: error("Missing boolean field: $name")
}

internal fun JsonObject.requiredStringArray(name: String): List<String> {
    return requiredArray(name).map { element ->
        element.jsonPrimitive.content
    }
}

internal fun JsonObject.optionalStringArray(name: String): List<String> {
    return this[name]?.jsonArray?.map { element ->
        element.jsonPrimitive.content
    }.orEmpty()
}
