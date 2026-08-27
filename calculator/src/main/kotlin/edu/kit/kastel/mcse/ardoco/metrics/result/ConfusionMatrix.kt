package edu.kit.kastel.mcse.ardoco.metrics.result

/**
 * The four counts of a binary confusion matrix.
 *
 * @param truePositives the number of true positives
 * @param falsePositives the number of false positives
 * @param falseNegatives the number of false negatives
 * @param trueNegatives the number of true negatives, or null if it is unknown
 */
data class ConfusionMatrix(
    /** The number of true positives. */
    val truePositives: Int,
    /** The number of false positives. */
    val falsePositives: Int,
    /** The number of false negatives. */
    val falseNegatives: Int,
    /** The number of true negatives. If no confusion matrix sum was provided, this is null. */
    val trueNegatives: Int?
) {
    init {
        require(truePositives >= 0) { "The number of true positives must not be negative but was $truePositives" }
        require(falsePositives >= 0) { "The number of false positives must not be negative but was $falsePositives" }
        require(falseNegatives >= 0) { "The number of false negatives must not be negative but was $falseNegatives" }
        require(trueNegatives == null || trueNegatives >= 0) { "The number of true negatives must not be negative but was $trueNegatives" }
    }

    /**
     * Returns the total number of classified elements, or null if [trueNegatives] is unknown.
     *
     * This is intentionally a function and not a property so that the four counts stay the only serialized state of this type.
     *
     * @return the total number of classified elements, or null if it is unknown
     */
    fun total(): Int? = trueNegatives?.let { truePositives + falsePositives + falseNegatives + it }

    /**
     * Sums the counts of this matrix and [other]. The result has no number of true negatives if either operand has none.
     *
     * @param other the matrix to add
     * @return the summed confusion matrix
     */
    operator fun plus(other: ConfusionMatrix): ConfusionMatrix =
        ConfusionMatrix(
            truePositives + other.truePositives,
            falsePositives + other.falsePositives,
            falseNegatives + other.falseNegatives,
            if (trueNegatives == null || other.trueNegatives == null) null else trueNegatives + other.trueNegatives
        )
}
