package edu.kit.kastel.mcse.ardoco.metrics.result

import edu.kit.kastel.mcse.ardoco.metrics.ClassificationMetricsCalculator
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.function.Executable

/**
 * Covers the invariants the result types enforce on direct construction, i.e. when they are built by hand or by a deserializer rather than by the
 * calculator. These are what keep the derived accessors from ever throwing or returning something inconsistent.
 */
class ResultValidationTest {
    private val calculator = ClassificationMetricsCalculator.Instance

    private fun single(
        truePositives: Set<String> = setOf("a", "b"),
        falsePositives: Set<String> = setOf("c"),
        falseNegatives: Set<String> = setOf("d"),
        trueNegatives: Int? = 5,
        fbetaScores: Map<Double, Double> = mapOf(1.0 to 0.5),
        confusionMatrix: ConfusionMatrix = ConfusionMatrix(truePositives.size, falsePositives.size, falseNegatives.size, trueNegatives)
    ) = SingleClassificationResult(
        truePositives,
        falsePositives,
        falseNegatives,
        trueNegatives,
        0.5,
        0.5,
        0.5,
        fbetaScores,
        null,
        null,
        null,
        null,
        null,
        confusionMatrix
    )

    @Test
    fun singleResultRejectsNegativeTrueNegativesTest() {
        assertAll(
            Executable { assertThrows<IllegalArgumentException> { single(trueNegatives = -1) } },
            Executable { assertDoesNotThrow { single(trueNegatives = 0) } },
            Executable { assertDoesNotThrow { single(trueNegatives = null) } }
        )
    }

    @Test
    fun singleResultRejectsFbetaScoresWithoutF1Test() {
        assertAll(
            Executable { assertThrows<IllegalArgumentException> { single(fbetaScores = emptyMap()) } },
            Executable { assertThrows<IllegalArgumentException> { single(fbetaScores = mapOf(2.0 to 0.5)) } },
            Executable { assertDoesNotThrow { single(fbetaScores = mapOf(1.0 to 0.5, 2.0 to 0.5)) } }
        )
    }

    @Test
    fun singleResultRejectsAnF1ThatDisagreesWithTheFbetaScoresTest() {
        // ClassificationResult guarantees that f1 is the F-beta score for beta 1.0; without this check the two accessors could disagree and
        // aggregation, which reads the map, would silently use the other value.
        assertAll(
            Executable { assertThrows<IllegalArgumentException> { single(fbetaScores = mapOf(1.0 to 0.25)) } },
            Executable { assertThrows<IllegalArgumentException> { single(fbetaScores = mapOf(1.0 to 0.25, 2.0 to 0.5)) } },
            Executable { assertDoesNotThrow { single(fbetaScores = mapOf(1.0 to 0.5, 2.0 to 0.25)) } },
            Executable { assertEquals(single().f1, single().fbeta(1.0)) }
        )
    }

    @Test
    fun aggregatedResultRejectsAnF1ThatDisagreesWithTheFbetaScoresTest() {
        val matrix = ConfusionMatrix(2, 1, 1, 5)
        assertThrows<IllegalArgumentException> {
            AggregatedClassificationResult(
                AggregationType.MACRO_AVERAGE,
                matrix,
                0.5,
                0.5,
                0.5,
                mapOf(1.0 to 0.25),
                null,
                null,
                null,
                null,
                null
            )
        }
    }

    @Test
    fun aggregationResultRejectsUnusableWeightsTest() {
        val aggregation = calculator.calculateAverages(listOf(calculator.calculateMetrics(setOf("a"), setOf("a"), 4)))
        assertAll(
            Executable {
                assertThrows<IllegalArgumentException> {
                    ClassificationAggregationResult(
                        aggregation.singleResults,
                        listOf(-1),
                        aggregation.macroAverage,
                        aggregation.weightedAverage,
                        aggregation.microAverage
                    )
                }
            },
            Executable {
                assertThrows<IllegalArgumentException> {
                    ClassificationAggregationResult(
                        aggregation.singleResults,
                        listOf(0),
                        aggregation.macroAverage,
                        aggregation.weightedAverage,
                        aggregation.microAverage
                    )
                }
            }
        )
    }

    @Test
    fun singleResultRejectsConfusionMatrixThatDisagreesWithTheElementsTest() {
        assertAll(
            // Wrong counts.
            Executable { assertThrows<IllegalArgumentException> { single(confusionMatrix = ConfusionMatrix(9, 1, 1, 5)) } },
            // Right counts but a true negative count that contradicts the trueNegatives field.
            Executable { assertThrows<IllegalArgumentException> { single(confusionMatrix = ConfusionMatrix(2, 1, 1, 6)) } },
            Executable { assertThrows<IllegalArgumentException> { single(trueNegatives = null, confusionMatrix = ConfusionMatrix(2, 1, 1, 5)) } },
            Executable { assertDoesNotThrow { single(confusionMatrix = ConfusionMatrix(2, 1, 1, 5)) } },
            // The default derives it from the elements, so it always agrees.
            Executable { assertEquals(ConfusionMatrix(2, 1, 1, 5), single().confusionMatrix) }
        )
    }

    @Test
    fun aggregatedResultRejectsFbetaScoresWithoutF1Test() {
        val matrix = ConfusionMatrix(2, 1, 1, 5)
        assertAll(
            Executable {
                assertThrows<IllegalArgumentException> {
                    AggregatedClassificationResult(AggregationType.MACRO_AVERAGE, matrix, 0.5, 0.5, 0.5, emptyMap(), null, null, null, null, null)
                }
            },
            Executable {
                assertDoesNotThrow {
                    AggregatedClassificationResult(
                        AggregationType.MACRO_AVERAGE,
                        matrix,
                        0.5,
                        0.5,
                        0.5,
                        mapOf(1.0 to 0.5),
                        null,
                        null,
                        null,
                        null,
                        null
                    )
                }
            },
            Executable {
                // The default keeps f1 and the F-beta scores in agreement.
                val result =
                    AggregatedClassificationResult(
                        type = AggregationType.MACRO_AVERAGE,
                        confusionMatrix = matrix,
                        precision = 0.5,
                        recall = 0.5,
                        f1 = 0.25,
                        accuracy = null,
                        specificity = null,
                        phiCoefficient = null,
                        phiCoefficientMax = null,
                        phiOverPhiMax = null
                    )
                assertEquals(mapOf(1.0 to 0.25), result.fbetaScores)
            }
        )
    }

    @Test
    fun aggregationResultRejectsEmptySingleResultsTest() {
        val aggregation = calculator.calculateAverages(listOf(calculator.calculateMetrics(setOf("a"), setOf("a"), 4)))
        assertThrows<IllegalArgumentException> {
            ClassificationAggregationResult(
                emptyList<SingleClassificationResult<String>>(),
                emptyList(),
                aggregation.macroAverage,
                aggregation.weightedAverage,
                aggregation.microAverage
            )
        }
    }

    @Test
    fun metricSpreadRejectsEmptyValuesTest() {
        assertAll(
            Executable { assertThrows<IllegalArgumentException> { MetricSpread.of(emptyList()) } },
            Executable { assertEquals(MetricSpread(0.5, 0.5, 0.5, 0.0), MetricSpread.of(listOf(0.5))) },
            Executable { assertEquals(MetricSpread(0.0, 1.0, 0.5, 0.5), MetricSpread.of(listOf(0.0, 1.0))) }
        )
    }
}
