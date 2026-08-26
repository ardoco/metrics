package edu.kit.kastel.mcse.ardoco.metrics.calculation

import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.function.Executable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ClassificationMetricsTest {
    @Test
    fun calculatePrecisionTest() {
        assertAll(
            Executable { assertEquals(.5, calculatePrecision(10, 10), 1e-3) },
            Executable { assertEquals(.857, calculatePrecision(6, 1), 1e-3) },
            Executable { assertEquals(.154, calculatePrecision(10, 55), 1e-3) },
            Executable { assertEquals(.905, calculatePrecision(210, 22), 1e-3) },
            // Documented sentinel: nothing was classified, so nothing was classified wrongly.
            Executable { assertEquals(1.0, calculatePrecision(0, 0), 1e-9) },
            Executable { assertEquals(0.0, calculatePrecision(0, 7), 1e-9) },
            Executable { assertEquals(1.0, calculatePrecision(7, 0), 1e-9) }
        )
    }

    @Test
    fun calculateRecallTest() {
        assertAll(
            Executable { assertEquals(.5, calculateRecall(10, 10), 1e-3) },
            Executable { assertEquals(.75, calculateRecall(6, 2), 1e-3) },
            Executable { assertEquals(.154, calculateRecall(10, 55), 1e-3) },
            Executable { assertEquals(.871, calculateRecall(210, 31), 1e-3) },
            // Documented sentinel: the ground truth is empty, so nothing was missed.
            Executable { assertEquals(1.0, calculateRecall(0, 0), 1e-9) },
            Executable { assertEquals(0.0, calculateRecall(0, 7), 1e-9) },
            Executable { assertEquals(1.0, calculateRecall(7, 0), 1e-9) }
        )
    }

    @Test
    fun calculateF1FromPrecisionRecallTest() {
        assertAll(
            Executable { assertEquals(1.0, calculateF1(1.0, 1.0), 1e-2) },
            Executable { assertEquals(0.0, calculateF1(0.0, 1.0), 1e-2) },
            Executable { assertEquals(0.0, calculateF1(1.0, 0.0), 1e-2) },
            Executable { assertEquals(0.18, calculateF1(.9, .1), 1e-2) },
            Executable { assertEquals(0.48, calculateF1(.6, .4), 1e-2) },
            Executable { assertEquals(0.42, calculateF1(.3, .7), 1e-2) },
            Executable { assertEquals(0.9, calculateF1(.9, .9), 1e-2) },
            Executable { assertEquals(0.48, calculateF1(.4, .6), 1e-2) },
            // Both zero: the NaN guard has to kick in.
            Executable { assertEquals(0.0, calculateF1(0.0, 0.0), 1e-9) }
        )
    }

    @Test
    fun calculateF1IsSymmetricTest() {
        val values = listOf(0.0, 0.1, 0.25, 0.5, 0.75, 0.9, 1.0)
        assertAll(
            values.flatMap { precision ->
                values.map { recall ->
                    Executable { assertEquals(calculateF1(precision, recall), calculateF1(recall, precision), 1e-12) }
                }
            }
        )
    }

    @Test
    fun calculateFBetaTest() {
        assertAll(
            // Textbook definition: F_beta = (1 + beta^2) * P * R / (beta^2 * P + R).
            Executable { assertEquals(.833333, calculateFBeta(.5, 1.0, 2.0), 1e-6) },
            Executable { assertEquals(.555556, calculateFBeta(.5, 1.0, 0.5), 1e-6) },
            Executable { assertEquals(.769231, calculateFBeta(.857143, .75, 2.0), 1e-6) },
            Executable { assertEquals(.833333, calculateFBeta(.857143, .75, 0.5), 1e-6) },
            Executable { assertEquals(1.0, calculateFBeta(1.0, 1.0, 3.0), 1e-9) },
            // Both zero: the NaN guard has to kick in for every beta.
            Executable { assertEquals(0.0, calculateFBeta(0.0, 0.0, 0.5), 1e-9) },
            Executable { assertEquals(0.0, calculateFBeta(0.0, 0.0, 1.0), 1e-9) },
            Executable { assertEquals(0.0, calculateFBeta(0.0, 0.0, 2.0), 1e-9) }
        )
    }

    @Test
    fun calculateFBetaWithBetaOneEqualsF1Test() {
        val values = listOf(0.0, 0.1, 0.25, 0.5, 0.75, 0.9, 1.0)
        assertAll(
            values.flatMap { precision ->
                values.map { recall ->
                    Executable { assertEquals(calculateF1(precision, recall), calculateFBeta(precision, recall, 1.0), 1e-12) }
                }
            }
        )
    }

    @Test
    fun calculateFBetaApproachesPrecisionAndRecallTest() {
        // For beta -> 0 the F-beta score approaches the precision, for beta -> infinity it approaches the recall.
        assertAll(
            Executable { assertEquals(.9, calculateFBeta(.9, .1, 0.001), 1e-4) },
            Executable { assertEquals(.1, calculateFBeta(.9, .1, 1000.0), 1e-4) },
            Executable { assertEquals(.3, calculateFBeta(.3, .7, 0.001), 1e-4) },
            Executable { assertEquals(.7, calculateFBeta(.3, .7, 1000.0), 1e-4) }
        )
    }

    @Test
    fun calculateFBetaIsMonotonicInBetaTest() {
        // With recall > precision, weighting the recall higher can only increase the score.
        val betas = listOf(0.25, 0.5, 1.0, 2.0, 4.0, 8.0)
        val scores = betas.map { calculateFBeta(.3, .7, it) }
        assertAll(
            scores.zipWithNext().map { (lower, higher) ->
                Executable { assertTrue(higher > lower) { "F-beta must increase with beta for recall > precision, but $higher <= $lower" } }
            }
        )
    }

    @Test
    fun calculateFBetaIsStableForExtremeBetasTest() {
        // Squaring the beta directly would overflow to infinity above roughly 1.3e154 and turn the result into NaN, which the guard would then
        // report as 0.0 instead of approaching the recall.
        assertAll(
            Executable { assertEquals(1.0, calculateFBeta(.5, 1.0, 1e200), 1e-9) },
            Executable { assertEquals(1.0, calculateFBeta(.5, 1.0, Double.MAX_VALUE), 1e-9) },
            Executable { assertEquals(.1, calculateFBeta(.9, .1, 1e200), 1e-9) },
            // The other end of the range underflows instead, which correctly approaches the precision.
            Executable { assertEquals(.9, calculateFBeta(.9, .1, Double.MIN_VALUE), 1e-9) },
            Executable { assertEquals(.5, calculateFBeta(.5, 1.0, 1e-200), 1e-9) },
            // Ordinary betas are unaffected by the reformulation.
            Executable { assertEquals(.8333333333333334, calculateFBeta(.5, 1.0, 2.0), 1e-15) },
            Executable { assertEquals(.9999990000019998, calculateFBeta(.5, 1.0, 1000.0), 1e-15) }
        )
    }

    @ParameterizedTest
    @ValueSource(doubles = [0.0, -1.0, -0.5, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY])
    fun calculateFBetaRejectsInvalidBetaTest(beta: Double) {
        assertThrows<IllegalArgumentException> { calculateFBeta(.5, .5, beta) }
    }

    @Test
    fun calculateAccuracyTest() {
        assertAll(
            Executable { assertEquals(.5, calculateAccuracy(10, 10, 10, 10), 1e-3) },
            Executable { assertEquals(.75, calculateAccuracy(6, 1, 2, 3), 1e-3) },
            Executable { assertEquals(.214, calculateAccuracy(10, 55, 55, 20), 1e-3) },
            Executable { assertEquals(.967, calculateAccuracy(210, 22, 31, 1337), 1e-3) },
            Executable { assertEquals(1.0, calculateAccuracy(10, 0, 0, 10), 1e-9) },
            Executable { assertEquals(0.0, calculateAccuracy(0, 10, 10, 0), 1e-9) }
        )
    }

    @Test
    fun calculateAccuracyWithoutAnyElementIsNotANumberTest() {
        // Pins existing behaviour: calculateAccuracy is the only function here without a zero-denominator guard.
        assertTrue(calculateAccuracy(0, 0, 0, 0).isNaN())
    }

    @Test
    fun calculateSpecificityTest() {
        assertAll(
            Executable { assertEquals(.5, calculateSpecificity(1, 1), 1e-3) },
            Executable { assertEquals(.76, calculateSpecificity(1337, 420), 1e-3) },
            Executable { assertEquals(.0, calculateSpecificity(0, 20), 1e-3) },
            Executable { assertEquals(1.0, calculateSpecificity(20, 0), 1e-3) },
            Executable { assertEquals(1.0, calculateSpecificity(0, 0), 1e-3) },
            Executable { assertEquals(.375, calculateSpecificity(3, 5), 1e-3) }
        )
    }

    @Test
    fun calculatePhiCoefficientTest() {
        assertAll(
            Executable { assertEquals(.0, calculatePhiCoefficient(10, 10, 10, 10), 1e-3) },
            Executable { assertEquals(.478, calculatePhiCoefficient(6, 1, 2, 3), 1e-3) },
            Executable { assertEquals(-.579, calculatePhiCoefficient(10, 55, 55, 20), 1e-3) },
            Executable {
                assertEquals(
                    .869,
                    calculatePhiCoefficient(210, 22, 31, 1337),
                    1e-3
                )
            },
            Executable { assertEquals(.0, calculatePhiCoefficient(0, 0, 11, 11), 1e-3) },
            Executable { assertEquals(.0, calculatePhiCoefficient(11, 0, 11, 0), 1e-3) },
            // Perfect agreement and perfect disagreement.
            Executable { assertEquals(1.0, calculatePhiCoefficient(10, 0, 0, 10), 1e-9) },
            Executable { assertEquals(-1.0, calculatePhiCoefficient(0, 10, 10, 0), 1e-9) }
        )
    }

    @Test
    fun calculatePhiCoefficientZeroMarginsTest() {
        // Each of the four marginals being zero has to short-circuit to 0.0 instead of dividing by zero.
        assertAll(
            // tp + fp == 0
            Executable { assertEquals(.0, calculatePhiCoefficient(0, 0, 5, 5), 1e-9) },
            // tp + fn == 0
            Executable { assertEquals(.0, calculatePhiCoefficient(0, 5, 0, 5), 1e-9) },
            // tn + fp == 0
            Executable { assertEquals(.0, calculatePhiCoefficient(5, 0, 5, 0), 1e-9) },
            // tn + fn == 0
            Executable { assertEquals(.0, calculatePhiCoefficient(5, 5, 0, 0), 1e-9) }
        )
    }

    @Test
    fun calculatePhiCoefficientMaxTest() {
        assertAll(
            // Standard case: fn + tp >= fp + tp.
            Executable { assertEquals(.836660, calculatePhiCoefficientMax(6, 1, 2, 3), 1e-6) },
            Executable { assertEquals(.977917, calculatePhiCoefficientMax(210, 22, 31, 1337), 1e-6) },
            // Mirrored case: fn + tp < fp + tp, so nominator and denominator are swapped.
            Executable { assertEquals(.577350, calculatePhiCoefficientMax(3, 7, 2, 8), 1e-6) },
            Executable { assertEquals(1.0, calculatePhiCoefficientMax(10, 10, 10, 10), 1e-9) },
            Executable { assertEquals(1.0, calculatePhiCoefficientMax(10, 0, 0, 10), 1e-9) },
            Executable { assertEquals(1.0, calculatePhiCoefficientMax(0, 10, 10, 0), 1e-9) }
        )
    }

    @Test
    fun calculatePhiCoefficientMaxDegenerateTest() {
        // Zero nominator or denominator must short-circuit to 0.0 instead of throwing.
        assertAll(
            Executable { assertEquals(.0, calculatePhiCoefficientMax(0, 0, 0, 0), 1e-9) },
            Executable { assertEquals(.0, calculatePhiCoefficientMax(5, 0, 0, 0), 1e-9) },
            Executable { assertEquals(.0, calculatePhiCoefficientMax(0, 0, 5, 0), 1e-9) },
            Executable { assertEquals(.0, calculatePhiCoefficientMax(0, 5, 0, 0), 1e-9) },
            Executable { assertEquals(.0, calculatePhiCoefficientMax(0, 0, 0, 5), 1e-9) }
        )
    }

    @Test
    fun calculatePhiOverPhiMaxTest() {
        assertAll(
            Executable { assertEquals(.571429, calculatePhiOverPhiMax(6, 1, 2, 3), 1e-6) },
            Executable { assertEquals(.888356, calculatePhiOverPhiMax(210, 22, 31, 1337), 1e-6) },
            Executable { assertEquals(.2, calculatePhiOverPhiMax(3, 7, 2, 8), 1e-6) },
            Executable { assertEquals(-.579487, calculatePhiOverPhiMax(10, 55, 55, 20), 1e-6) },
            Executable { assertEquals(1.0, calculatePhiOverPhiMax(10, 0, 0, 10), 1e-9) },
            Executable { assertEquals(-1.0, calculatePhiOverPhiMax(0, 10, 10, 0), 1e-9) },
            // phiMax == 0 short-circuits to 0.0.
            Executable { assertEquals(.0, calculatePhiOverPhiMax(0, 0, 0, 0), 1e-9) }
        )
    }

    @ParameterizedTest
    @CsvSource(
        "10, 0, 0, 1.0, 1.0",
        "0, 10, 10, 0.0, 0.0",
        "10, 10, 10, 0.5, 0.5",
        "6, 1, 2, 0.857143, 0.75",
        "210, 22, 31, 0.905172, 0.871369",
        "10, 55, 55, 0.153846, 0.153846",
        "3, 7, 2, 0.3, 0.6"
    )
    fun referencePrecisionAndRecallTest(
        tp: Int,
        fp: Int,
        fn: Int,
        expectedPrecision: Double,
        expectedRecall: Double
    ) {
        assertAll(
            Executable { assertEquals(expectedPrecision, calculatePrecision(tp, fp), 1e-6) },
            Executable { assertEquals(expectedRecall, calculateRecall(tp, fn), 1e-6) }
        )
    }

    @ParameterizedTest
    @CsvSource(
        "10, 0, 0, 1.0, 1.0, 1.0",
        "0, 10, 10, 0.0, 0.0, 0.0",
        "10, 10, 10, 0.5, 0.5, 0.5",
        "6, 1, 2, 0.833333, 0.8, 0.769231",
        "210, 22, 31, 0.898204, 0.887949, 0.877926",
        "10, 55, 55, 0.153846, 0.153846, 0.153846",
        "3, 7, 2, 0.333333, 0.4, 0.5"
    )
    fun referenceFScoresTest(
        tp: Int,
        fp: Int,
        fn: Int,
        expectedFHalf: Double,
        expectedF1: Double,
        expectedF2: Double
    ) {
        val precision = calculatePrecision(tp, fp)
        val recall = calculateRecall(tp, fn)
        assertAll(
            Executable { assertEquals(expectedFHalf, calculateFBeta(precision, recall, 0.5), 1e-6) },
            Executable { assertEquals(expectedF1, calculateF1(precision, recall), 1e-6) },
            Executable { assertEquals(expectedF2, calculateFBeta(precision, recall, 2.0), 1e-6) }
        )
    }

    @ParameterizedTest
    @CsvSource(
        "10, 0, 0, 10, 1.0, 1.0",
        "0, 10, 10, 0, 0.0, 0.0",
        "10, 10, 10, 10, 0.5, 0.5",
        "6, 1, 2, 3, 0.75, 0.75",
        "210, 22, 31, 1337, 0.966875, 0.983812",
        "10, 55, 55, 20, 0.214286, 0.266667",
        "3, 7, 2, 8, 0.55, 0.533333"
    )
    fun referenceAccuracyAndSpecificityTest(
        tp: Int,
        fp: Int,
        fn: Int,
        tn: Int,
        expectedAccuracy: Double,
        expectedSpecificity: Double
    ) {
        assertAll(
            Executable { assertEquals(expectedAccuracy, calculateAccuracy(tp, fp, fn, tn), 1e-6) },
            Executable { assertEquals(expectedSpecificity, calculateSpecificity(tn, fp), 1e-6) }
        )
    }

    @ParameterizedTest
    @CsvSource(
        "10, 0, 0, 10, 1.0, 1.0, 1.0",
        "0, 10, 10, 0, -1.0, 1.0, -1.0",
        "10, 10, 10, 10, 0.0, 1.0, 0.0",
        "6, 1, 2, 3, 0.478091, 0.836660, 0.571429",
        "210, 22, 31, 1337, 0.868739, 0.977917, 0.888356",
        "10, 55, 55, 20, -0.579487, 1.0, -0.579487",
        "3, 7, 2, 8, 0.115470, 0.577350, 0.2"
    )
    fun referencePhiTest(
        tp: Int,
        fp: Int,
        fn: Int,
        tn: Int,
        expectedPhi: Double,
        expectedPhiMax: Double,
        expectedPhiOverPhiMax: Double
    ) {
        assertAll(
            Executable { assertEquals(expectedPhi, calculatePhiCoefficient(tp, fp, fn, tn), 1e-6) },
            Executable { assertEquals(expectedPhiMax, calculatePhiCoefficientMax(tp, fp, fn, tn), 1e-6) },
            Executable { assertEquals(expectedPhiOverPhiMax, calculatePhiOverPhiMax(tp, fp, fn, tn), 1e-6) }
        )
    }

    @ParameterizedTest
    @CsvSource(
        "0, 0, 0, 0",
        "5, 0, 0, 0",
        "0, 5, 0, 0",
        "0, 0, 5, 0",
        "0, 0, 0, 5",
        "10, 0, 0, 10",
        "0, 10, 10, 0",
        "10, 10, 10, 10",
        "6, 1, 2, 3",
        "3, 7, 2, 8",
        "210, 22, 31, 1337",
        "10, 55, 55, 20"
    )
    fun metricInvariantsTest(
        tp: Int,
        fp: Int,
        fn: Int,
        tn: Int
    ) {
        val precision = calculatePrecision(tp, fp)
        val recall = calculateRecall(tp, fn)
        val f1 = calculateF1(precision, recall)
        val specificity = calculateSpecificity(tn, fp)
        val phi = calculatePhiCoefficient(tp, fp, fn, tn)
        val phiMax = calculatePhiCoefficientMax(tp, fp, fn, tn)
        val phiOverPhiMax = calculatePhiOverPhiMax(tp, fp, fn, tn)
        assertAll(
            Executable { assertInUnitInterval("precision", precision) },
            Executable { assertInUnitInterval("recall", recall) },
            Executable { assertInUnitInterval("specificity", specificity) },
            Executable { assertInUnitInterval("F0.5", calculateFBeta(precision, recall, 0.5)) },
            Executable { assertInUnitInterval("F1", f1) },
            Executable { assertInUnitInterval("F2", calculateFBeta(precision, recall, 2.0)) },
            Executable { assertInUnitInterval("phiMax", phiMax) },
            Executable { assertTrue(abs(phi) <= 1.0 + 1e-9) { "phi must be within [-1, 1] but was $phi" } },
            Executable { assertTrue(abs(phiOverPhiMax) <= 1.0 + 1e-9) { "phi/phiMax must be within [-1, 1] but was $phiOverPhiMax" } },
            Executable { assertTrue(phiMax >= abs(phi) - 1e-9) { "phiMax ($phiMax) must not be smaller than abs(phi) (${abs(phi)})" } },
            Executable {
                val lower = min(precision, recall)
                val upper = max(precision, recall)
                assertTrue(f1 >= lower - 1e-9 && f1 <= upper + 1e-9) { "F1 ($f1) must lie between precision and recall ($lower..$upper)" }
            }
        )
    }

    private fun assertInUnitInterval(
        name: String,
        value: Double
    ) {
        assertFalse(value.isNaN()) { "$name must not be NaN" }
        assertTrue(value.isFinite()) { "$name must be finite but was $value" }
        assertTrue(value >= -1e-9 && value <= 1.0 + 1e-9) { "$name must be within [0, 1] but was $value" }
    }
}
