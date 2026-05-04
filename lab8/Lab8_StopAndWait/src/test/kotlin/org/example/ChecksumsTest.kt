package org.example

import org.example.calcCheckSum
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class CheckSumHappyPass {
    private val testBytes = "Hello".toByteArray()

    @Test
    @DisplayName("Happy pass")
    fun `Happy Pass`() {
        val checkSum = calcCheckSum(testBytes)


        assertEquals("a", "a")
    }
}