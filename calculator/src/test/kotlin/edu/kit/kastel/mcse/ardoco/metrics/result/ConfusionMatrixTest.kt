package edu.kit.kastel.mcse.ardoco.metrics.result

import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.function.Executable

class ConfusionMatrixTest {
    @Test
    fun totalTest() {
        assertAll(
            Executable { assertEquals(21, ConfusionMatrix(6, 1, 2, 12).total) },
            Executable { assertEquals(0, ConfusionMatrix(0, 0, 0, 0).total) },
            Executable { assertNull(ConfusionMatrix(6, 1, 2, null).total) }
        )
    }

    @Test
    fun plusTest() {
        val sum = ConfusionMatrix(6, 1, 2, 12) + ConfusionMatrix(4, 3, 5, 8)
        assertEquals(ConfusionMatrix(10, 4, 7, 20), sum)
    }

    @Test
    fun plusWithoutTrueNegativesTest() {
        // The number of true negatives is only known if it is known for both operands.
        assertAll(
            Executable { assertNull((ConfusionMatrix(6, 1, 2, 12) + ConfusionMatrix(4, 3, 5, null)).trueNegatives) },
            Executable { assertNull((ConfusionMatrix(6, 1, 2, null) + ConfusionMatrix(4, 3, 5, 8)).trueNegatives) },
            Executable { assertNull((ConfusionMatrix(6, 1, 2, null) + ConfusionMatrix(4, 3, 5, null)).trueNegatives) },
            Executable { assertEquals(ConfusionMatrix(10, 4, 7, null), ConfusionMatrix(6, 1, 2, null) + ConfusionMatrix(4, 3, 5, null)) }
        )
    }

    @Test
    fun plusIsAssociativeAndCommutativeTest() {
        val a = ConfusionMatrix(6, 1, 2, 12)
        val b = ConfusionMatrix(4, 3, 5, 8)
        val c = ConfusionMatrix(1, 1, 1, 1)
        assertAll(
            Executable { assertEquals(a + b, b + a) },
            Executable { assertEquals((a + b) + c, a + (b + c)) }
        )
    }

    @Test
    fun negativeCountsAreRejectedTest() {
        assertAll(
            Executable { assertThrows<IllegalArgumentException> { ConfusionMatrix(-1, 0, 0, 0) } },
            Executable { assertThrows<IllegalArgumentException> { ConfusionMatrix(0, -1, 0, 0) } },
            Executable { assertThrows<IllegalArgumentException> { ConfusionMatrix(0, 0, -1, 0) } },
            Executable { assertThrows<IllegalArgumentException> { ConfusionMatrix(0, 0, 0, -1) } }
        )
    }
}
