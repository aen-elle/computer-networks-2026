    package org.example

    import java.io.FileOutputStream
    import java.lang.Exception
    import java.net.DatagramPacket
    import java.net.InetAddress
    import kotlin.math.log

    class Server(private val bufferSize: Int, private val socket: LossySocket) {
        private var expectedSeqNum = 0

        fun receiveFile(path: String) {
            val outputStream = FileOutputStream(path)

            try {
                while (true) {
                    val packet = receivePacket()
                    if (packet == null) {
                        logger.info { "Packet was not received correctly, awaiting for a resend"}
                        continue
                    } else {
                        logger.info { "The packet is correct, writing it to file..." }
                        if (packet.isEot) {
                            logger.info { "Received an EOT, ending file transmission" }
                            break
                        }
                        if (packet.data.isNotEmpty()) {
                            outputStream.write(packet.data)
                            outputStream.flush()
                        }
                    }

                }
            } catch (e: Exception) {
                logger.error { e.message }
            }
            finally {
                logger.info { "File transmission finished" }
                outputStream.close()
            }
        }

        private fun sendAck(address: InetAddress, port: Int, seqNum: Int) {
            logger.info { "Sending ack to seqnum $seqNum" }
            val ackPacket = serializePacket(makeAck(seqNum))
            socket.send(ackPacket, address, port)
        }

        private fun receivePacket(): Packet? {
            val buffer = ByteArray(bufferSize)
            val datagram = DatagramPacket(buffer, buffer.size)

            socket.datagramSocket.receive(datagram)

            val packet = deserializePacket(datagram.data.copyOf(datagram.length))

            val packetWithoutChecksum = packet.copy(checksum = 0)

            if (verifyChecksum(serializePacket(packetWithoutChecksum), packet.checksum)) {
                if (expectedSeqNum == packet.seqNum) {
                    if (packet.isEot) {
                        logger.info { "We received an EOT" }
                    }
                    logger.info { "Valid checksum and expected seqnum, sending ACK" }
                    sendAck(datagram.address, datagram.port, expectedSeqNum)
                    expectedSeqNum = 1 - expectedSeqNum
                    return packet
                } else {
                    logger.info { "Unexpected seqnum, but checksum is valid, sending an ACK to previous packet" }
                    sendAck(datagram.address, datagram.port, 1 - expectedSeqNum)
                }
            }
            logger.info { "Checksum is invalid" }
            logger.info {
                """
                        There is something wrong with the packet.
                        Packet data checksum = ${calcCheckSum(serializePacket(packetWithoutChecksum))}, received checksum = ${packet.checksum}
                        Comparison result = ${verifyChecksum(serializePacket(packetWithoutChecksum), packet.checksum)}
                        Expected seq num = $expectedSeqNum, received seq num = ${packet.seqNum}
                    """.trimIndent()
            }
            return null
        }
    }