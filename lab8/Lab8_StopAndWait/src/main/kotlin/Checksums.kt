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

fun testChecksum() {
    logger.info { "=== Testing checksums ===" }

    val message = "Hello, StopAndWait!".toByteArray()
    val checksum = calcCheckSum(message)
    val isValid = verifyChecksum(message, checksum)
    logger.info { "1. Valid message: $isValid (expected true)" }

    val corrupted = message.copyOf()
    if (corrupted.isNotEmpty()) corrupted[0] = (corrupted[0].toInt() xor 1).toByte()
    val isValidCorrupted = verifyChecksum(corrupted, checksum)
    logger.info { "2. Corrupted message: $isValidCorrupted (expected false)" }

    val empty = ByteArray(0)
    val emptyChecksum = calcCheckSum(empty)
    val isValidEmpty = verifyChecksum(empty, emptyChecksum)
    logger.info { "3. Empty message: $isValidEmpty (expected true)" }
}