package edu.kit.kastel.mcse.ardoco.metrics.internal

import edu.kit.kastel.mcse.ardoco.metrics.calculation.calculateAccuracy
import edu.kit.kastel.mcse.ardoco.metrics.calculation.calculateFBeta
import edu.kit.kastel.mcse.ardoco.metrics.calculation.calculatePhiCoefficient
import edu.kit.kastel.mcse.ardoco.metrics.calculation.calculatePhiCoefficientMax
import edu.kit.kastel.mcse.ardoco.metrics.calculation.calculatePhiOverPhiMax
import edu.kit.kastel.mcse.ardoco.metrics.calculation.calculatePrecision
import edu.kit.kastel.mcse.ardoco.metrics.calculation.calculateRecall
import edu.kit.kastel.mcse.ardoco.metrics.calculation.calculateSpecificity
import edu.kit.kastel.mcse.ardoco.metrics.result.AggregatedClassificationResult
import edu.kit.kastel.mcse.ardoco.metrics.result.AggregationType
import edu.kit.kastel.mcse.ardoco.metrics.result.ConfusionMatrix

/**
 * The plain metric values of a classification, without any information about where they came from. Used as the carrier between calculating the metrics
 * and building the public result types.
 */
internal data class ClassificationMetricValues(
    val precision: Double,
    val recall: Double,
    val fbetaScores: Map<Double, Double>,
    val accuracy: Double?,
    val specificity: Double?,
    val phiCoefficient: Double?,
    val phiCoefficientMax: Double?,
    val phiOverPhiMax: Double?
) {
    val f1: Double get() = fbetaScores.getValue(1.0)
}

/**
 * Validates the requested betas and normalises them to a distinct, ascending list that always contains 1.0.
 *
 * @throws IllegalArgumentException if any beta is not a finite number greater than 0
 */
internal fun normalizeBetas(betas: Collection<Double>): List<Double> {
    betas.forEach { require(it > 0.0 && it.isFinite()) { "Beta must be a finite number greater than 0 but was $it" } }
    return (betas + 1.0).distinct().sorted()
}

/** Calculates all metrics that the given confusion matrix allows. The metrics requiring true negatives are null if they are unknown. */
internal fun computeMetrics(
    confusionMatrix: ConfusionMatrix,
    betas: List<Double>
): ClassificationMetricValues {
    val tp = confusionMatrix.truePositives
    val fp = confusionMatrix.falsePositives
    val fn = confusionMatrix.falseNegatives

    val precision = calculatePrecision(tp, fp)
    val recall = calculateRecall(tp, fn)
    val fbetaScores = betas.associateWith { calculateFBeta(precision, recall, it) }

    val tn =
        confusionMatrix.trueNegatives
            ?: return ClassificationMetricValues(precision, recall, fbetaScores, null, null, null, null, null)

    return ClassificationMetricValues(
        precision,
        recall,
        fbetaScores,
        calculateAccuracy(tp, fp, fn, tn),
        calculateSpecificity(tn, fp),
        calculatePhiCoefficient(tp, fp, fn, tn),
        calculatePhiCoefficientMax(tp, fp, fn, tn),
        calculatePhiOverPhiMax(tp, fp, fn, tn)
    )
}

/** Builds an aggregated result of the given [type] from these values and the pooled [confusionMatrix]. */
internal fun ClassificationMetricValues.toAggregatedResult(
    type: AggregationType,
    confusionMatrix: ConfusionMatrix
): AggregatedClassificationResult =
    AggregatedClassificationResult(
        type,
        confusionMatrix,
        precision,
        recall,
        f1,
        fbetaScores,
        accuracy,
        specificity,
        phiCoefficient,
        phiCoefficientMax,
        phiOverPhiMax
    )
