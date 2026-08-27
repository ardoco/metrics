/* Licensed under MIT 2026. */
package edu.kit.kastel.mcse.ardoco.metrics;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import edu.kit.kastel.mcse.ardoco.metrics.result.AggregationType;
import edu.kit.kastel.mcse.ardoco.metrics.result.ClassificationAggregationResult;
import edu.kit.kastel.mcse.ardoco.metrics.result.ClassificationMetric;
import edu.kit.kastel.mcse.ardoco.metrics.result.ConfusionMatrix;
import edu.kit.kastel.mcse.ardoco.metrics.result.MetricSpread;
import edu.kit.kastel.mcse.ardoco.metrics.result.SingleClassificationResult;

/**
 * Pins the API shape that the wiki documents for Java callers. Kotlin default arguments are invisible from Java and {@code @JvmOverloads} is not
 * allowed on interface members, so every variant has to exist as its own overload. This test fails to compile if one of them disappears.
 */
class JavaInteropTest {
    private final ClassificationMetricsCalculator calculator = ClassificationMetricsCalculator.getInstance();

    @Test
    void singleResultTest() {
        SingleClassificationResult<String> result = calculator.calculateMetrics(Set.of("A", "B", "C", "D", "E"), Set.of("A", "B"), 20, List.of(0.5, 2.0));

        assertAll( //
                () -> assertEquals(0.4, result.getPrecision(), 1e-12), //
                () -> assertEquals(1.0, result.getRecall(), 1e-12), //
                () -> assertEquals(result.getFbetaScores().get(1.0), result.getF1()), //
                () -> assertEquals(Set.of(0.5, 1.0, 2.0), result.getFbetaScores().keySet()), //
                () -> assertEquals(0.8695652173913044, result.fbeta(3.0), 1e-12), //
                () -> assertNull(result.fbetaOrNull(3.0)), //
                () -> assertEquals(new ConfusionMatrix(2, 3, 0, 15), result.getConfusionMatrix()), //
                () -> assertEquals(Integer.valueOf(20), result.getConfusionMatrix().total()));
    }

    @Test
    void defaultBetasTest() {
        assertAll( //
                () -> assertEquals(Set.of(1.0), ClassificationMetricsCalculator.getDefaultBetas()), //
                // The three-argument overload has to stay reachable from Java, where the Kotlin default is not visible.
                () -> assertEquals(Set.of(1.0), calculator.calculateMetrics(Set.of("A"), Set.of("A"), (Integer) null).getFbetaScores().keySet()));
    }

    @Test
    void stringProviderTest() {
        SingleClassificationResult<String> result = calculator.calculateMetrics(Set.of(1, 2, 3), Set.of(1, 4), String::valueOf, null);
        assertAll( //
                () -> assertEquals(Set.of("1"), result.getTruePositives()), //
                () -> assertEquals(Set.of("2", "3"), result.getFalsePositives()), //
                () -> assertEquals(Set.of("4"), result.getFalseNegatives()));
    }

    @Test
    void aggregationTest() {
        SingleClassificationResult<String> first = calculator.calculateMetrics(Set.of("A", "B", "C", "D", "E"), Set.of("A", "B"), 20, List.of(0.5, 2.0));
        SingleClassificationResult<String> second = calculator.calculateMetrics(Set.of("F"), Set.of("F", "G", "H"), 20, List.of(0.5, 2.0));

        ClassificationAggregationResult<String> aggregation = calculator.calculateAverages(List.of(first, second), null, null);

        assertAll( //
                () -> assertEquals(0.5357142857142858, aggregation.getMacroAverage().getF1(), 1e-12), //
                () -> assertEquals(0.76, aggregation.getWeightedAverage().getPrecision(), 1e-12), //
                () -> assertEquals(0.5769230769230769, aggregation.getMicroAverage().getFbetaScores().get(2.0), 1e-12), //
                () -> assertSame(aggregation.getMicroAverage(), aggregation.get(AggregationType.MICRO_AVERAGE)), //
                () -> assertEquals(new ConfusionMatrix(3, 3, 2, 32), aggregation.getConfusionMatrix()), //
                () -> assertEquals(Set.of("A", "B", "F"), aggregation.truePositives()), //
                () -> assertEquals(Set.of("C", "D", "E"), aggregation.falsePositives()), //
                () -> assertEquals(Set.of("G", "H"), aggregation.falseNegatives()), //
                () -> assertEquals(new MetricSpread(0.4, 1.0, 0.7, 0.3), aggregation.spread(ClassificationMetric.PRECISION)), //
                () -> assertEquals(0.5769230769230769, aggregation.fbetaSpread(2.0).getMean(), 1e-12), //
                () -> assertEquals(3, aggregation.asList().size()), //
                () -> assertEquals(Set.of(0.5, 1.0, 2.0), aggregation.getBetas()), //
                () -> assertEquals(List.of(2, 3), aggregation.getWeights()), //
                () -> assertEquals(2, aggregation.getSingleResults().size()));
    }

    @Test
    void aggregationOverloadsTest() {
        SingleClassificationResult<String> first = calculator.calculateMetrics(Set.of("A", "B"), Set.of("A", "C"), 8);
        SingleClassificationResult<String> second = calculator.calculateMetrics(Set.of("D"), Set.of("D", "E"), 8);

        assertAll( //
                () -> assertEquals(0.75, calculator.calculateAverages(List.of(first, second)).getMacroAverage().getPrecision(), 1e-12), //
                () -> assertEquals(0.75, calculator.calculateAverages(List.of(first, second), List.of(1, 1)).getWeightedAverage().getPrecision(), 1e-12), //
                () -> assertEquals(0.75, calculator.calculateAverages(List.of(first, second), List.of(1, 1), List.of(2.0)).getWeightedAverage().getPrecision(),
                        1e-12));
    }
}
