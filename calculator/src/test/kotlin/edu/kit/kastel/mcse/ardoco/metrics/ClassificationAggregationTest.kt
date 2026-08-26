package edu.kit.kastel.mcse.ardoco.metrics

import edu.kit.kastel.mcse.ardoco.metrics.calculation.calculateFBeta
import edu.kit.kastel.mcse.ardoco.metrics.result.AggregationType
import edu.kit.kastel.mcse.ardoco.metrics.result.ClassificationAggregationResult
import edu.kit.kastel.mcse.ardoco.metrics.result.ClassificationMetric
import edu.kit.kastel.mcse.ardoco.metrics.result.ConfusionMatrix
import edu.kit.kastel.mcse.ardoco.metrics.result.MetricSpread
import edu.kit.kastel.mcse.ardoco.metrics.result.SingleClassificationResult
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.function.Executable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.math.abs

class ClassificationAggregationTest {
    private val calculator = ClassificationMetricsCalculator.Instance
    private val betas = listOf(0.5, 2.0)

    /**
     * Three results with deliberately skewed precision and recall and unequal ground truth sizes:
     *  - r1: tp = 2, fp = 1, fn = 2, tn = 5  (precision 0.667, recall 0.5,   ground truth size 4)
     *  - r2: tp = 1, fp = 3, fn = 0, tn = 8  (precision 0.25,  recall 1.0,   ground truth size 1)
     *  - r3: tp = 2, fp = 0, fn = 4, tn = 14 (precision 1.0,   recall 0.333, ground truth size 6)
     */
    private fun results(confusionMatrixSums: Boolean = true): List<SingleClassificationResult<String>> =
        listOf(
            calculator.calculateMetrics(setOf("a", "b", "c"), setOf("a", "b", "d", "e"), if (confusionMatrixSums) 10 else null, betas),
            calculator.calculateMetrics(setOf("f", "g", "h", "i"), setOf("f"), if (confusionMatrixSums) 12 else null, betas),
            calculator.calculateMetrics(setOf("j", "k"), setOf("j", "k", "l", "m", "n", "o"), if (confusionMatrixSums) 20 else null, betas)
        )

    private fun aggregate(
        weights: List<Int>? = null,
        betaOverride: Collection<Double>? = null
    ): ClassificationAggregationResult<String> = calculator.calculateAverages(results(), weights, betaOverride)

    @Test
    fun macroAverageTest() {
        val macro = aggregate().macroAverage
        assertAll(
            Executable { assertEquals(AggregationType.MACRO_AVERAGE, macro.type) },
            Executable { assertEquals(0.63888889, macro.precision, 1e-8) },
            Executable { assertEquals(0.61111111, macro.recall, 1e-8) },
            Executable { assertEquals(0.49047619, macro.f1, 1e-8) },
            Executable { assertEquals(0.54446779, macro.fbetaScores.getValue(0.5), 1e-8) },
            Executable { assertEquals(0.49047619, macro.fbetaScores.getValue(1.0), 1e-8) },
            Executable { assertEquals(0.51197706, macro.fbetaScores.getValue(2.0), 1e-8) },
            Executable { assertEquals(0.75, macro.accuracy!!, 1e-8) },
            Executable { assertEquals(0.85353535, macro.specificity!!, 1e-8) },
            Executable { assertEquals(0.43064161, macro.phiCoefficient!!, 1e-8) },
            Executable { assertEquals(0.57912008, macro.phiCoefficientMax!!, 1e-8) },
            Executable { assertEquals(0.81481481, macro.phiOverPhiMax!!, 1e-8) }
        )
    }

