package edu.kit.kastel.mcse.ardoco.metrics.result

import edu.kit.kastel.mcse.ardoco.metrics.result.ClassificationResult.Companion.logger

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
    /** The true negatives. If not available, this is null. */
    val trueNegatives: Int?,
    override val precision: Double,
    override val recall: Double,
    override val f1: Double,
    // Only if tn is available
    override val accuracy: Double?,
    override val specificity: Double?,
    override val phiCoefficient: Double?,
    override val phiCoefficientMax: Double?,
    override val phiOverPhiMax: Double?
) : ClassificationResult {
    override fun prettyPrint() {
        logger.info("True Positives: ${truePositives.size}")
        logger.info("False Positives: ${falsePositives.size}")
        logger.info("False Negatives: ${falseNegatives.size}")
        logger.info("True Negatives: ${trueNegatives ?: "N/A"}")
        super.prettyPrint()
    }

    /** Calculates the F-beta score of the classification. */
    fun fBeta(beta: Double): Double {
        if (beta == 0.0) {
            error("Beta cannot be zero for F-beta score.")
        }

        if (beta == 1.0) return f1

        val betaSquared = beta * beta
        return (1 + betaSquared) * (precision * recall) / ((betaSquared * precision) + recall)
    }
}
