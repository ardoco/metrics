package edu.kit.kastel.mcse.ardoco.metrics.internal

import edu.kit.kastel.mcse.ardoco.metrics.ClassificationMetricsCalculator
import edu.kit.kastel.mcse.ardoco.metrics.result.AggregatedClassificationResult
import edu.kit.kastel.mcse.ardoco.metrics.result.AggregationType
import edu.kit.kastel.mcse.ardoco.metrics.result.ClassificationAggregationResult
import edu.kit.kastel.mcse.ardoco.metrics.result.ConfusionMatrix
import edu.kit.kastel.mcse.ardoco.metrics.result.SingleClassificationResult

internal class ClassificationMetricsCalculatorImpl : ClassificationMetricsCalculator {
    override fun <T> calculateMetrics(
        classification: Set<T>,
        groundTruth: Set<T>,
        confusionMatrixSum: Int?,
        betas: Collection<Double>
    ): SingleClassificationResult<T> {
        val tp = classification.intersect(groundTruth)
        val fp = classification.filter { !groundTruth.contains(it) }.toSet()
        val fn = groundTruth.filter { !classification.contains(it) }.toSet()
        val tn =
            confusionMatrixSum?.let { sum ->
                val trueNegatives = sum - (tp.size + fp.size + fn.size)
                require(trueNegatives >= 0) {
                    "The confusion matrix sum ($sum) must be at least the number of classified and expected elements (${tp.size + fp.size + fn.size})"
                }
                trueNegatives
            }

        val metrics = computeMetrics(ConfusionMatrix(tp.size, fp.size, fn.size, tn), normalizeBetas(betas))
        return SingleClassificationResult(
            tp,
            fp,
            fn,
            tn,
            metrics.precision,
            metrics.recall,
            metrics.f1,
            metrics.fbetaScores,
            metrics.accuracy,
            metrics.specificity,
            metrics.phiCoefficient,
            metrics.phiCoefficientMax,
            metrics.phiOverPhiMax
        )
    }

    override fun <T> calculateAverages(
        singleClassificationResults: List<SingleClassificationResult<out T>>,
        weights: List<Int>?,
        betas: Collection<Double>?
    ): ClassificationAggregationResult<T> {
        validate(singleClassificationResults, weights)

        val weightsForAverage = weights ?: singleClassificationResults.map { it.truePositives.size + it.falseNegatives.size }
        require(weightsForAverage.all { it >= 0 }) { "Weights must not be negative but were $weightsForAverage" }
        require(weightsForAverage.sum() > 0) {
            if (weights == null) {
                "At least one result must have a non-empty ground truth, otherwise all default weights are 0. Provide explicit weights instead."
            } else {
                "At least one weight must be greater than 0 but all weights were 0"
            }
        }
        val betasForAverage = normalizeBetas(betas ?: singleClassificationResults.flatMap { it.fbetaScores.keys })
        val pooledConfusionMatrix = singleClassificationResults.map { it.confusionMatrix }.reduce(ConfusionMatrix::plus)

        val macroAverage =
            weightedAverage(
                singleClassificationResults,
                singleClassificationResults.map { 1 },
                betasForAverage,
                pooledConfusionMatrix,
                AggregationType.MACRO_AVERAGE
            )
        val weightedAverage =
            weightedAverage(
                singleClassificationResults,
                weightsForAverage,
                betasForAverage,
                pooledConfusionMatrix,
                AggregationType.WEIGHTED_AVERAGE
            )
        val microAverage =
            computeMetrics(pooledConfusionMatrix, betasForAverage).toAggregatedResult(AggregationType.MICRO_AVERAGE, pooledConfusionMatrix)

        return ClassificationAggregationResult(
            singleClassificationResults,
            weightsForAverage,
            macroAverage,
            weightedAverage,
            microAverage
        )
    }

    private fun validate(
        singleClassificationResults: List<SingleClassificationResult<*>>,
        weights: List<Int>?
    ) {
        require(singleClassificationResults.isNotEmpty()) { "classificationResults must not be empty" }
        require(
            singleClassificationResults.all {
                (singleClassificationResults[0].trueNegatives == null) == (it.trueNegatives == null)
            }
        ) { "All classificationResults must have either all or no tn" }
        require(weights == null || weights.size == singleClassificationResults.size) {
            "There must be exactly one weight per result but there were ${weights?.size} weights for ${singleClassificationResults.size} results"
        }
    }

    private fun weightedAverage(
        singleClassificationResults: List<SingleClassificationResult<*>>,
        weights: List<Int>,
        betas: List<Double>,
        confusionMatrix: ConfusionMatrix,
        type: AggregationType
    ): AggregatedClassificationResult {
        val hasTrueNegatives = singleClassificationResults[0].trueNegatives != null

        val values =
            ClassificationMetricValues(
                weightedMean(singleClassificationResults.map { it.precision }, weights),
                weightedMean(singleClassificationResults.map { it.recall }, weights),
                betas.associateWith { beta -> weightedMean(singleClassificationResults.map { it.fbeta(beta) }, weights) },
                if (hasTrueNegatives) weightedMean(singleClassificationResults.map { it.accuracy!! }, weights) else null,
                if (hasTrueNegatives) weightedMean(singleClassificationResults.map { it.specificity!! }, weights) else null,
                if (hasTrueNegatives) weightedMean(singleClassificationResults.map { it.phiCoefficient!! }, weights) else null,
                if (hasTrueNegatives) weightedMean(singleClassificationResults.map { it.phiCoefficientMax!! }, weights) else null,
                if (hasTrueNegatives) weightedMean(singleClassificationResults.map { it.phiOverPhiMax!! }, weights) else null
            )
        return values.toAggregatedResult(type, confusionMatrix)
    }

    private fun weightedMean(
        values: List<Double>,
        weights: List<Int>
    ): Double {
        var weightedSum = 0.0
        var sumOfWeights = 0.0
        for ((index, value) in values.withIndex()) {
            weightedSum += value * weights[index]
            sumOfWeights += weights[index]
        }
        return weightedSum / sumOfWeights
    }
}
