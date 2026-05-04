package org.example

fun calcCheckSum(data: ByteArray): Int {
    var sum = 0
    var i = 0

    while (i < data.size - 1) {
        val high = data[i]
        val low = data[i + 1]

        val word = ((high.toInt() and 0xFF) shl 8) or (low.toInt() and 0xFF)
        sum += word
        sum = sum and 0xFFFF

        i += 2
    }

    if (i == data.size - 1) {
        val lastByte = (data[i].toInt() and 0xFF) shl 8
        sum += lastByte
        sum = sum and 0xFFFF

    }

    return sum.inv() and 0xFFFF
}


fun verifyChecksum(data: ByteArray, receivedChecksum: Int): Boolean {
    var sum = calcCheckSum(data).inv() and 0xFFFF
    sum += receivedChecksum
    return (sum and 0xFFFF) == 0xFFFF
}