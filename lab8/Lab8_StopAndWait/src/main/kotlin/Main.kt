package org.example

import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.DatagramSocket
import java.net.InetAddress

val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    val mode = args[0]

    testChecksum()

    when (mode) {
        "cl" -> {
            val filePath = args[1]
            val port = if (args.size >= 3) args[2].toInt() else 8000
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
        }
        "srv" -> {
            logger.info { "Starting StopAndWait server..." }
            val filePath = args[1]
            val port = if (args.size >= 3) args[2].toInt() else 8000
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
        "dup" -> {
            logger.info { "Starting StopAndWait duplex peer..." }
            val shouldSendFirst = args[6].toInt() > 0
            val localFileToSend = args[1]
            val remoteFileToSave = args[2]
            val localPort = if (args.size >= 4) args[3].toInt() else 8001
            val remoteAddr = if (args.size >= 5) InetAddress.getByName(args[4]) else InetAddress.getByName("localhost")
            val remotePort = if (args.size >= 6) args[5].toInt() else 8000

            val socket = LossySocket(DatagramSocket(localPort))
            val duplexPeer = DuplexPeer(socket)
            try {
                if (shouldSendFirst) {
                    duplexPeer.sendThenReceive(localFileToSend, remoteFileToSave, remoteAddr, remotePort)
                } else {
                    duplexPeer.receiveThenSend(remoteFileToSave, localFileToSend)
                }
            } catch (e: Exception) {
                logger.error(e) { "Duplex exchange failed" }
            } finally {
                socket.datagramSocket.close()
            }
        }
    }
}