package edu.kit.kastel.mcse.ardoco.metrics.result

import kotlin.math.sqrt

/**
 * Describes how one metric is distributed across the single results of an aggregation.
 *
 * @param min the smallest value
 * @param max the largest value
 * @param mean the arithmetic (unweighted) mean of the values
 * @param standardDeviation the population standard deviation of the values, i.e. divided by the number of values
 */
data class MetricSpread(
    /** The smallest value. */
    val min: Double,
    /** The largest value. */
    val max: Double,
    /** The arithmetic (unweighted) mean of the values. */
    val mean: Double,
    /** The population standard deviation of the values. */
    val standardDeviation: Double
) {
    internal companion object {
        /** Calculates the spread of the given non-empty list of values. */
        internal fun of(values: List<Double>): MetricSpread {
            require(values.isNotEmpty()) { "values must not be empty" }
            val mean = values.average()
            val variance = values.sumOf { (it - mean) * (it - mean) } / values.size
            return MetricSpread(values.min(), values.max(), mean, sqrt(variance))
        }
    }
}
