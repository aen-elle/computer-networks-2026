package org.example

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.random.Random

class LossySocket(val datagramSocket: DatagramSocket,
    private val lossChance: Double = 0.3) {

    fun send(packet: ByteArray, address: InetAddress, port: Int) {
        if (Random.nextDouble() >= lossChance) {
            datagramSocket.send(DatagramPacket(packet, packet.size, address, port))
        } else {
            logger.info { "We lost a packet!" }
        }
    }
}