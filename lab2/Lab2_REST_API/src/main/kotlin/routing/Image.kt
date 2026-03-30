package org.example.routing

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import org.example.data.MOCK_PRODUCTS
import java.io.File
import java.util.UUID

suspend fun ImagePost(call: ApplicationCall) {
    val productId = getAndVerifyId(call)
    val picId = UUID.randomUUID().toString()
    val picData: ByteArray = call.receive<ByteArray>()

    val file = File("/uploads/$picId")
    file.parentFile.mkdirs()

    if (picData.isEmpty()) {
        throw IllegalArgumentException("Upload went wrong")
    }
    file.writeBytes(picData)

    MOCK_PRODUCTS.find { product -> product.id == productId }!!.icon = picId
    call.respond(HttpStatusCode.Accepted)
}

suspend fun ImageGet(call: ApplicationCall) {
    val productId = getAndVerifyId(call)

    val picId = MOCK_PRODUCTS.find { it.id == productId }!!.icon
    if (picId == null) {
        throw NoSuchElementException("No image is associated with product $productId")
    }

    val pic = File("/uploads/$picId")
    if (pic.exists() && pic.isFile) {
        val contentType = ContentType.Image.PNG
        call.response.header(HttpHeaders.ContentType, contentType.toString())
        call.respondFile(pic)
    } else {
        throw IllegalStateException("Image for product $productId must exist but does not or is corrupted.")
    }
}