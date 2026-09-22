package edu.kit.kastel.mcse.ardoco.metrics.result

/**
 * The metrics of a [ClassificationResult] that can be selected programmatically, e.g. to inspect their spread across the results of an aggregation.
 *
 * The F-beta scores are not part of this enum because they are parameterised by beta; use [ClassificationAggregationResult.fbetaSpread] instead.
 */
enum class ClassificationMetric {
    /** See [ClassificationResult.precision]. */
    PRECISION,

    /** See [ClassificationResult.recall]. */
    RECALL,

    /** See [ClassificationResult.f1]. */
    F1,

    /** See [ClassificationResult.accuracy]. */
    ACCURACY,

    /** See [ClassificationResult.specificity]. */
    SPECIFICITY,

    /** See [ClassificationResult.phiCoefficient]. */
    PHI_COEFFICIENT,

    /** See [ClassificationResult.phiCoefficientMax]. */
    PHI_COEFFICIENT_MAX,

    /** See [ClassificationResult.phiOverPhiMax]. */
    PHI_OVER_PHI_MAX;

    /**
     * Returns the value of this metric in [result], or null if the metric is not available for that result.
     *
     * @param result the result to read the metric from
     * @return the value of this metric, or null if it is not available
     */
    fun valueIn(result: ClassificationResult): Double? =
        when (this) {
            PRECISION -> result.precision
            RECALL -> result.recall
            F1 -> result.f1
            ACCURACY -> result.accuracy
            SPECIFICITY -> result.specificity
            PHI_COEFFICIENT -> result.phiCoefficient
            PHI_COEFFICIENT_MAX -> result.phiCoefficientMax
            PHI_OVER_PHI_MAX -> result.phiOverPhiMax
        }
}
