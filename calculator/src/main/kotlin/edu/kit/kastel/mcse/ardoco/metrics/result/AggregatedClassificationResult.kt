package edu.kit.kastel.mcse.ardoco.metrics.result

import edu.kit.kastel.mcse.ardoco.metrics.result.ClassificationResult.Companion.logger

/**
 * Represents one aggregation of multiple classification results.
 *
 * This type only carries the aggregated values; the results it was calculated from are held once by the surrounding
 * [ClassificationAggregationResult].
 */
data class AggregatedClassificationResult(
    /** The type of aggregation */
    val type: AggregationType,
    /** The confusion matrix pooled over all aggregated results */
    override val confusionMatrix: ConfusionMatrix,
    override val precision: Double,
    override val recall: Double,
    override val f1: Double,
    override val fbetaScores: Map<Double, Double> = mapOf(1.0 to f1),
    override val accuracy: Double?,
    override val specificity: Double?,
    override val phiCoefficient: Double?,
    override val phiCoefficientMax: Double?,
    override val phiOverPhiMax: Double?
) : ClassificationResult {
    init {
        require(fbetaScores.containsKey(1.0)) { "The F-beta scores must contain the F1-score (beta 1.0)" }
    }

    override fun prettyPrint() {
        logger.info("Type: $type")
        super.prettyPrint()
    }
}
