package org.example.routing

import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import org.example.data.MOCK_PRODUCTS

suspend fun ProductListGet(call: ApplicationCall) {
    call.respond(MOCK_PRODUCTS.toList())
}