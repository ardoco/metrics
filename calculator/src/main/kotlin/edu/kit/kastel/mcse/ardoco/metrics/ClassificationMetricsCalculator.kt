package edu.kit.kastel.mcse.ardoco.metrics

import edu.kit.kastel.mcse.ardoco.metrics.internal.ClassificationMetricsCalculatorImpl
import edu.kit.kastel.mcse.ardoco.metrics.result.ClassificationAggregationResult
import edu.kit.kastel.mcse.ardoco.metrics.result.SingleClassificationResult

/**
 * Interface for calculating classification metrics.
 */
interface ClassificationMetricsCalculator {
    companion object {
        /** A default instance of the classification metrics calculator. */
        @JvmStatic
        val Instance: ClassificationMetricsCalculator = ClassificationMetricsCalculatorImpl()

        /** The betas of the F-beta scores that are calculated if no betas are requested explicitly. */
        @JvmStatic
        val DefaultBetas: Set<Double> = setOf(1.0)
    }

    /**
     * Calculates the metrics for the given classification, including only the F1-score.
     * @param T the type of classified elements
     * @param classification the classification
     * @param groundTruth the ground truth
     * @param confusionMatrixSum the sum of the confusion matrix. If not provided, some metrics can't be calculated.
     * @return the classification result
     */
    fun <T> calculateMetrics(
        classification: Set<T>,
        groundTruth: Set<T>,
        confusionMatrixSum: Int?
    ): SingleClassificationResult<T> = calculateMetrics(classification, groundTruth, confusionMatrixSum, DefaultBetas)

    /**
     * Calculates the metrics for the given classification.
     * @param T the type of classified elements
     * @param classification the classification
     * @param groundTruth the ground truth
     * @param confusionMatrixSum the sum of the confusion matrix. If not provided, some metrics can't be calculated.
     * @param betas the betas of the F-beta scores to calculate. Beta 1.0 is always calculated. Each beta must be finite and greater than 0.
     * @return the classification result
     */
    fun <T> calculateMetrics(
        classification: Set<T>,
        groundTruth: Set<T>,
        confusionMatrixSum: Int?,
        betas: Collection<Double>
    ): SingleClassificationResult<T>

    /**
     * Calculates the metrics for the given classification, including only the F1-score.
     * @param T the type of classified elements
     * @param classification the classification
     * @param groundTruth the ground truth
     * @param stringProvider a function to convert the classification and ground truth to strings
     * @param confusionMatrixSum the sum of the confusion matrix. If not provided, some metrics can't be calculated.
     * @return the classification result
     */
    fun <T> calculateMetrics(
        classification: Set<T>,
        groundTruth: Set<T>,
        stringProvider: (T) -> String,
        confusionMatrixSum: Int?
    ): SingleClassificationResult<String> = calculateMetrics(classification, groundTruth, stringProvider, confusionMatrixSum, DefaultBetas)

    /**
     * Calculates the metrics for the given classification.
     * @param T the type of classified elements
     * @param classification the classification
     * @param groundTruth the ground truth
     * @param stringProvider a function to convert the classification and ground truth to strings
     * @param confusionMatrixSum the sum of the confusion matrix. If not provided, some metrics can't be calculated.
     * @param betas the betas of the F-beta scores to calculate. Beta 1.0 is always calculated. Each beta must be finite and greater than 0.
     * @return the classification result
     */
    fun <T> calculateMetrics(
        classification: Set<T>,
        groundTruth: Set<T>,
        stringProvider: (T) -> String,
        confusionMatrixSum: Int?,
        betas: Collection<Double>
    ): SingleClassificationResult<String> =
        calculateMetrics(
            classification.map { stringProvider(it) }.toSet(),
            groundTruth.map { stringProvider(it) }.toSet(),
            confusionMatrixSum,
            betas
        )

    /**
     * Aggregates the given classification results, weighting each result by the size of its ground truth.
     * @param T the type of classified elements
     * @param singleClassificationResults the classification results
     * @return the aggregation of the classification results
     */
    fun <T> calculateAverages(singleClassificationResults: List<SingleClassificationResult<out T>>): ClassificationAggregationResult<T> =
        calculateAverages(singleClassificationResults, null)

    /**
     * Aggregates the given classification results.
     * @param T the type of classified elements
     * @param singleClassificationResults the classification results
     * @param weights the weights for the classification results. If not provided, the size of the gold standard is used as weight. There must be
     *                exactly one weight per result.
     * @return the aggregation of the classification results
     */
    fun <T> calculateAverages(
        singleClassificationResults: List<SingleClassificationResult<out T>>,
        weights: List<Int>?
    ): ClassificationAggregationResult<T> = calculateAverages(singleClassificationResults, weights, null)

    /**
     * Aggregates the given classification results.
     *
     * The macro and the weighted average of an F-beta score are the (weighted) mean of the F-beta scores of the single results, whereas the micro
     * average is recalculated from the pooled confusion matrix.
     *
     * @param T the type of classified elements
     * @param singleClassificationResults the classification results
     * @param weights the weights for the classification results. If not provided, the size of the gold standard is used as weight. There must be
     *                exactly one weight per result.
     * @param betas the betas of the F-beta scores to aggregate. If not provided, all betas of the given results are used. Beta 1.0 is always
     *              included. A beta that is missing from a single result is recalculated from its precision and recall.
     * @return the aggregation of the classification results
     */
    fun <T> calculateAverages(
        singleClassificationResults: List<SingleClassificationResult<out T>>,
        weights: List<Int>?,
        betas: Collection<Double>?
    ): ClassificationAggregationResult<T>
}
