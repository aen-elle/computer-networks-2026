package org.example.routing

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.request.contentType
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.response.*
import io.ktor.server.routing.contentType
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.util.reflect.TypeInfo
import java.lang.IllegalArgumentException

fun Route.mainRoutes() {
    route("/product") {
        post {
            try {
                ProductPost(call)
            } catch (e: ContentTransformationException) {
                call.respond(HttpStatusCode.BadRequest, "${e.message}")
            }
        }
    }

    route("/product/{productId}") {
        get {
            try {
                ProductGet(call)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, "${e.message}")
                return@get
            } catch (e: NoSuchElementException) {
                call.respond(HttpStatusCode.NotFound, "${e.message}")
                return@get
            }
        }
        put {
            try {
                ProductPut(call)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, "${e.message}")
                return@put
            } catch (e: NoSuchElementException) {
                call.respond(HttpStatusCode.NotFound, "${e.message}")
                return@put
            }
        }
        delete {
            try {
                ProductDelete(call)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, "${e.message}")
                return@delete
            } catch (e: NoSuchElementException) {
                call.respond(HttpStatusCode.NotFound, "${e.message}")
                return@delete
            }
        }

        route("/image") {
            post {
                val contentType = call.request.contentType()
                if (!contentType.match(ContentType.Image.PNG)) {
                    call.respond(HttpStatusCode.UnsupportedMediaType, "Expected multipart/form-data")
                    return@post
                }

                try {
                    ImagePost(call)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, "${e.message}")
                    return@post
                } catch (e: IllegalStateException) {
                    call.respond(HttpStatusCode.BadGateway, "${e.message}")
                    return@post
                }
            }

            get {
                try {
                    ImageGet(call)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, "${e.message}")
                    return@get
                } catch (e: RuntimeException) {
                    call.respond(HttpStatusCode.BadGateway, "${e.message}")
                }
            }
        }
    }

    route("/products") {
        get {
            ProductListGet(call)
        }
    }
}