    @Test
    fun weightedAverageWithDefaultWeightsTest() {
        val aggregation = aggregate()
        val weighted = aggregation.weightedAverage
        assertAll(
            // The default weight of a result is the size of its ground standard, i.e. tp + fn.
            Executable { assertEquals(listOf(4, 1, 6), aggregation.weights) },
            Executable { assertEquals(AggregationType.WEIGHTED_AVERAGE, weighted.type) },
            Executable { assertEquals(0.81060606, weighted.precision, 1e-8) },
            Executable { assertEquals(0.45454545, weighted.recall, 1e-8) },
            Executable { assertEquals(0.51688312, weighted.f1, 1e-8) },
            Executable { assertEquals(0.64362108, weighted.fbetaScores.getValue(0.5), 1e-8) },
            Executable { assertEquals(0.45799595, weighted.fbetaScores.getValue(2.0), 1e-8) },
            Executable { assertEquals(0.75909091, weighted.accuracy!!, 1e-8) },
            Executable { assertEquals(0.91460055, weighted.specificity!!, 1e-8) },
            Executable { assertEquals(0.44607684, weighted.phiCoefficient!!, 1e-8) },
            Executable { assertEquals(0.60805335, weighted.phiCoefficientMax!!, 1e-8) },
            Executable { assertEquals(0.79797980, weighted.phiOverPhiMax!!, 1e-8) }
        )
    }

    @Test
    fun weightedAverageWithExplicitWeightsTest() {
        val aggregation = aggregate(weights = listOf(2, 3, 5))
        val weighted = aggregation.weightedAverage
        assertAll(
            Executable { assertEquals(listOf(2, 3, 5), aggregation.weights) },
            Executable { assertEquals(0.70833333, weighted.precision, 1e-8) },
            Executable { assertEquals(0.56666667, weighted.recall, 1e-8) },
            Executable { assertEquals(0.48428571, weighted.f1, 1e-8) },
            Executable { assertEquals(0.57037815, weighted.fbetaScores.getValue(0.5), 1e-8) },
            Executable { assertEquals(0.48507085, weighted.fbetaScores.getValue(2.0), 1e-8) },
            Executable { assertEquals(0.765, weighted.accuracy!!, 1e-8) },
            Executable { assertEquals(0.88484848, weighted.specificity!!, 1e-8) },
            Executable { assertEquals(0.45377763, weighted.phiCoefficient!!, 1e-8) },
            Executable { assertEquals(0.54286471, weighted.phiCoefficientMax!!, 1e-8) },
            Executable { assertEquals(0.88888889, weighted.phiOverPhiMax!!, 1e-8) }
        )
    }

    @Test
    fun microAverageTest() {
        val micro = aggregate().microAverage
        assertAll(
            Executable { assertEquals(AggregationType.MICRO_AVERAGE, micro.type) },
            // Recalculated from the pooled confusion matrix tp = 5, fp = 4, fn = 6, tn = 27.
            Executable { assertEquals(ConfusionMatrix(5, 4, 6, 27), micro.confusionMatrix) },
            Executable { assertEquals(0.55555556, micro.precision, 1e-8) },
            Executable { assertEquals(0.45454545, micro.recall, 1e-8) },
            Executable { assertEquals(0.5, micro.f1, 1e-8) },
            Executable { assertEquals(0.53191489, micro.fbetaScores.getValue(0.5), 1e-8) },
            Executable { assertEquals(0.47169811, micro.fbetaScores.getValue(2.0), 1e-8) },
            Executable { assertEquals(0.76190476, micro.accuracy!!, 1e-8) },
            Executable { assertEquals(0.87096774, micro.specificity!!, 1e-8) },
            Executable { assertEquals(0.34879284, micro.phiCoefficient!!, 1e-8) },
            Executable { assertEquals(0.87669552, micro.phiCoefficientMax!!, 1e-8) },
            Executable { assertEquals(0.39784946, micro.phiOverPhiMax!!, 1e-8) }
        )
    }

