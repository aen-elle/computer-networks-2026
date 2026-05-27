import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

val logger = KotlinLogging.logger {  }

fun main() {
    val serverAddress = InetAddress.getByName("localhost")
    val serverPort = 8888
    val port = 8889

    val socket = DatagramSocket(port)
    socket.soTimeout = 1000

    var lossCount = 0
    val rttList = mutableListOf<Double>()

    logger.info { "Client is running on $port..." }

    for (seq in 1..10) {
        val sendTime = System.currentTimeMillis()
        val message = "Ping $seq $sendTime".toByteArray()
        val packet = DatagramPacket(message, message.size, serverAddress, serverPort)
        socket.send(packet)

        try {
            val receiveBuffer = ByteArray(1200)
            val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
            socket.receive(receivePacket)
            val receiveTime = System.currentTimeMillis()
            val rtt = (receiveTime - sendTime) / 1000.0
            rttList.add(rtt)
            val receivedMessage = String(receivePacket.data, 0, receivePacket.length)
            logger.info { "Received: $receivedMessage, RTT = $rtt sec" }
        } catch (e: SocketTimeoutException) {
            logger.info { "Request timed out" }
            lossCount++
        }
    }

    socket.close()

    val lossPercent = (lossCount * 100.0 / 10)
    logger.info { "--- Ping statistics ---" }
    logger.info { "10 packets transmitted, ${10 - lossCount} received, ${String.format("%.1f", lossPercent)}% packet loss" }

    if (rttList.isNotEmpty()) {
        val minRtt = rttList.minOrNull()!!
        val maxRtt = rttList.maxOrNull()!!
        val avgRtt = rttList.average()
        logger.info { "round-trip min/avg/max = ${String.format("%.3f", minRtt)}/${String.format("%.3f", avgRtt)}/${String.format("%.3f", maxRtt)} sec" }
    } else {
        logger.info { "No successful responses" }
    }
}