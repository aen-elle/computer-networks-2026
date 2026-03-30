package org.example.routing

import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import org.example.data.MOCK_PRODUCTS
import org.example.data.Product
import org.example.data.ProductPostRequest
import org.example.data.ProductPutRequest
import java.util.UUID

suspend fun ProductGet(call: ApplicationCall) {
    val id = getAndVerifyId(call)

    val product = MOCK_PRODUCTS.find { it.id == id }
    if (product == null) {
        throw NoSuchElementException("No product with that id has been found")
    }

    call.respond(product)
}

suspend fun ProductPost(call: ApplicationCall) {
    val product = call.receive<ProductPostRequest>()
    val newProduct = Product(
        id = UUID.randomUUID().toString(),
        name = product.name,
        description = product.description,
    )

    MOCK_PRODUCTS.add(newProduct)
    call.respond(newProduct)
}

suspend fun ProductPut(call: ApplicationCall) {
    val id = getAndVerifyId(call)

    if (MOCK_PRODUCTS.any { it.id == id }) {
        val productRequest = call.receive<ProductPutRequest>()
        val index = MOCK_PRODUCTS.indexOfFirst { it.id == id }
        MOCK_PRODUCTS[index] = MOCK_PRODUCTS[index].copy(
            id = productRequest.id ?: MOCK_PRODUCTS[index].id,
            name = productRequest.name ?: MOCK_PRODUCTS[index].name,
            description = productRequest.description ?: MOCK_PRODUCTS[index].description,
            icon = MOCK_PRODUCTS[index].icon
        )
        call.respond(MOCK_PRODUCTS[index])
    } else {
        throw NoSuchElementException("No product with that id has been found")
    }

}

suspend fun ProductDelete(call: ApplicationCall) {
    val id = getAndVerifyId(call)

    if (MOCK_PRODUCTS.any { it.id == id }) {
        val index = MOCK_PRODUCTS.indexOfFirst { it.id == id }
        call.respond(MOCK_PRODUCTS.removeAt(index))
    } else {
        throw NoSuchElementException("No product with that id has been found")
    }
}