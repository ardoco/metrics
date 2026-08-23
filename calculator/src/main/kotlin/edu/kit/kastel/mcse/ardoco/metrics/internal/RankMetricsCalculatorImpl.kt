package edu.kit.kastel.mcse.ardoco.metrics.internal

import edu.kit.kastel.mcse.ardoco.metrics.RankMetricsCalculator
import edu.kit.kastel.mcse.ardoco.metrics.calculation.calculateAUC
import edu.kit.kastel.mcse.ardoco.metrics.calculation.calculateLAG
import edu.kit.kastel.mcse.ardoco.metrics.calculation.calculateMAP
import edu.kit.kastel.mcse.ardoco.metrics.result.AggregatedRankMetricsResult
import edu.kit.kastel.mcse.ardoco.metrics.result.AggregationType
import edu.kit.kastel.mcse.ardoco.metrics.result.SingleRankMetricsResult

internal class RankMetricsCalculatorImpl : RankMetricsCalculator {
    override fun calculateMetrics(
        rankedResults: List<List<String>>,
        groundTruth: Set<String>,
        relevanceBasedInput: RankMetricsCalculator.RelevanceBasedInput<Double>?
    ): SingleRankMetricsResult {
        require(rankedResults.isNotEmpty())
        require(rankedResults.all { it.size == rankedResults.first().size })
        val map = calculateMAP(rankedResults, groundTruth)
        val lag = calculateLAG(rankedResults, groundTruth)
        val auc = relevanceBasedInput?.let { calculateAUC(rankedResults, it.rankedRelevances, groundTruth, it.biggerIsMoreSimilar) }
        return SingleRankMetricsResult(map, lag, auc, groundTruth.size)
    }

    override fun calculateAverages(
        singleRankMetricsResults: List<SingleRankMetricsResult>,
        weights: List<Int>?
    ): List<AggregatedRankMetricsResult> {
        val macroAverage = calculateMacroAverage(singleRankMetricsResults)

        val weightsForAverage = weights ?: singleRankMetricsResults.map { it.groundTruthSize }
        val weightedAverage = calculateWeightedAverage(singleRankMetricsResults, weightsForAverage, AggregationType.WEIGHTED_AVERAGE)

        return listOf(macroAverage, weightedAverage)
    }

    private fun calculateMacroAverage(singleRankMetricsResults: List<SingleRankMetricsResult>): AggregatedRankMetricsResult {
        return calculateWeightedAverage(singleRankMetricsResults, singleRankMetricsResults.map { 1 }, AggregationType.MACRO_AVERAGE)
    }

    private fun calculateWeightedAverage(
        singleRankMetricsResults: List<SingleRankMetricsResult>,
        weights: List<Int>,
        type: AggregationType
    ): AggregatedRankMetricsResult {
        require(singleRankMetricsResults.isNotEmpty()) { "rankMetricsResults must not be empty" }

        var map = 0.0
        var lag = 0.0
        var auc = 0.0

        var sumOfWeights = 0.0

        for ((i, rankMetricsResult) in singleRankMetricsResults.withIndex()) {
            map += rankMetricsResult.map * weights[i]
            lag += rankMetricsResult.lag * weights[i]
            if (rankMetricsResult.auc != null) auc += rankMetricsResult.auc * weights[i]
            sumOfWeights += weights[i]
        }

        map /= sumOfWeights
        lag /= sumOfWeights

        return if (singleRankMetricsResults.all { it.auc == null }) {
            AggregatedRankMetricsResult(type, map, lag, null, singleRankMetricsResults, weights)
        } else {
            AggregatedRankMetricsResult(type, map, lag, auc, singleRankMetricsResults, weights)
        }
    }
}
