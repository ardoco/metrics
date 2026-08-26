package edu.kit.kastel.mcse.ardoco.metrics.result

import edu.kit.kastel.mcse.ardoco.metrics.calculation.calculateFBeta

/**
 * Represents the result of metrics for one classification task.
 * @param T the type of classified elements
 */
data class SingleClassificationResult<T>(
    /** The true positives */
    val truePositives: Set<T>,
    /** The false positives */
    val falsePositives: Set<T>,
    /** The false negatives */
    val falseNegatives: Set<T>,
    /** The number of true negatives. If not available, this is null. */
    val trueNegatives: Int?,
    override val precision: Double,
    override val recall: Double,
    override val f1: Double,
    override val fbetaScores: Map<Double, Double> = mapOf(1.0 to f1),
    // Only if tn is available
    override val accuracy: Double?,
    override val specificity: Double?,
    override val phiCoefficient: Double?,
    override val phiCoefficientMax: Double?,
    override val phiOverPhiMax: Double?,
    /** The confusion matrix of this result. Defaults to the counts of the classified elements and has to agree with them. */
    override val confusionMatrix: ConfusionMatrix =
        ConfusionMatrix(truePositives.size, falsePositives.size, falseNegatives.size, trueNegatives)
) : ClassificationResult {
    init {
        require(trueNegatives == null || trueNegatives >= 0) { "The number of true negatives must not be negative but was $trueNegatives" }
        require(fbetaScores.containsKey(1.0)) { "The F-beta scores must contain the F1-score (beta 1.0)" }
        require(confusionMatrix == ConfusionMatrix(truePositives.size, falsePositives.size, falseNegatives.size, trueNegatives)) {
            "The confusion matrix $confusionMatrix does not match the classified elements"
        }
    }

    /**
     * Returns the F-beta score for [beta]. If it was not calculated for this result, it is recomputed from [precision] and [recall], which yields the
     * exact same value.
     *
     * @param beta the weight of the recall relative to the precision; must be finite and greater than 0
     * @return the F-beta score
     * @throws IllegalArgumentException if [beta] is not a finite number greater than 0
     */
    fun fbeta(beta: Double): Double = fbetaScores[beta] ?: calculateFBeta(precision, recall, beta)
}
