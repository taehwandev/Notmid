package app.thdev.glassnavlab.core.data.impl.notmid

import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Small fixture reader for the pinned schema subset, not a general OpenAPI validator.
 * Samples include every declared property; the snapshot does not declare required fields.
 */
internal class OpenApiContractFixture {
    private val document = checkNotNull(javaClass.getResourceAsStream("/notmid-openapi.json")) {
        "Pinned Android contract resource missing"
    }.bufferedReader().use { Json.parseToJsonElement(it.readText()).jsonObject }

    fun schema(name: String): JsonObject = document.getValue("components").jsonObject
        .getValue("schemas").jsonObject.getValue(name).jsonObject

    fun resolve(schema: JsonObject): JsonObject = schema["\$ref"]?.let {
        this.schema(it.jsonPrimitive.content.substringAfterLast('/'))
    } ?: schema

    fun sample(schema: JsonObject): JsonElement {
        val resolved = resolve(schema)
        resolved["const"]?.let { return it }
        resolved["enum"]?.let { return it.jsonArray.first() }
        resolved["oneOf"]?.let { return sample(it.jsonArray.first().jsonObject) }
        return when (resolved.getValue("type").jsonPrimitive.content) {
            "object" -> JsonObject(resolved.getValue("properties").jsonObject
                .mapValues { (_, property) -> sample(property.jsonObject) })
            "array" -> JsonArray(listOf(sample(resolved.getValue("items").jsonObject)))
            "string" -> JsonPrimitive("contract-value")
            "number", "integer" -> JsonPrimitive(1)
            "boolean" -> JsonPrimitive(true)
            else -> error("Unsupported fixture schema: $resolved")
        }
    }

    fun operation(request: NotmidNetworkRequest): JsonObject {
        val paths = document.getValue("paths").jsonObject
        val path = paths.keys.singleOrNull { template ->
            val expected = template.split('/')
            val actual = request.path.split('/')
            expected.size == actual.size && expected.zip(actual).all { (left, right) ->
                if (left.startsWith('{')) right.isNotEmpty() else left == right
            }
        } ?: error("Undocumented request path: ${request.path}")
        return paths.getValue(path).jsonObject[request.method.name.lowercase()]?.jsonObject
            ?: error("Undocumented method: ${request.method} $path")
    }

    fun response(operation: JsonObject): JsonObject = sample(operation.getValue("responses")
        .jsonObject.getValue("200").jsonObject.jsonSchema()).jsonObject

    fun validateRequest(request: NotmidNetworkRequest, operation: JsonObject) {
        operation["requestBody"]?.let {
            validate(Json.parseToJsonElement(checkNotNull(request.body)), it.jsonObject.jsonSchema())
        }
        // Empty POST bodies are used by endpoints with no documented request body.
        if (operation["requestBody"] == null && request.body != null) {
            check(Json.parseToJsonElement(checkNotNull(request.body)) == JsonObject(emptyMap()))
        }
        val security = operation["security"]?.jsonArray
        val requiresBearer = !security.isNullOrEmpty() && security.all { "bearerAuth" in it.jsonObject }
        val authorization = request.headers["authorization"]
        if (requiresBearer || authorization != null) {
            check(authorization == "Bearer contract-token")
        }
    }

    /** Checks declared field types/enums and rejects undocumented client fields.
     * Does not invent requiredness, format, range, or business constraints absent in the snapshot.
     */
    fun validate(value: JsonElement, schema: JsonObject) {
        val resolved = resolve(schema)
        resolved["const"]?.let { check(value == it); return }
        resolved["enum"]?.let { check(value in it.jsonArray); return }
        resolved["oneOf"]?.let { choices ->
            check(choices.jsonArray.count { runCatching { validate(value, it.jsonObject) }.isSuccess } == 1)
            return
        }
        when (resolved.getValue("type").jsonPrimitive.content) {
            "object" -> {
                val properties = resolved.getValue("properties").jsonObject
                val fields = value.jsonObject
                check(fields.keys.all { it in properties }) { "Undocumented client fields: ${fields.keys - properties.keys}" }
                resolved["required"]?.jsonArray?.forEach { check(it.jsonPrimitive.content in fields) }
                fields.forEach { (key, item) -> validate(item, properties.getValue(key).jsonObject) }
            }
            "array" -> value.jsonArray.forEach { validate(it, resolved.getValue("items").jsonObject) }
            "string" -> check(value.jsonPrimitive.isString)
            "number", "integer" -> check(!value.jsonPrimitive.isString && value.jsonPrimitive.doubleOrNull != null)
            "boolean" -> check(!value.jsonPrimitive.isString && value.jsonPrimitive.booleanOrNull != null)
            else -> error("Unsupported schema: $resolved")
        }
    }

    private fun JsonObject.jsonSchema(): JsonObject = getValue("content").jsonObject
        .getValue("application/json").jsonObject.getValue("schema").jsonObject
}

internal fun JsonObject.replacing(vararg path: String, value: JsonElement?): JsonObject {
    val key = path.first()
    val replacement = if (path.size == 1) value else getValue(key).jsonObject
        .replacing(*path.drop(1).toTypedArray(), value = value)
    return JsonObject(toMutableMap().apply { if (replacement == null) remove(key) else put(key, replacement) })
}
