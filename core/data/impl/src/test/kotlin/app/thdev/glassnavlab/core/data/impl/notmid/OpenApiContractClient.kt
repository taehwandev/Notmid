package app.thdev.glassnavlab.core.data.impl.notmid

import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkClient
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkRequest
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkResponse
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

internal class OpenApiContractClient(
    val contract: OpenApiContractFixture = OpenApiContractFixture(),
    private val transform: (String, JsonObject) -> JsonObject = { _, response -> response },
) : NotmidNetworkClient {
    val operations = mutableListOf<String>()

    override suspend fun execute(request: NotmidNetworkRequest): NotmidNetworkResponse {
        val operation = contract.operation(request)
        contract.validateRequest(request, operation)
        val id = operation.getValue("operationId").jsonPrimitive.content
        operations += id
        return NotmidNetworkResponse(200, transform(id, contract.response(operation)).toString(), emptyMap())
    }
}