    @Test
    fun microAverageIsRecalculatedFromPooledCountsTest() {
        val aggregation = aggregate()
        val pooled = calculator.calculateMetrics(setOf("a", "b", "c", "d", "e"), setOf("a", "b", "c", "d", "e"), null, betas)
        assertAll(
            // Not equal to any (weighted) mean of the single results.
            Executable { assertNotEquals(aggregation.macroAverage.precision, aggregation.microAverage.precision) },
            Executable { assertNotEquals(aggregation.weightedAverage.precision, aggregation.microAverage.precision) },
            // Precision and recall of the micro average are exactly those of the pooled confusion matrix.
            Executable { assertEquals(5.0 / 9.0, aggregation.microAverage.precision, 1e-12) },
            Executable { assertEquals(5.0 / 11.0, aggregation.microAverage.recall, 1e-12) },
            Executable { assertEquals(1.0, pooled.precision, 1e-12) }
        )
    }

    @Test
    fun pooledConfusionMatrixIsTheSameForEveryAggregationTest() {
        val aggregation = aggregate()
        val expected = ConfusionMatrix(5, 4, 6, 27)
        assertAll(
            Executable { assertEquals(expected, aggregation.confusionMatrix) },
            Executable { assertEquals(expected, aggregation.macroAverage.confusionMatrix) },
            Executable { assertEquals(expected, aggregation.weightedAverage.confusionMatrix) },
            Executable { assertEquals(expected, aggregation.microAverage.confusionMatrix) },
            Executable { assertEquals(42, aggregation.confusionMatrix.total()) }
        )
    }

    @Test
    fun aggregatedFBetaIsTheMeanOfTheSingleFBetasTest() {
        // The central semantic guarantee: macro and weighted F-beta average the per-result F-beta scores. Recalculating F-beta from the averaged
        // precision and recall would give a substantially different (and wrong) number.
        val aggregation = aggregate()
        val macro = aggregation.macroAverage
        val singles = aggregation.singleResults
        assertAll(
            Executable { assertEquals(singles.map { it.f1 }.average(), macro.f1, 1e-12) },
            Executable { assertEquals(singles.map { it.fbeta(2.0) }.average(), macro.fbetaScores.getValue(2.0), 1e-12) },
            Executable { assertEquals(singles.map { it.fbeta(0.5) }.average(), macro.fbetaScores.getValue(0.5), 1e-12) },
            Executable {
                val f1FromAveragedPrecisionAndRecall = calculateFBeta(macro.precision, macro.recall, 1.0)
                assertTrue(abs(f1FromAveragedPrecisionAndRecall - macro.f1) > 0.1) {
                    "Fixture is not discriminating: $f1FromAveragedPrecisionAndRecall vs ${macro.f1}"
                }
            },
            Executable {
                val f2FromAveragedPrecisionAndRecall = calculateFBeta(macro.precision, macro.recall, 2.0)
                assertTrue(abs(f2FromAveragedPrecisionAndRecall - macro.fbetaScores.getValue(2.0)) > 0.1) {
                    "Fixture is not discriminating: $f2FromAveragedPrecisionAndRecall vs ${macro.fbetaScores.getValue(2.0)}"
                }
            }
        )
    }

    @Test
    fun singleResultAggregatesToItselfTest() {
        val single = calculator.calculateMetrics(setOf("a", "b", "c"), setOf("a", "b", "d", "e"), 10, betas)
        val aggregation = calculator.calculateAverages(listOf(single))
        assertAll(
            aggregation.asList().flatMap { aggregated ->
                listOf(
                    Executable { assertEquals(single.precision, aggregated.precision, 1e-12) },
                    Executable { assertEquals(single.recall, aggregated.recall, 1e-12) },
                    Executable { assertEquals(single.f1, aggregated.f1, 1e-12) },
                    Executable { assertEquals(single.fbetaScores, aggregated.fbetaScores) },
                    Executable { assertEquals(single.accuracy!!, aggregated.accuracy!!, 1e-12) },
                    Executable { assertEquals(single.specificity!!, aggregated.specificity!!, 1e-12) },
                    Executable { assertEquals(single.phiCoefficient!!, aggregated.phiCoefficient!!, 1e-12) },
                    Executable { assertEquals(single.confusionMatrix, aggregated.confusionMatrix) }
                )
            }
        )
    }

