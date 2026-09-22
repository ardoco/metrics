package edu.kit.kastel.mcse.ardoco.metrics.rest.controller

import edu.kit.kastel.mcse.ardoco.metrics.ClassificationMetricsCalculator
import edu.kit.kastel.mcse.ardoco.metrics.result.ClassificationAggregationResult
import edu.kit.kastel.mcse.ardoco.metrics.result.SingleClassificationResult
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import io.swagger.v3.oas.annotations.parameters.RequestBody as ApiRequestBody

@RestController
@RequestMapping("/classification-metrics")
@Tag(name = "Classification Metrics", description = "Calculate classification metrics for one project and aggregate them across projects.")
class ClassificationMetricsController {
    @Operation(summary = "Check if the service is running")
    @ApiResponse(responseCode = "200", description = "The service is running.")
    @GetMapping
    fun running(): String = "ClassificationMetricsController is running"

    @Operation(
        summary = "Calculate classification metrics for one project",
        description =
            "Compares a classification against a ground truth. Without a confusion matrix sum the metrics that need true negatives are null. " +
                "Without betas only the F1-score is calculated; beta 1.0 is always included."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "The metrics of the classification."),
        ApiResponse(
            responseCode = "400",
            description = "A beta is not a finite number greater than 0, or the confusion matrix sum is smaller than the number of elements.",
            content = [
                Content(
                    mediaType = "text/plain",
                    examples = [ExampleObject(value = "Beta must be a finite number greater than 0 but was 0.0")]
                )
            ]
        )
    )
    @PostMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun calculateClassificationMetrics(
        @ApiRequestBody(
            required = true,
            content = [
                Content(
                    mediaType = "application/json",
                    examples = [
                        ExampleObject(name = "F1 only", value = MINIMAL_REQUEST_EXAMPLE),
                        ExampleObject(name = "With confusion matrix sum and betas", value = SINGLE_REQUEST_EXAMPLE)
                    ]
                )
            ]
        )
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
        description =
            "The response holds the three aggregations by name, the aggregated results and the weights once, and the pooled confusion matrix. " +
                "The betas of the F-beta scores apply to all projects and therefore have to be specified on the request level. Without weights the " +
                "size of the ground truth of each request is used; there has to be exactly one weight per request."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "The macro, weighted and micro average of the given projects."),
        ApiResponse(
            responseCode = "400",
            description =
                "A beta is invalid, the number of weights does not match the number of requests, betas were given inside a single request, a " +
                    "confusion matrix sum is too small, or only some requests have a confusion matrix sum.",
            content = [
                Content(
                    mediaType = "text/plain",
                    examples = [ExampleObject(value = "There must be exactly one weight per result but there were 1 weights for 2 results")]
                )
            ]
        )
    )
    @PostMapping("/average", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun calculateMultipleClassificationMetrics(
        @ApiRequestBody(
            required = true,
            content = [
                Content(
                    mediaType = "application/json",
                    examples = [
                        ExampleObject(name = "Default weights, F1 only", value = MINIMAL_AVERAGE_REQUEST_EXAMPLE),
                        ExampleObject(name = "With weights and betas", value = AVERAGE_REQUEST_EXAMPLE)
                    ]
                )
            ]
        )
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
        @field:Schema(description = "The classification tasks to calculate metrics for and aggregate.")
        val classificationMetricsRequests: List<ClassificationMetricsRequest>,
        @field:Schema(
            description = "One weight per request for the weighted average. Defaults to the size of the ground truth of each request.",
            example = "[2, 3]",
            nullable = true
        )
        val weights: List<Int>? = null,
        @field:Schema(
            description = "The betas of the F-beta scores to calculate for all requests. Beta 1.0 is always included.",
            example = "[0.5, 2.0]",
            nullable = true
        )
        val betas: List<Double>? = null
    )

    data class ClassificationMetricsRequest(
        @field:Schema(description = "The classified elements.", example = "[\"A\", \"B\", \"C\", \"D\", \"E\"]")
        val classification: List<String>,
        @field:Schema(description = "The expected elements.", example = "[\"A\", \"B\"]")
        val groundTruth: List<String>,
        @field:Schema(
            description =
                "The total number of elements that could have been classified. Without it the metrics that need true negatives are null. " +
                    "Must be at least the number of classified and expected elements.",
            example = "20",
            nullable = true
        )
        val confusionMatrixSum: Int? = null,
        @field:Schema(
            description = "The betas of the F-beta scores to calculate. Beta 1.0 is always included. Not allowed on the /average endpoint.",
            example = "[0.5, 2.0]",
            nullable = true
        )
        val betas: List<Double>? = null
    )

    private companion object {
        const val MINIMAL_REQUEST_EXAMPLE = """{
  "classification" : [ "A", "B", "C", "D", "E" ],
  "groundTruth" : [ "A", "B" ]
}"""

        const val SINGLE_REQUEST_EXAMPLE = """{
  "classification" : [ "A", "B", "C", "D", "E" ],
  "groundTruth" : [ "A", "B" ],
  "confusionMatrixSum" : 20,
  "betas" : [ 0.5, 2.0 ]
}"""

        const val MINIMAL_AVERAGE_REQUEST_EXAMPLE = """{
  "classificationMetricsRequests" : [ {
    "classification" : [ "A", "B", "C", "D", "E" ],
    "groundTruth" : [ "A", "B" ],
    "confusionMatrixSum" : 20
  }, {
    "classification" : [ "F" ],
    "groundTruth" : [ "F", "G", "H" ],
    "confusionMatrixSum" : 20
  } ]
}"""

        const val AVERAGE_REQUEST_EXAMPLE = """{
  "classificationMetricsRequests" : [ {
    "classification" : [ "A", "B", "C", "D", "E" ],
    "groundTruth" : [ "A", "B" ],
    "confusionMatrixSum" : 20
  }, {
    "classification" : [ "F" ],
    "groundTruth" : [ "F", "G", "H" ],
    "confusionMatrixSum" : 20
  } ],
  "weights" : [ 2, 3 ],
  "betas" : [ 0.5, 2.0 ]
}"""
    }
}
