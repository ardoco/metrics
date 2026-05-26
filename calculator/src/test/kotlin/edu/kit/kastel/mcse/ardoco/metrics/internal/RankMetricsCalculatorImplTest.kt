package edu.kit.kastel.mcse.ardoco.metrics.internal

import edu.kit.kastel.mcse.ardoco.metrics.RankMetricsCalculator
import edu.kit.kastel.mcse.ardoco.metrics.result.AggregationType
import edu.kit.kastel.mcse.ardoco.metrics.result.SingleRankMetricsResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class RankMetricsCalculatorImplTest {
    @Test
    fun calculateAveragesShouldAverageAucWithWeights() {
        val results =
            listOf(
                SingleRankMetricsResult(map = 0.2, lag = 0.1, auc = 0.2, groundTruthSize = 1),
                SingleRankMetricsResult(map = 0.8, lag = 0.3, auc = 0.6, groundTruthSize = 3)
            )

        val weightedAverage =
            RankMetricsCalculator.Instance
                .calculateAverages(results, listOf(1, 3))
                .first { it.type == AggregationType.WEIGHTED_AVERAGE }

        assertNotNull(weightedAverage.auc)
        assertEquals(0.5, weightedAverage.auc!!, 1e-9)
    }
}
