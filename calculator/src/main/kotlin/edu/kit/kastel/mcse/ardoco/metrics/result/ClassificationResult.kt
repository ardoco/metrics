package edu.kit.kastel.mcse.ardoco.metrics.result

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Represents the result of metrics for a classification task.
 */
interface ClassificationResult {
    companion object {
        @JvmStatic
        val logger: Logger = LoggerFactory.getLogger(ClassificationResult::class.java)
    }

    /**
     * The confusion matrix this result describes: for a single result the counts of its own classified elements, for an aggregation the counts pooled
     * over all aggregated results.
     *
     * The metrics next to it are recomputable from it for a single result and for the micro average. They are **not** for the macro and the weighted
     * average, which are means over the single results: there the matrix describes the underlying data rather than the origin of the values.
     */
    val confusionMatrix: ConfusionMatrix

    /** Precision of the classification. */
    val precision: Double

    /** Recall of the classification. */
    val recall: Double

    /** F1-Score of the classification. Always equal to the entry of [fbetaScores] for beta 1.0. */
    val f1: Double

    /** All calculated F-beta scores of the classification, keyed by beta in ascending order. Always contains the key 1.0. */
    val fbetaScores: Map<Double, Double>

    /** Accuracy of the classification. */
    val accuracy: Double?

    /** Specificity of the classification. */
    val specificity: Double?

    /** Phi coefficient of the classification. */
    val phiCoefficient: Double?

    /** Maximum phi coefficient of the classification. */
    val phiCoefficientMax: Double?

    /**
     * Phi coefficient over maximum phi coefficient of the classification. Within [0, 1] for a non-negative [phiCoefficient]; for a negative one it is
     * not a normalized score and may fall below -1, see `calculatePhiOverPhiMax`.
     */
    val phiOverPhiMax: Double?

    /**
     * Returns the F-beta score for [beta], or null if it was not calculated for this result.
     *
     * @param beta the weight of the recall relative to the precision
     * @return the F-beta score, or null if it was not calculated
     */
    fun fbetaOrNull(beta: Double): Double? = fbetaScores[beta]

    /** Prints the result in a human-readable format to the logger. */
    fun prettyPrint() {
        logger.info("True Positives: ${confusionMatrix.truePositives}")
        logger.info("False Positives: ${confusionMatrix.falsePositives}")
        logger.info("False Negatives: ${confusionMatrix.falseNegatives}")
        logger.info("True Negatives: ${confusionMatrix.trueNegatives ?: "N/A"}")
        logger.info("Precision: $precision")
        logger.info("Recall: $recall")
        for ((beta, score) in fbetaScores) {
            logger.info("F$beta-Score: $score")
        }
        if (accuracy != null) logger.info("Accuracy: $accuracy")
        if (specificity != null) logger.info("Specificity: $specificity")
        if (phiCoefficient != null) logger.info("Phi Coefficient: $phiCoefficient")
        if (phiCoefficientMax != null) logger.info("Phi Coefficient Max: $phiCoefficientMax")
        if (phiOverPhiMax != null) logger.info("Phi over Phi Max: $phiOverPhiMax")
    }
}
