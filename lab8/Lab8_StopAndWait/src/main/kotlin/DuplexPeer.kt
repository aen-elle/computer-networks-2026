package org.example

import java.io.File
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.io.FileOutputStream

class DuplexPeer(
    private val socket: LossySocket,
    private val maxRetries: Int = 10,
    private val timeout: Int = 3000,
    private val receiveTimeout: Int = 60000,
    private val packetDataSize: Int = 1024
) {
    fun sendThenReceive(
        fileToSend: String,
        fileToReceive: String,
        remoteAddr: InetAddress,
        remotePort: Int
    ) {
        logger.info { "Starting duplex: send first, then receive" }
        sendFile(fileToSend, remoteAddr, remotePort)
        logger.info { "Send completed, now waiting to receive file" }
        val sender = receiveFile(fileToReceive)
        if (sender != null) {
            logger.info { "Receive completed from ${sender.first}:${sender.second}" }
        } else {
            logger.warn { "No file received" }
        }
    }

    fun receiveThenSend(
        fileToReceive: String,
        fileToSend: String,
    ) {
        logger.info { "Starting duplex: receive first, then send" }
        val sender = receiveFile(fileToReceive)
        if (sender != null) {
            logger.info { "Receive completed from ${sender.first}:${sender.second}" }
            sendFile(fileToSend, sender.first, sender.second)
            logger.info { "Send completed" }
        } else {
            logger.warn { "No file received, skipping send" }
        }
    }


    private fun sendFile(path: String, address: InetAddress, port: Int) {
        val file = File(path)
        var currentSeqNum = 0

        file.forEachBlock(blockSize = packetDataSize) { buffer, bytesRead ->
            val data = buffer.copyOf(bytesRead)
            sendPacketWithRetry(currentSeqNum, data, address, port)
            currentSeqNum = 1 - currentSeqNum
        }
        sendPacketWithRetry(currentSeqNum, ByteArray(0), address, port, isEot = true)
        logger.info { "File $path sent successfully to $address:$port" }
    }

    private fun receiveFile(path: String): Pair<InetAddress, Int>? {
        val outputStream = FileOutputStream(path)
        var expectedSeqNum = 0
        var clientAddr: InetAddress? = null
        var clientPort: Int? = null

        try {
            while (true) {
                val result = receivePacketWithAck(expectedSeqNum)
                    ?: continue
                val (packet, senderAddr, senderPort) = result

                if (clientAddr == null) {
                    clientAddr = senderAddr
                    clientPort = senderPort
                } else if (senderAddr != clientAddr || senderPort != clientPort) {
                    logger.info { "Ignoring packet from unknown source ${senderAddr}:$senderPort" }
                    continue
                }

                if (packet.isEot) {
                    logger.info { "EOT received, finishing reception" }
                    break
                }
                if (packet.data.isNotEmpty()) {
                    outputStream.write(packet.data)
                    outputStream.flush()
                }
                expectedSeqNum = 1 - expectedSeqNum
            }
        } finally {
            outputStream.close()
        }

        return Pair(clientAddr, clientPort)
    }

    private fun sendPacketWithRetry(seqNum: Int, data: ByteArray, address: InetAddress, port: Int, isEot: Boolean = false) {
        val packetBytes = serializePacket(newPacket(seqNum, data, isAck = false, ackNum = null, isEot = isEot))
        var retries = 0
        while (retries < maxRetries) {
            try {
                socket.send(packetBytes, address, port)
                socket.datagramSocket.soTimeout = timeout
                val buffer = ByteArray(2048)
                val datagram = DatagramPacket(buffer, buffer.size)
                socket.datagramSocket.receive(datagram)
                val ack = deserializePacket(datagram.data.copyOf(datagram.length))
                if (ack.isAck && ack.ackNum == seqNum) {
                    logger.info { "ACK received for seqNum=$seqNum" }
                    return
                } else {
                    logger.info { "Wrong ACK, expected $seqNum, got ${ack.ackNum}" }
                }
            } catch (e: SocketTimeoutException) {
                logger.info { "Timeout, retry ${retries + 1}/$maxRetries" }
                retries++
            }
        }
        throw IllegalStateException("Failed to send packet with seqNum=$seqNum after $maxRetries retries")
    }

    private fun receivePacketWithAck(expectedSeqNum: Int): Triple<Packet, InetAddress, Int>? {
        val previousTimeout = socket.datagramSocket.soTimeout
        socket.datagramSocket.soTimeout = receiveTimeout
        val buffer = ByteArray(2048)
        val datagram = DatagramPacket(buffer, buffer.size)

        return try {
            socket.datagramSocket.receive(datagram)
            val packet = deserializePacket(datagram.data.copyOf(datagram.length))
            val senderAddr = datagram.address
            val senderPort = datagram.port

            val packetWithoutChecksum = packet.copy(checksum = 0)
            if (!verifyChecksum(serializePacket(packetWithoutChecksum), packet.checksum)) {
                logger.info { "Checksum mismatch, dropping packet from $senderAddr:$senderPort" }
                return null
            }

            if (packet.seqNum == expectedSeqNum) {
                sendAck(expectedSeqNum, senderAddr, senderPort)
                logger.info { "Valid packet with seqNum=$expectedSeqNum, ACK sent" }
                Triple(packet, senderAddr, senderPort)
            } else {
                val ackNum = 1 - expectedSeqNum
                sendAck(ackNum, senderAddr, senderPort)
                logger.info { "Unexpected seqNum=${packet.seqNum}, sent ACK for $ackNum, packet ignored" }
                null
            }
        } finally {
            socket.datagramSocket.soTimeout = previousTimeout
        }
    }

    private fun sendAck(seqNum: Int, address: InetAddress, port: Int) {
        val ackPacket = serializePacket(makeAck(seqNum))
        socket.send(ackPacket, address, port)
    }
}