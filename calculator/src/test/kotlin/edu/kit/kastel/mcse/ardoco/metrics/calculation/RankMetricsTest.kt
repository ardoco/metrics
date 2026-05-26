package edu.kit.kastel.mcse.ardoco.metrics.calculation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RankMetricsTest {
    @Test
    fun calculateAucShouldReturnOneForPerfectRanking() {
        val auc =
            calculateAUC(
                rankedResults = listOf(listOf("tp1", "tp2", "fp1", "fp2")),
                rankedRelevances = listOf(listOf(0.9, 0.8, 0.7, 0.6)),
                groundTruth = setOf("tp1", "tp2"),
                biggerIsMoreSimilar = true
            )

        assertEquals(1.0, auc, 1e-9)
    }

    @Test
    fun calculateAucShouldReturnZeroForWorstRanking() {
        val auc =
            calculateAUC(
                rankedResults = listOf(listOf("tp1", "tp2", "fp1", "fp2")),
                rankedRelevances = listOf(listOf(0.6, 0.7, 0.8, 0.9)),
                groundTruth = setOf("tp1", "tp2"),
                biggerIsMoreSimilar = true
            )

        assertEquals(0.0, auc, 1e-9)
    }

    @Test
    fun calculateAucShouldTreatTiedScoresAsHalfCorrect() {
        val auc =
            calculateAUC(
                rankedResults = listOf(listOf("tp", "fp")),
                rankedRelevances = listOf(listOf(0.5, 0.5)),
                groundTruth = setOf("tp"),
                biggerIsMoreSimilar = true
            )

        assertEquals(0.5, auc, 1e-9)
    }

    @Test
    fun calculateAucShouldSupportAscendingScores() {
        val auc =
            calculateAUC(
                rankedResults = listOf(listOf("tp1", "tp2", "fp1", "fp2")),
                rankedRelevances = listOf(listOf(0.1, 0.2, 0.3, 0.4)),
                groundTruth = setOf("tp1", "tp2"),
                biggerIsMoreSimilar = false
            )

        assertEquals(1.0, auc, 1e-9)
    }

    // Tests based on internet examples (Google Developers ML Crash Course and related references):
    // "If every positive example is ranked higher than every negative example, AUC = 1.0"
    @Test
    fun calculateAucShouldReturnOneForThreePositivesAboveThreeNegatives() {
        // Positives: 0.9, 0.8, 0.7 — Negatives: 0.3, 0.2, 0.1 (exact internet example)
        val auc =
            calculateAUC(
                rankedResults = listOf(listOf("tp1", "tp2", "tp3", "fp1", "fp2", "fp3")),
                rankedRelevances = listOf(listOf(0.9, 0.8, 0.7, 0.3, 0.2, 0.1)),
                groundTruth = setOf("tp1", "tp2", "tp3"),
                biggerIsMoreSimilar = true
            )

        assertEquals(1.0, auc, 1e-9)
    }

    // "If every positive example is ranked lower than every negative example, AUC = 0.0"
    @Test
    fun calculateAucShouldReturnZeroForThreePositivesBelowThreeNegatives() {
        // Positives: 0.1, 0.2, 0.3 — Negatives: 0.7, 0.8, 0.9 (exact internet example)
        val auc =
            calculateAUC(
                rankedResults = listOf(listOf("tp1", "tp2", "tp3", "fp1", "fp2", "fp3")),
                rankedRelevances = listOf(listOf(0.1, 0.2, 0.3, 0.7, 0.8, 0.9)),
                groundTruth = setOf("tp1", "tp2", "tp3"),
                biggerIsMoreSimilar = true
            )

        assertEquals(0.0, auc, 1e-9)
    }

    // "Half of the positive-negative pairs are correctly ranked, half are not" → AUC = 0.5
    // Non-tie partial example:
    // Scores: tp1=0.8, tp2=0.2, fp1=0.9, fp2=0.1
    // Pairs: (tp1=0.8 vs fp1=0.9) → fp wins (0), (tp1=0.8 vs fp2=0.1) → tp wins (1),
    //        (tp2=0.2 vs fp1=0.9) → fp wins (0), (tp2=0.2 vs fp2=0.1) → tp wins (1)
    // AUC = (0+1+0+1)/4 = 0.5
    @Test
    fun calculateAucShouldReturnHalfForMixedRankingWithHalfConcordantPairs() {
        val auc =
            calculateAUC(
                rankedResults = listOf(listOf("tp1", "tp2", "fp1", "fp2")),
                rankedRelevances = listOf(listOf(0.8, 0.2, 0.9, 0.1)),
                groundTruth = setOf("tp1", "tp2"),
                biggerIsMoreSimilar = true
            )

        assertEquals(0.5, auc, 1e-9)
    }
}
