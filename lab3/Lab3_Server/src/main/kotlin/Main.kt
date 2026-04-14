package org.example

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.util.concurrent.Executors

val logger = KotlinLogging.logger {}
private var listening = true

fun main(args: Array<String>) {
    val port = if (args.isNotEmpty()) args[0].toInt() else 8000
    val concLevel = if (args.size > 1) args[1].toInt() else null
    val executor = if (concLevel != null) {
        Executors.newFixedThreadPool(concLevel)
    } else {
        Executors.newCachedThreadPool()
    }

    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info { "Closing server..." }
        executor.shutdown()
        listening = false
    })

    val server = ServerSocket(port)
    logger.info { "The main web server is running at localhost:$port" }

    while (listening) {
        val client = server.accept()
        executor.submit {
            try {
                val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                val request = HttpRequestMessage(reader)
                request.respond(client.getOutputStream())
            } catch (e: Exception) {
                e.printStackTrace()
                logger.error { e.message }
            } finally {
                client.close()
            }
        }
    }
}