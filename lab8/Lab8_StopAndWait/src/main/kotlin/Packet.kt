package org.example

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

data class Packet(
    val seqNum: Int,
    val data: ByteArray,
    val isAck: Boolean,
    val ackNum: Int?,
    val isEot: Boolean,
    var checksum: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Packet

        if (seqNum != other.seqNum) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = seqNum
        result = 31 * result + data.contentHashCode()
        return result
    }
}

fun serializePacket(packet: Packet): ByteArray {
    val byteStream = ByteArrayOutputStream()
    val dataStream = DataOutputStream(byteStream)

    dataStream.writeByte(packet.seqNum)
    dataStream.writeByte(if (packet.isAck) 1 else 0)
    dataStream.writeShort(packet.ackNum ?: 0)
    dataStream.writeShort(packet.data.size)
    dataStream.writeShort(packet.checksum)
    dataStream.writeByte((if (packet.isEot) 1 else 0))

    dataStream.write(packet.data)

    return byteStream.toByteArray()
}

fun deserializePacket(bytes: ByteArray): Packet {
    val byteStream = ByteArrayInputStream(bytes)
    val dataStream = DataInputStream(byteStream)

    val seqNum = dataStream.readByte().toInt()
    val isAck = dataStream.readByte().toInt() == 1
    val ackNum = dataStream.readShort().toInt()
    val dataLength = dataStream.readShort().toInt()
    val checksum = dataStream.readShort().toInt()
    val isEot = dataStream.readByte().toInt() == 1

    val data = ByteArray(dataLength)
    dataStream.readFully(data)

    return Packet(
        seqNum = seqNum,
        isAck = isAck,
        ackNum = ackNum,
        data = data,
        checksum = checksum,
        isEot = isEot
    )
}

fun makeAck(seqNum: Int): Packet = newPacket(
    seqNum = seqNum,
    isAck = true,
    ackNum = seqNum,
    data = ByteArray(0),
    isEot = false
)

fun newPacket(
    seqNum: Int,
    data: ByteArray,
    isAck: Boolean = false,
    ackNum: Int? = null,
    isEot: Boolean = false
): Packet {
    val temp = Packet(seqNum, data, isAck, ackNum, isEot, 0)
    val bytes = serializePacket(temp)
    return temp.copy(checksum = calcCheckSum(bytes))
}