    @Test
    fun identicalResultsAggregateToThemselvesTest() {
        val single = calculator.calculateMetrics(setOf("a", "b", "c"), setOf("a", "b", "d", "e"), 10, betas)
        val aggregation = calculator.calculateAverages(listOf(single, single, single))
        assertAll(
            Executable { assertEquals(single.precision, aggregation.macroAverage.precision, 1e-12) },
            Executable { assertEquals(single.precision, aggregation.weightedAverage.precision, 1e-12) },
            Executable { assertEquals(single.precision, aggregation.microAverage.precision, 1e-12) },
            Executable { assertEquals(single.f1, aggregation.macroAverage.f1, 1e-12) },
            Executable { assertEquals(single.f1, aggregation.weightedAverage.f1, 1e-12) },
            Executable { assertEquals(single.f1, aggregation.microAverage.f1, 1e-12) },
            Executable { assertEquals(ConfusionMatrix(6, 3, 6, 15), aggregation.confusionMatrix) }
        )
    }

    @Test
    fun macroEqualsWeightedForEqualWeightsTest() {
        val aggregation = aggregate(weights = listOf(3, 3, 3))
        assertAll(
            Executable { assertEquals(aggregation.macroAverage.precision, aggregation.weightedAverage.precision, 1e-12) },
            Executable { assertEquals(aggregation.macroAverage.recall, aggregation.weightedAverage.recall, 1e-12) },
            Executable { assertEquals(aggregation.macroAverage.fbetaScores.keys, aggregation.weightedAverage.fbetaScores.keys) },
            Executable {
                assertAll(
                    aggregation.macroAverage.fbetaScores.map { (beta, score) ->
                        Executable { assertEquals(score, aggregation.weightedAverage.fbetaScores.getValue(beta), 1e-12) }
                    }
                )
            },
            Executable { assertEquals(aggregation.macroAverage.accuracy!!, aggregation.weightedAverage.accuracy!!, 1e-12) }
        )
    }

    @Test
    fun withoutTrueNegativesTest() {
        val aggregation = calculator.calculateAverages(results(confusionMatrixSums = false))
        assertAll(
            aggregation.asList().flatMap { aggregated ->
                listOf(
                    Executable { assertNull(aggregated.accuracy) },
                    Executable { assertNull(aggregated.specificity) },
                    Executable { assertNull(aggregated.phiCoefficient) },
                    Executable { assertNull(aggregated.phiCoefficientMax) },
                    Executable { assertNull(aggregated.phiOverPhiMax) },
                    Executable { assertNull(aggregated.confusionMatrix.trueNegatives) }
                )
            } +
                listOf(
                    // Precision, recall and the F-beta scores are unaffected.
                    Executable { assertEquals(0.63888889, aggregation.macroAverage.precision, 1e-8) },
                    Executable { assertEquals(0.49047619, aggregation.macroAverage.f1, 1e-8) },
                    Executable { assertEquals(ConfusionMatrix(5, 4, 6, null), aggregation.confusionMatrix) }
                )
        )
    }

    @Test
    fun betasAreDerivedFromTheSingleResultsTest() {
        val aggregation = aggregate()
        assertAll(
            Executable { assertEquals(setOf(0.5, 1.0, 2.0), aggregation.betas) },
            Executable {
                assertEquals(
                    listOf(0.5, 1.0, 2.0),
                    aggregation.macroAverage.fbetaScores.keys
                        .toList()
                )
            },
            Executable {
                assertEquals(
                    listOf(0.5, 1.0, 2.0),
                    aggregation.weightedAverage.fbetaScores.keys
                        .toList()
                )
            },
            Executable {
                assertEquals(
                    listOf(0.5, 1.0, 2.0),
                    aggregation.microAverage.fbetaScores.keys
                        .toList()
                )
            }
        )
    }

