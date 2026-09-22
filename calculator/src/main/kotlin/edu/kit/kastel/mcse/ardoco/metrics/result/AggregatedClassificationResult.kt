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
    /**
     * The confusion matrix pooled over all aggregated results. All three aggregation types carry the same one, but only [AggregationType.MICRO_AVERAGE]
     * is calculated from it; for the macro and the weighted average the metrics generally differ from what these counts would give.
     */
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
        require(fbetaScores.getValue(1.0) == f1) {
            "The F-beta score for beta 1.0 (${fbetaScores.getValue(1.0)}) must be the F1-score ($f1)"
        }
    }

    override fun prettyPrint() {
        logger.info("Type: $type")
        super.prettyPrint()
    }
}
