package edu.kit.kastel.mcse.ardoco.metrics.rest.controller

import edu.kit.kastel.mcse.ardoco.metrics.ClassificationMetricsCalculator
import edu.kit.kastel.mcse.ardoco.metrics.result.ClassificationAggregationResult
import edu.kit.kastel.mcse.ardoco.metrics.result.SingleClassificationResult
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/classification-metrics")
class ClassificationMetricsController {
    @Operation(summary = "Check if the service is running")
    @GetMapping
    fun running(): String = "ClassificationMetricsController is running"

    @Operation(summary = "Calculate classification metrics for one project")
    @PostMapping
    fun calculateClassificationMetrics(
        @RequestBody body: ClassificationMetricsRequest
    ): SingleClassificationResult<String> {
        val classificationMetricsCalculator = ClassificationMetricsCalculator.Instance
        return classificationMetricsCalculator.calculateMetrics(
            body.classification.toSet(),
            body.groundTruth.toSet(),
            body.confusionMatrixSum,
            body.betas ?: emptyList()
        )
    }

    @Operation(
        summary = "Calculate classification metrics for multiple projects. Calculate the macro, the weighted and the micro average.",
        description = "The betas of the F-beta scores apply to all projects and therefore have to be specified on the request level."
    )
    @PostMapping("/average")
    fun calculateMultipleClassificationMetrics(
        @RequestBody body: AverageClassificationMetricsRequest
    ): ClassificationAggregationResult<String> {
        val classificationMetricsCalculator = ClassificationMetricsCalculator.Instance

        val requests = body.classificationMetricsRequests
        require(requests.all { it.betas == null }) { "betas must be specified on the request level, not per classification metrics request" }

        val betas = body.betas ?: emptyList()
        val results =
            requests.map {
                classificationMetricsCalculator.calculateMetrics(it.classification.toSet(), it.groundTruth.toSet(), it.confusionMatrixSum, betas)
            }

        return classificationMetricsCalculator.calculateAverages(results, body.weights, body.betas)
    }

    data class AverageClassificationMetricsRequest(
        val classificationMetricsRequests: List<ClassificationMetricsRequest>,
        val weights: List<Int>? = null,
        val betas: List<Double>? = null
    )

    data class ClassificationMetricsRequest(
        val classification: List<String>,
        val groundTruth: List<String>,
        val confusionMatrixSum: Int? = null,
        val betas: List<Double>? = null
    )
}
