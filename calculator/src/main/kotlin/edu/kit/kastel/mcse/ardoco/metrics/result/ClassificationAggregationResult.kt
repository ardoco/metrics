package edu.kit.kastel.mcse.ardoco.metrics.result

/**
 * The aggregation of multiple classification results: the macro, weighted and micro average, together with the single results they were calculated
 * from.
 *
 * The single results and the weights are stored once for the whole aggregation; everything that can be derived from them &ndash; the pooled confusion
 * matrix, the unions of classified elements and the spread of a metric &ndash; is provided on demand.
 *
 * @param T the type of classified elements
 */
data class ClassificationAggregationResult<T>(
    /** The single results that were aggregated. */
    val singleResults: List<SingleClassificationResult<out T>>,
    /** The weights used for [weightedAverage], in the order of [singleResults]. */
    val weights: List<Int>,
    /** The macro average, i.e. every single result contributes with the same weight. */
    val macroAverage: AggregatedClassificationResult,
    /** The weighted average according to [weights]. */
    val weightedAverage: AggregatedClassificationResult,
    /** The micro average, i.e. calculated from the confusion matrix pooled over all single results. */
    val microAverage: AggregatedClassificationResult
) {
    init {
        require(singleResults.isNotEmpty()) { "singleResults must not be empty" }
        require(weights.size == singleResults.size) {
            "There must be exactly one weight per single result but there were ${weights.size} weights for ${singleResults.size} results"
        }
        require(macroAverage.type == AggregationType.MACRO_AVERAGE) { "macroAverage must be of type ${AggregationType.MACRO_AVERAGE}" }
        require(weightedAverage.type == AggregationType.WEIGHTED_AVERAGE) { "weightedAverage must be of type ${AggregationType.WEIGHTED_AVERAGE}" }
        require(microAverage.type == AggregationType.MICRO_AVERAGE) { "microAverage must be of type ${AggregationType.MICRO_AVERAGE}" }
    }

    /** The confusion matrix pooled over all [singleResults]. */
    val confusionMatrix: ConfusionMatrix
        get() = microAverage.confusionMatrix

    /** The betas of the F-beta scores that were calculated for every aggregation. Always contains 1.0. */
    val betas: Set<Double>
        get() = macroAverage.fbetaScores.keys

    /**
     * Returns the aggregation of the given [type].
     *
     * @param type the type of aggregation
     * @return the aggregation of that type
     */
    operator fun get(type: AggregationType): AggregatedClassificationResult =
        when (type) {
            AggregationType.MACRO_AVERAGE -> macroAverage
            AggregationType.WEIGHTED_AVERAGE -> weightedAverage
            AggregationType.MICRO_AVERAGE -> microAverage
        }

    /**
     * Returns all aggregations, ordered macro, weighted, micro.
     *
     * This is intentionally a function and not a property: as a property it would be serialized in addition to the three individual aggregations.
     *
     * @return all aggregations
     */
    fun asList(): List<AggregatedClassificationResult> = listOf(macroAverage, weightedAverage, microAverage)

    /**
     * Returns the union of the true positives of all [singleResults].
     *
     * @return the union of all true positives
     */
    fun truePositives(): Set<T> = singleResults.flatMapTo(mutableSetOf()) { it.truePositives }

    /**
     * Returns the union of the false positives of all [singleResults].
     *
     * @return the union of all false positives
     */
    fun falsePositives(): Set<T> = singleResults.flatMapTo(mutableSetOf()) { it.falsePositives }

    /**
     * Returns the union of the false negatives of all [singleResults].
     *
     * @return the union of all false negatives
     */
    fun falseNegatives(): Set<T> = singleResults.flatMapTo(mutableSetOf()) { it.falseNegatives }

    /**
     * Returns how the given metric is distributed across the [singleResults].
     *
     * @param metric the metric to inspect
     * @return the spread of the metric, or null if the metric is not available for the single results
     */
    fun spread(metric: ClassificationMetric): MetricSpread? {
        val values = singleResults.map { metric.valueIn(it) }
        if (values.any { it == null }) return null
        return MetricSpread.of(values.map { it as Double })
    }

    /**
     * Returns how the F-beta score for [beta] is distributed across the [singleResults].
     *
     * @param beta the weight of the recall relative to the precision; must be finite and greater than 0
     * @return the spread of the F-beta score
     * @throws IllegalArgumentException if [beta] is not a finite number greater than 0
     */
    fun fbetaSpread(beta: Double): MetricSpread = MetricSpread.of(singleResults.map { it.fbeta(beta) })

    /** Prints all aggregations in a human-readable format to the logger. */
    fun prettyPrint() {
        asList().forEach { it.prettyPrint() }
    }
}
