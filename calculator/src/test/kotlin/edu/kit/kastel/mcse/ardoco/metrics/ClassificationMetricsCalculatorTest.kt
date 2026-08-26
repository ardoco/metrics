package edu.kit.kastel.mcse.ardoco.metrics

import edu.kit.kastel.mcse.ardoco.metrics.calculation.calculateFBeta
import edu.kit.kastel.mcse.ardoco.metrics.result.ConfusionMatrix
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.function.Executable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ClassificationMetricsCalculatorTest {
    private val calculator = ClassificationMetricsCalculator.Instance

    // tp = {a, b}, fp = {c}, fn = {d, e}, tn = 10 - 5 = 5
    private val classification = setOf("a", "b", "c")
    private val groundTruth = setOf("a", "b", "d", "e")

    @Test
    fun setAlgebraTest() {
        val result = calculator.calculateMetrics(classification, groundTruth, 10)
        assertAll(
            Executable { assertEquals(setOf("a", "b"), result.truePositives) },
            Executable { assertEquals(setOf("c"), result.falsePositives) },
            Executable { assertEquals(setOf("d", "e"), result.falseNegatives) },
            Executable { assertEquals(5, result.trueNegatives) },
            Executable { assertEquals(ConfusionMatrix(2, 1, 2, 5), result.confusionMatrix) },
            Executable { assertEquals(10, result.confusionMatrix.total()) }
        )
    }

    @Test
    fun allMetricsTest() {
        val result = calculator.calculateMetrics(classification, groundTruth, 10)
        assertAll(
            Executable { assertEquals(0.66666667, result.precision, 1e-8) },
            Executable { assertEquals(0.5, result.recall, 1e-8) },
            Executable { assertEquals(0.57142857, result.f1, 1e-8) },
            Executable { assertEquals(0.7, result.accuracy!!, 1e-8) },
            Executable { assertEquals(0.83333333, result.specificity!!, 1e-8) },
            Executable { assertEquals(0.35634832, result.phiCoefficient!!, 1e-8) },
            Executable { assertEquals(0.80178373, result.phiCoefficientMax!!, 1e-8) },
            Executable { assertEquals(0.44444444, result.phiOverPhiMax!!, 1e-8) }
        )
    }

    @Test
    fun withoutConfusionMatrixSumTest() {
        val result = calculator.calculateMetrics(classification, groundTruth, null)
        assertAll(
            Executable { assertNull(result.trueNegatives) },
            Executable { assertNull(result.confusionMatrix.trueNegatives) },
            Executable { assertNull(result.confusionMatrix.total()) },
            Executable { assertNull(result.accuracy) },
            Executable { assertNull(result.specificity) },
            Executable { assertNull(result.phiCoefficient) },
            Executable { assertNull(result.phiCoefficientMax) },
            Executable { assertNull(result.phiOverPhiMax) },
            // Precision, recall and the F-beta scores never depend on the true negatives.
            Executable { assertEquals(0.66666667, result.precision, 1e-8) },
            Executable { assertEquals(0.5, result.recall, 1e-8) },
            Executable { assertEquals(0.57142857, result.f1, 1e-8) }
        )
    }

    @Test
    fun tooSmallConfusionMatrixSumIsRejectedTest() {
        // 5 elements are classified or expected, so a sum of 4 would imply a negative number of true negatives.
        assertAll(
            Executable { assertThrows<IllegalArgumentException> { calculator.calculateMetrics(classification, groundTruth, 4) } },
            Executable { assertDoesNotThrow { calculator.calculateMetrics(classification, groundTruth, 5) } },
            Executable { assertEquals(0, calculator.calculateMetrics(classification, groundTruth, 5).trueNegatives) }
        )
    }

    @Test
    fun emptyClassificationAndGroundTruthTest() {
        // Documented sentinels: nothing was classified wrongly and nothing was missed.
        val result = calculator.calculateMetrics(emptySet<String>(), emptySet(), null)
        assertAll(
            Executable { assertEquals(1.0, result.precision, 1e-9) },
            Executable { assertEquals(1.0, result.recall, 1e-9) },
            Executable { assertEquals(1.0, result.f1, 1e-9) },
            Executable { assertEquals(ConfusionMatrix(0, 0, 0, null), result.confusionMatrix) }
        )
    }

    @Test
    fun disjointClassificationTest() {
        val result = calculator.calculateMetrics(setOf("a", "b"), setOf("c", "d"), null)
        assertAll(
            Executable { assertEquals(0.0, result.precision, 1e-9) },
            Executable { assertEquals(0.0, result.recall, 1e-9) },
            Executable { assertEquals(0.0, result.f1, 1e-9) },
            Executable { assertEquals(ConfusionMatrix(0, 2, 2, null), result.confusionMatrix) }
        )
    }

    @Test
    fun classificationSupersetOfGroundTruthTest() {
        val result = calculator.calculateMetrics(setOf("a", "b", "c"), setOf("a", "b"), null)
        assertAll(
            Executable { assertEquals(0.66666667, result.precision, 1e-8) },
            Executable { assertEquals(1.0, result.recall, 1e-9) }
        )
    }

    @Test
    fun groundTruthSupersetOfClassificationTest() {
        val result = calculator.calculateMetrics(setOf("a", "b"), setOf("a", "b", "c"), null)
        assertAll(
            Executable { assertEquals(1.0, result.precision, 1e-9) },
            Executable { assertEquals(0.66666667, result.recall, 1e-8) }
        )
    }

    @Test
    fun defaultBetasTest() {
        val result = calculator.calculateMetrics(classification, groundTruth, 10)
        assertAll(
            Executable { assertEquals(setOf(1.0), ClassificationMetricsCalculator.DefaultBetas) },
            Executable { assertEquals(listOf(1.0), result.fbetaScores.keys.toList()) },
            Executable { assertEquals(result.f1, result.fbetaScores.getValue(1.0)) }
        )
    }

    @Test
    fun requestedBetasTest() {
        val result = calculator.calculateMetrics(classification, groundTruth, 10, listOf(2.0, 0.5, 2.0))
        assertAll(
            // Duplicates are dropped, beta 1.0 is added, and the keys are ascending.
            Executable { assertEquals(listOf(0.5, 1.0, 2.0), result.fbetaScores.keys.toList()) },
            Executable { assertEquals(0.625, result.fbetaScores.getValue(0.5), 1e-8) },
            Executable { assertEquals(0.57142857, result.fbetaScores.getValue(1.0), 1e-8) },
            Executable { assertEquals(0.52631579, result.fbetaScores.getValue(2.0), 1e-8) },
            Executable { assertEquals(result.f1, result.fbetaScores.getValue(1.0)) },
            Executable {
                assertAll(
                    result.fbetaScores.map { (beta, score) ->
                        Executable { assertEquals(calculateFBeta(result.precision, result.recall, beta), score, 1e-12) }
                    }
                )
            }
        )
    }

    @Test
    fun fbetaOfNotRequestedBetaIsRecomputedTest() {
        val result = calculator.calculateMetrics(classification, groundTruth, 10)
        assertAll(
            Executable { assertNull(result.fbetaOrNull(3.0)) },
            Executable { assertEquals(calculateFBeta(result.precision, result.recall, 3.0), result.fbeta(3.0), 1e-12) },
            Executable { assertEquals(result.f1, result.fbeta(1.0)) },
            Executable { assertEquals(result.f1, result.fbetaOrNull(1.0)) }
        )
    }

    @ParameterizedTest
    @ValueSource(doubles = [0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY])
    fun invalidBetasAreRejectedTest(beta: Double) {
        assertAll(
            Executable { assertThrows<IllegalArgumentException> { calculator.calculateMetrics(classification, groundTruth, 10, listOf(beta)) } },
            Executable { assertThrows<IllegalArgumentException> { calculator.calculateMetrics(classification, groundTruth, 10).fbeta(beta) } }
        )
    }

    @Test
    fun stringProviderOverloadTest() {
        val classified = setOf(Element("a"), Element("b"), Element("c"))
        val expected = setOf(Element("a"), Element("b"), Element("d"), Element("e"))
        val result = calculator.calculateMetrics(classified, expected, { it.id }, 10, listOf(2.0))
        assertAll(
            Executable { assertEquals(setOf("a", "b"), result.truePositives) },
            Executable { assertEquals(setOf("c"), result.falsePositives) },
            Executable { assertEquals(setOf("d", "e"), result.falseNegatives) },
            Executable { assertEquals(0.66666667, result.precision, 1e-8) },
            Executable { assertEquals(listOf(1.0, 2.0), result.fbetaScores.keys.toList()) }
        )
    }

    @Test
    fun stringProviderOverloadWithDefaultBetasTest() {
        val classified = setOf(Element("a"), Element("b"))
        val expected = setOf(Element("a"))
        val result = calculator.calculateMetrics(classified, expected, { it.id }, null)
        assertAll(
            Executable { assertEquals(setOf("a"), result.truePositives) },
            Executable { assertEquals(listOf(1.0), result.fbetaScores.keys.toList()) }
        )
    }

    @Test
    fun customElementTypeTest() {
        val classified = setOf(Element("a"), Element("b"))
        val expected = setOf(Element("a"), Element("c"))
        val result = calculator.calculateMetrics(classified, expected, null)
        assertAll(
            Executable { assertEquals(setOf(Element("a")), result.truePositives) },
            Executable { assertEquals(setOf(Element("b")), result.falsePositives) },
            Executable { assertEquals(setOf(Element("c")), result.falseNegatives) },
            Executable { assertEquals(0.5, result.precision, 1e-9) },
            Executable { assertEquals(0.5, result.recall, 1e-9) }
        )
    }

    @Test
    fun prettyPrintDoesNotThrowTest() {
        assertAll(
            Executable { assertDoesNotThrow { calculator.calculateMetrics(classification, groundTruth, 10).prettyPrint() } },
            Executable { assertDoesNotThrow { calculator.calculateMetrics(classification, groundTruth, null).prettyPrint() } },
            Executable {
                assertDoesNotThrow { calculator.calculateMetrics(classification, groundTruth, 10, listOf(0.5, 2.0, 4.0)).prettyPrint() }
            }
        )
    }

    private data class Element(
        val id: String
    )
}