    @Test
    fun betasAreUnitedAcrossDisagreeingSingleResultsTest() {
        val first = calculator.calculateMetrics(setOf("a", "b"), setOf("a", "c"), 8, listOf(0.5))
        val second = calculator.calculateMetrics(setOf("d", "e"), setOf("d"), 8, listOf(3.0))
        val aggregation = calculator.calculateAverages(listOf(first, second))
        assertAll(
            Executable { assertEquals(setOf(0.5, 1.0, 3.0), aggregation.betas) },
            // The beta missing from a single result is recalculated from its own precision and recall.
            Executable {
                assertEquals(listOf(first.fbeta(3.0), second.fbeta(3.0)).average(), aggregation.macroAverage.fbetaScores.getValue(3.0), 1e-12)
            }
        )
    }

    @Test
    fun explicitBetasMayRequestUnstoredBetasTest() {
        val aggregation = aggregate(betaOverride = listOf(4.0))
        val singles = results()
        assertAll(
            Executable { assertEquals(setOf(1.0, 4.0), aggregation.betas) },
            Executable { assertEquals(singles.map { it.fbeta(4.0) }.average(), aggregation.macroAverage.fbetaScores.getValue(4.0), 1e-12) },
            Executable { assertEquals(calculateFBeta(5.0 / 9.0, 5.0 / 11.0, 4.0), aggregation.microAverage.fbetaScores.getValue(4.0), 1e-12) }
        )
    }

    @ParameterizedTest
    @EnumSource(AggregationType::class)
    fun getReturnsTheNamedAggregationTest(type: AggregationType) {
        val aggregation = aggregate()
        val byType = aggregation[type]
        val byName =
            when (type) {
                AggregationType.MACRO_AVERAGE -> aggregation.macroAverage
                AggregationType.WEIGHTED_AVERAGE -> aggregation.weightedAverage
                AggregationType.MICRO_AVERAGE -> aggregation.microAverage
            }
        assertAll(
            Executable { assertSame(byName, byType) },
            Executable { assertEquals(type, byType.type) }
        )
    }

    @Test
    fun asListTest() {
        val aggregation = aggregate()
        assertAll(
            Executable { assertEquals(3, aggregation.asList().size) },
            Executable {
                assertEquals(
                    listOf(AggregationType.MACRO_AVERAGE, AggregationType.WEIGHTED_AVERAGE, AggregationType.MICRO_AVERAGE),
                    aggregation.asList().map { it.type }
                )
            },
            Executable { assertEquals(aggregation.asList(), AggregationType.entries.map { aggregation[it] }) }
        )
    }

    @Test
    fun unionsOfClassifiedElementsTest() {
        val aggregation = aggregate()
        assertAll(
            Executable { assertEquals(setOf("a", "b", "f", "j", "k"), aggregation.truePositives()) },
            Executable { assertEquals(setOf("c", "g", "h", "i"), aggregation.falsePositives()) },
            Executable { assertEquals(setOf("d", "e", "l", "m", "n", "o"), aggregation.falseNegatives()) },
            // The unions match the pooled counts because the fixture has no overlapping elements.
            Executable { assertEquals(aggregation.confusionMatrix.truePositives, aggregation.truePositives().size) },
            Executable { assertEquals(aggregation.confusionMatrix.falsePositives, aggregation.falsePositives().size) },
            Executable { assertEquals(aggregation.confusionMatrix.falseNegatives, aggregation.falseNegatives().size) }
        )
    }

