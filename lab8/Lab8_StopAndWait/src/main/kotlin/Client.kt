package org.example

import java.io.File
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.SocketTimeoutException

class Client(private val packetDataSize: Int = 1024, private val socket: LossySocket,
                private val address: InetAddress, private val port: Int,
                private val maxRetries: Int = 10, private val timeout: Int = 3000) {
    private var currentSeqNum = 0

    fun sendFile(path: String) {
        val file = File(path)

        file.forEachBlock(blockSize = packetDataSize) { buffer, bytesRead ->
            val data = buffer.copyOf(bytesRead)
            val isSent = sendPacket(data)
            if (!isSent) {
                throw IllegalStateException("Timeout limit exceeded, sending failed")
            }
            currentSeqNum = 1 - currentSeqNum
        }
        val isEotSent = sendEOT()
        if (!isEotSent) {
            throw IllegalStateException("Timeout limit for EOT exceeded, file sent but server is unresponsive")
        }
    }

    fun sendPacket(data: ByteArray): Boolean {
        logger.info { "Preparing a data packet..." }
        val packet = serializePacket(newPacket(
            currentSeqNum,
            data,
            isAck = false,
            ackNum = null,
            isEot = false
        ))

        return sendWithRetry(packet)
    }

    private fun sendWithRetry(packet: ByteArray): Boolean {
        logger.info { "Beginning packing send..." }
        var retries = 0
        while (retries < maxRetries) {
            try {
                socket.send(packet, address, port)

                socket.datagramSocket.soTimeout = timeout
                val buffer = ByteArray(2048)
                val datagram = DatagramPacket(buffer, buffer.size)
                socket.datagramSocket.receive(datagram)

                val ack = deserializePacket(datagram.data.copyOf(datagram.length))

                if (ack.isAck && ack.ackNum == currentSeqNum) {
                    logger.info { "Got correct ACK" }
                    return true
                } else {
                    logger.info { "Got wrong ACK number, expected $currentSeqNum" }
                }
            } catch (e: SocketTimeoutException) {
                logger.info { "Timeout, retry ${retries + 1}/$maxRetries" }
                retries++
            }
        }
        return false
    }

    private fun sendEOT(): Boolean {
        logger.info { "Finished with data, sending an EOT" }
        val seqNum = currentSeqNum
        val eot = serializePacket(newPacket(
            seqNum = seqNum,
            data = ByteArray(0),
            isAck = false,
            ackNum = null,
            isEot = true
        ))

        return sendWithRetry(eot)
    }
}