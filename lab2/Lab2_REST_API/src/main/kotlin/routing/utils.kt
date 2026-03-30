package org.example.routing

import io.ktor.server.application.ApplicationCall
import kotlin.text.isEmpty

fun getAndVerifyId(call: ApplicationCall): String {
    val id = call.parameters["productId"]

    if (id == null || id.isEmpty()) {
        throw IllegalArgumentException("No product id")
    }
    return id
}