    @Test
    fun spreadTest() {
        val aggregation = aggregate()
        assertAll(
            Executable { assertSpread(MetricSpread(0.25, 1.0, 0.63888889, 0.30681558), aggregation.spread(ClassificationMetric.PRECISION)) },
            Executable { assertSpread(MetricSpread(0.33333333, 1.0, 0.61111111, 0.28327886), aggregation.spread(ClassificationMetric.RECALL)) },
            Executable { assertSpread(MetricSpread(0.4, 0.57142857, 0.49047619, 0.07030868), aggregation.spread(ClassificationMetric.F1)) },
            Executable { assertSpread(MetricSpread(0.7, 0.8, 0.75, 0.04082483), aggregation.spread(ClassificationMetric.ACCURACY)) },
            Executable { assertSpread(MetricSpread(0.38461538, 0.625, 0.51197706, 0.09865898), aggregation.fbetaSpread(2.0)) },
            // The mean of the spread is the macro average of that metric.
            Executable { assertEquals(aggregation.macroAverage.precision, aggregation.spread(ClassificationMetric.PRECISION)!!.mean, 1e-12) },
            Executable { assertEquals(aggregation.macroAverage.f1, aggregation.spread(ClassificationMetric.F1)!!.mean, 1e-12) },
            Executable { assertEquals(aggregation.macroAverage.fbetaScores.getValue(2.0), aggregation.fbetaSpread(2.0).mean, 1e-12) }
        )
    }

    @ParameterizedTest
    @EnumSource(ClassificationMetric::class)
    fun spreadIsAvailableForEveryMetricTest(metric: ClassificationMetric) {
        val spread = aggregate().spread(metric)!!
        assertAll(
            Executable { assertTrue(spread.min <= spread.mean) { "min ${spread.min} must not exceed mean ${spread.mean}" } },
            Executable { assertTrue(spread.mean <= spread.max) { "mean ${spread.mean} must not exceed max ${spread.max}" } },
            Executable { assertTrue(spread.standardDeviation >= 0.0) { "standard deviation must not be negative" } }
        )
    }

    @ParameterizedTest
    @EnumSource(
        value = ClassificationMetric::class,
        names = ["ACCURACY", "SPECIFICITY", "PHI_COEFFICIENT", "PHI_COEFFICIENT_MAX", "PHI_OVER_PHI_MAX"]
    )
    fun spreadIsNullWithoutTrueNegativesTest(metric: ClassificationMetric) {
        assertNull(calculator.calculateAverages(results(confusionMatrixSums = false)).spread(metric))
    }

    @Test
    fun spreadOfSingleResultTest() {
        val single = calculator.calculateMetrics(setOf("a", "b", "c"), setOf("a", "b", "d", "e"), 10, betas)
        val spread = calculator.calculateAverages(listOf(single)).spread(ClassificationMetric.PRECISION)!!
        assertAll(
            Executable { assertEquals(single.precision, spread.min, 1e-12) },
            Executable { assertEquals(single.precision, spread.max, 1e-12) },
            Executable { assertEquals(single.precision, spread.mean, 1e-12) },
            Executable { assertEquals(0.0, spread.standardDeviation, 1e-12) }
        )
    }

    @Test
    fun emptyResultListIsRejectedTest() {
        assertThrows<IllegalArgumentException> { calculator.calculateAverages(emptyList<SingleClassificationResult<String>>()) }
    }

    @Test
    fun mixedTrueNegativesAreRejectedTest() {
        val withTrueNegatives = calculator.calculateMetrics(setOf("a"), setOf("a"), 4)
        val withoutTrueNegatives = calculator.calculateMetrics(setOf("b"), setOf("b"), null)
        assertAll(
            Executable { assertThrows<IllegalArgumentException> { calculator.calculateAverages(listOf(withTrueNegatives, withoutTrueNegatives)) } },
            Executable { assertThrows<IllegalArgumentException> { calculator.calculateAverages(listOf(withoutTrueNegatives, withTrueNegatives)) } }
        )
    }

    @Test
    fun mismatchedWeightsAreRejectedTest() {
        assertAll(
            Executable { assertThrows<IllegalArgumentException> { aggregate(weights = listOf(1, 2)) } },
            Executable { assertThrows<IllegalArgumentException> { aggregate(weights = listOf(1, 2, 3, 4)) } },
            Executable { assertThrows<IllegalArgumentException> { aggregate(weights = emptyList()) } }
        )
    }

