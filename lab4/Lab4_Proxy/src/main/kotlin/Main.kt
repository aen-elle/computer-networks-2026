package org.example

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpStatusCode
import io.ktor.server.engine.EngineConnectorBuilder
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.uri
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.network.tls.certificates.buildKeyStore
import io.ktor.network.tls.certificates.saveToFile
import io.ktor.server.application.log
import io.ktor.server.engine.sslConnector
import io.ktor.server.request.receive
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.example.cache.Cache
import java.io.File
import java.io.FileInputStream
import java.security.KeyStore

private val logger = KotlinLogging.logger { }

fun checkAndCreateCert(path: String): KeyStore {
    if (File(path).exists()) {
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        FileInputStream(path).use {
            try {
                keyStore.load(it, "test".toCharArray())
            } catch(e: Exception) {
                logger.error { e.message }
            }
        }
        return keyStore
    } else {
        val keyStoreFile = File(path)
        val keyStore = buildKeyStore {
            certificate("lab4Keystore") {
                password = "test"
                domains = listOf("127.0.0.1", "0.0.0.0", "localhost")
            }
        }
        keyStore.saveToFile(keyStoreFile, "test")
        return keyStore
    }
}


suspend fun forwardResponse(call: RoutingCall, origResponse: HttpResponse) {
    call.response.status(origResponse.status)
    origResponse.headers.forEach { key, values ->
        if (key !in listOf("Host", "Transfer-Encoding", "Connection", "Content-Length")) {
            call.response.header(key, values.joinToString(","))
        }
    }
    call.respondBytes(origResponse.bodyAsBytes())
}

fun main() {

    val keyStore = checkAndCreateCert("build/keystore.jks")

    val cache: Cache = Cache.getInstance()

    embeddedServer(Netty, configure = {
        connectors.add(EngineConnectorBuilder().apply {
            host = "localhost"
            port = 8082
            sslConnector(
                keyStore = keyStore,
                keyAlias = "lab4Keystore",
                keyStorePassword = { "test".toCharArray() },
                privateKeyPassword = { "test".toCharArray() }
            ) {
                port = 8443
                host = "localhost"
            }
        })
    }) {
        val httpClient = HttpClient()
        routing {
            get("{...}") {
                val origPath = call.request.uri.drop(1)
                logger.debug { "Trying to get $origPath" }
                if (isForbidden(origPath)) {
                    call.respond(HttpStatusCode.Forbidden, "The domain is in the forbidden domains list.")
                }

                val cached = cache.load(origPath)

                try {
                    val response: HttpResponse = httpClient.get(origPath) {
                        call.request.headers.forEach { key, values ->
                            if (key !in listOf("Host", "Transfer-Encoding", "Connection", "Content-Length")) {
                                header(key, values.joinToString(","))
                            }
                        }
                        if (cached != null) {
                            logger.debug { "Cache hit!" }
                            cached.metaData.lastModified.let { header("If-Modified-Since", it) }
                            cached.metaData.etag.let { header("If-None-Match", it) }
                        }
                    }

                    when (response.status) {
                        HttpStatusCode.NotModified -> {
                            logger.debug { "Getting from cache..." }
                            if (cached != null) {
                                cached.metaData.contentType?.let { call.response.header("Content-Type", it) }
                                cached.metaData.contentEncoding?.let { call.response.header("Content-Encoding", it) }
                                call.respondBytes(cached.bytes)
                            } else {
                                call.respond(HttpStatusCode.NotFound, "Cache error")
                            }
                        }
                        HttpStatusCode.OK -> {
                            val body = response.bodyAsBytes()
                            val lastModified = response.headers["Last-Modified"]
                            val etag = response.headers["ETag"]
                            val contentType = response.headers["Content-Type"]
                            val contentEncoding = response.headers["Content-Encoding"]
                            logger.debug { "Saving to cache..." }
                            cache.save(origPath, body, lastModified, etag, contentType, contentEncoding)
                            forwardResponse(call, response)
                        }
                        else -> {
                            call.response.status(response.status)
                            call.respondBytes(response.bodyAsBytes())
                        }
                    }
                } catch(e: Exception) {
                    call.respond(HttpStatusCode.BadGateway,
                        "Proxy error: ${e.message}")
                }
            }

            post("{...}") {
                val origPath = call.request.uri.drop(1)
                if (isForbidden(origPath)) {
                    call.respond(HttpStatusCode.Forbidden, "The domain is in the forbidden domains list.")
                }

                try {
                    val response: HttpResponse = httpClient.post(origPath) {
                        call.request.headers.forEach { key, values ->
                            if (key !in listOf("Host", "Transfer-Encoding", "Connection", "Content-Length", "Content-Disposition", "Content-Encoding")) {
                                header(key, values.joinToString(","))
                            }
                        }
                        setBody(call.receive<ByteArray>())
                    }
                    forwardResponse(call, response)
                } catch(e: Exception) {
                    call.respond(HttpStatusCode.BadGateway,
                        "Proxy error: ${e.message}")
                }
            }
        }
    }.start(wait = true)
}

private fun isForbidden(origPath: String): Boolean {
    val jsonString = File("config.json").readText()
    val config = Json.decodeFromString<ProxyConfig>(jsonString)
    config.blocked_domains.forEach {
        if (origPath.startsWith(it) || origPath == it) {
            return true
        }
    }
    return false
}

@Serializable
data class ProxyConfig(
    val blocked_domains: List<String>,
)