import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.random.Random

val logger = KotlinLogging.logger {  }

fun main() {
    val port = 8888
    val socket = LossySocket(DatagramSocket(port))
    val buf = ByteArray(1200)

    logger.info { "Server is running on $port..." }
    while (true) {
        val packet = DatagramPacket(buf, buf.size)
        socket.receive(packet)

        val rec = String(packet.data, 0, packet.length)
        logger.info { "Received a packet!" }
        logger.info { "Attempting response: ${rec.uppercase()}" }
        socket.send(rec.uppercase().toByteArray(), packet.address, packet.port)
    }
}


class LossySocket(val datagramSocket: DatagramSocket,
                  private val lossChance: Double = 0.2) {

    fun send(packet: ByteArray, address: InetAddress, port: Int) {
        if (Random.nextDouble() >= lossChance) {
            datagramSocket.send(DatagramPacket(packet, packet.size, address, port))
        } else {
            logger.info { "We lost a packet!" }
        }
    }

    fun receive(packet: DatagramPacket) = datagramSocket.receive(packet)
}