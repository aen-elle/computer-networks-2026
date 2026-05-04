package org.example

import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.DatagramSocket
import java.net.InetAddress

val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    val isClient = args[0] == "cl"
    val filePath = args[1]
    val port = if (args.size >= 3) args[2].toInt() else 8000
    logger.info { "${isClient} = ${args[0]}, $filePath, $port" }

    if (isClient) {
        val serverAddress = if (args.size >= 4) {
            InetAddress.getByName(args[3])
        } else {
            InetAddress.getByName("localhost")
        }

        val serverPort = if (args.size >= 5) args[4].toInt() else 8000
        logger.info { "Starting StopAndWait client..." }

        val udpSocket = LossySocket(DatagramSocket(port))
        logger.info { "Running on $port port" }

        val client = Client(socket = udpSocket, address = serverAddress, port = serverPort)
        try {
            client.sendFile(filePath)
            logger.info { "File sent successfully!" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to send file: ${e.message}" }
        } finally {
            udpSocket.datagramSocket.close()
        }

    } else {
        logger.info { "Starting StopAndWait server..." }

        val udpSocket = LossySocket(DatagramSocket(port))
        logger.info { "Running on $port port" }

        val bufSize = 2048
        val srv = Server(bufSize, udpSocket)
        try {
            srv.receiveFile(filePath)
            logger.info { "File received successfully!" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to receive file: ${e.message}" }
        } finally {
            udpSocket.datagramSocket.close()
        }
    }
}