    @Test
    fun unusableWeightsAreRejectedTest() {
        // A weighted mean divides by the sum of the weights, so an all-zero or negative set of weights would produce NaN or out-of-range metrics.
        assertAll(
            Executable { assertThrows<IllegalArgumentException> { aggregate(weights = listOf(0, 0, 0)) } },
            Executable { assertThrows<IllegalArgumentException> { aggregate(weights = listOf(-1, 2, 3)) } },
            Executable { assertThrows<IllegalArgumentException> { aggregate(weights = listOf(1, -1, 0)) } },
            // A single positive weight is enough.
            Executable { assertDoesNotThrow { aggregate(weights = listOf(0, 0, 1)) } }
        )
    }

    @Test
    fun emptyGroundTruthsMakeTheDefaultWeightsUnusableTest() {
        // The default weight of a result is the size of its ground truth, so results without a ground truth would all weigh 0.
        val empty = calculator.calculateMetrics(emptySet<String>(), emptySet(), 10)
        val thrown = assertThrows<IllegalArgumentException> { calculator.calculateAverages(listOf(empty, empty)) }
        assertAll(
            Executable { assertTrue(thrown.message!!.contains("ground truth")) { "the message should explain the cause: ${thrown.message}" } },
            // Explicit weights make it well-defined again.
            Executable { assertDoesNotThrow { calculator.calculateAverages(listOf(empty, empty), listOf(1, 1)) } }
        )
    }

    @Test
    fun invalidBetaOverrideIsRejectedTest() {
        assertAll(
            Executable { assertThrows<IllegalArgumentException> { aggregate(betaOverride = listOf(0.0)) } },
            Executable { assertThrows<IllegalArgumentException> { aggregate(betaOverride = listOf(-2.0)) } },
            Executable { assertThrows<IllegalArgumentException> { aggregate(betaOverride = listOf(Double.NaN)) } }
        )
    }

    @Test
    fun containerRejectsMismatchedAggregationTypesTest() {
        val aggregation = aggregate()
        assertAll(
            Executable {
                assertThrows<IllegalArgumentException> {
                    ClassificationAggregationResult(
                        aggregation.singleResults,
                        aggregation.weights,
                        aggregation.weightedAverage,
                        aggregation.weightedAverage,
                        aggregation.microAverage
                    )
                }
            },
            Executable {
                assertThrows<IllegalArgumentException> {
                    ClassificationAggregationResult(
                        aggregation.singleResults,
                        aggregation.weights,
                        aggregation.macroAverage,
                        aggregation.microAverage,
                        aggregation.microAverage
                    )
                }
            },
            Executable {
                assertThrows<IllegalArgumentException> {
                    ClassificationAggregationResult(
                        aggregation.singleResults,
                        aggregation.weights,
                        aggregation.macroAverage,
                        aggregation.weightedAverage,
                        aggregation.macroAverage
                    )
                }
            },
            Executable {
                assertThrows<IllegalArgumentException> {
                    ClassificationAggregationResult(
                        aggregation.singleResults,
                        listOf(1),
                        aggregation.macroAverage,
                        aggregation.weightedAverage,
                        aggregation.microAverage
                    )
                }
            }
        )
    }

    @Test
    fun prettyPrintDoesNotThrowTest() {
        assertAll(
            Executable { assertDoesNotThrow { aggregate().prettyPrint() } },
            Executable { assertDoesNotThrow { calculator.calculateAverages(results(confusionMatrixSums = false)).prettyPrint() } },
            Executable { assertDoesNotThrow { calculator.calculateAverages(results()).prettyPrint() } },
            Executable { assertDoesNotThrow { aggregate().asList().forEach { it.prettyPrint() } } }
        )
    }

    private fun assertSpread(
        expected: MetricSpread,
        actual: MetricSpread?
    ) {
        requireNotNull(actual) { "expected a spread but got null" }
        assertAll(
            Executable { assertEquals(expected.min, actual.min, 1e-8) },
            Executable { assertEquals(expected.max, actual.max, 1e-8) },
            Executable { assertEquals(expected.mean, actual.mean, 1e-8) },
            Executable { assertEquals(expected.standardDeviation, actual.standardDeviation, 1e-8) }
        )
    }
}
