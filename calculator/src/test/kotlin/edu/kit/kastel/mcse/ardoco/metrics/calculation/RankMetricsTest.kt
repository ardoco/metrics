package edu.kit.kastel.mcse.ardoco.metrics.calculation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RankMetricsTest {
    @Test
    fun calculateAucShouldReturnOneForPerfectRanking() {
        val auc = calculateAUC(
            rankedResults = listOf(listOf("tp1", "tp2", "fp1", "fp2")),
            rankedRelevances = listOf(listOf(0.9, 0.8, 0.7, 0.6)),
            groundTruth = setOf("tp1", "tp2"),
            biggerIsMoreSimilar = true
        )

        assertEquals(1.0, auc, 1e-9)
    }

    @Test
    fun calculateAucShouldReturnZeroForWorstRanking() {
        val auc = calculateAUC(
            rankedResults = listOf(listOf("tp1", "tp2", "fp1", "fp2")),
            rankedRelevances = listOf(listOf(0.6, 0.7, 0.8, 0.9)),
            groundTruth = setOf("tp1", "tp2"),
            biggerIsMoreSimilar = true
        )

        assertEquals(0.0, auc, 1e-9)
    }

    @Test
    fun calculateAucShouldTreatTiedScoresAsHalfCorrect() {
        val auc = calculateAUC(
            rankedResults = listOf(listOf("tp", "fp")),
            rankedRelevances = listOf(listOf(0.5, 0.5)),
            groundTruth = setOf("tp"),
            biggerIsMoreSimilar = true
        )

        assertEquals(0.5, auc, 1e-9)
    }

    @Test
    fun calculateAucShouldSupportAscendingScores() {
        val auc = calculateAUC(
            rankedResults = listOf(listOf("tp1", "tp2", "fp1", "fp2")),
            rankedRelevances = listOf(listOf(0.1, 0.2, 0.3, 0.4)),
            groundTruth = setOf("tp1", "tp2"),
            biggerIsMoreSimilar = false
        )

        assertEquals(1.0, auc, 1e-9)
    }
}
