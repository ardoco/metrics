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
        val result = classificationMetricsCalculator.calculateMetrics(body.classification.toSet(), body.groundTruth.toSet(), body.confusionMatrixSum)
        return result
    }

    @Operation(summary = "Calculate classification metrics for multiple projects. Calculate the average and optionally a weighted average.")
    @PostMapping("/average")
    fun calculateMultipleClassificationMetrics(
        @RequestBody body: AverageClassificationMetricsRequest
    ): ClassificationAggregationResult<String> {
        val classificationMetricsCalculator = ClassificationMetricsCalculator.Instance

        val requests = body.classificationMetricsRequests
        val results =
            requests.map {
                classificationMetricsCalculator.calculateMetrics(it.classification.toSet(), it.groundTruth.toSet(), it.confusionMatrixSum)
            }

        return classificationMetricsCalculator.calculateAverages(results, body.weights)
    }

    data class AverageClassificationMetricsRequest(
        val classificationMetricsRequests: List<ClassificationMetricsRequest>,
        val weights: List<Int>? = null
    )

    data class ClassificationMetricsRequest(
        val classification: List<String>,
        val groundTruth: List<String>,
        val confusionMatrixSum: Int? = null
    )
}
