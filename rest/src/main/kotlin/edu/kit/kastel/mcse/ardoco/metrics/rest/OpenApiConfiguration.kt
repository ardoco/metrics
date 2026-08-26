package edu.kit.kastel.mcse.ardoco.metrics.rest

import com.fasterxml.jackson.databind.ObjectMapper
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Documents the result schemas of the metrics library in the OpenAPI specification.
 *
 * The result types live in the calculator module, which deliberately has no dependency on Swagger or Jackson, so their properties are described from
 * here instead of being annotated. The descriptions are matched by property name and therefore apply to the single results, the aggregations and the
 * nested confusion matrices alike.
 */
@Configuration
class OpenApiConfiguration {
    @Bean
    fun resultSchemaDescriptions(): OpenApiCustomizer =
        OpenApiCustomizer { openApi ->
            val objectMapper = ObjectMapper()
            openApi.components?.schemas?.forEach { (schemaName, schema) ->
                SCHEMA_DESCRIPTIONS[schemaName]?.let { schema.description = it }
                // Attached here rather than to @ApiResponse, because a @Content override would replace the schema springdoc infers from the return
                // type and thereby drop the generic result schemas from the specification.
                SCHEMA_EXAMPLES[schemaName]?.let { schema.example = objectMapper.readTree(it) }
                schema.properties?.forEach { (propertyName, property) ->
                    if (property.description == null) {
                        PROPERTY_DESCRIPTIONS[propertyName]?.let { property.description = it }
                    }
                }
            }
        }

    private companion object {
        val SCHEMA_EXAMPLES =
            mapOf(
                "SingleClassificationResultString" to SINGLE_RESULT_EXAMPLE,
                "ClassificationAggregationResultString" to AGGREGATION_EXAMPLE
            )

        val SCHEMA_DESCRIPTIONS =
            mapOf(
                "SingleClassificationResultString" to "The metrics of one classification task, together with the classified elements.",
                "AggregatedClassificationResult" to
                    "One aggregation of multiple classification results: the aggregated metrics and the confusion matrix they are based on.",
                "ClassificationAggregationResultString" to
                    "The aggregation of multiple classification results. The macro, the weighted and the micro average are reachable by name; the " +
                    "aggregated results and the weights are held once for the whole aggregation.",
                "ConfusionMatrix" to "The four counts of a binary confusion matrix.",
                "ClassificationMetricsRequest" to "One classification task to calculate metrics for.",
                "AverageClassificationMetricsRequest" to "Multiple classification tasks to calculate metrics for and aggregate."
            )

        val PROPERTY_DESCRIPTIONS =
            mapOf(
                "type" to "The type of aggregation.",
                "truePositives" to "The elements that were classified and are in the ground truth, or their number in a confusion matrix.",
                "falsePositives" to "The elements that were classified but are not in the ground truth, or their number in a confusion matrix.",
                "falseNegatives" to "The elements that are in the ground truth but were not classified, or their number in a confusion matrix.",
                "trueNegatives" to "The number of correctly unclassified elements. Null if no confusion matrix sum was provided.",
                "confusionMatrix" to "The confusion matrix the metrics were derived from. For an aggregation it is pooled over all results.",
                "precision" to "TP / (TP + FP). 1.0 if nothing was classified, because then nothing was classified wrongly.",
                "recall" to "TP / (TP + FN). 1.0 if the ground truth is empty, because then nothing was missed.",
                "f1" to "The harmonic mean of precision and recall. Always equal to the entry of fbetaScores for beta 1.0.",
                "fbetaScores" to
                    "All calculated F-beta scores, keyed by beta. Always contains the key 1.0. For an aggregation, the macro and the weighted " +
                    "average are the (weighted) mean of the scores of the single results, while the micro average is recalculated from the pooled " +
                    "confusion matrix.",
                "accuracy" to "(TP + TN) / (TP + FP + FN + TN). Null if no confusion matrix sum was provided.",
                "specificity" to "TN / (TN + FP), the true negative rate. Null if no confusion matrix sum was provided.",
                "phiCoefficient" to
                    "The phi coefficient, also known as the Matthews correlation coefficient, between -1 and +1. Null if no confusion matrix sum " +
                    "was provided.",
                "phiCoefficientMax" to "The largest phi coefficient the marginals of this confusion matrix allow. Null if no sum was provided.",
                "phiOverPhiMax" to "The phi coefficient divided by its maximum possible value. Null if no confusion matrix sum was provided.",
                "singleResults" to "The results that were aggregated.",
                "weights" to "The weights used for the weighted average, in the order of the aggregated results.",
                "macroAverage" to "Every single result contributes with the same weight.",
                "weightedAverage" to "The average weighted according to the weights.",
                "microAverage" to "Calculated from the confusion matrix pooled over all single results.",
                "betas" to "The betas of the F-beta scores present in every aggregation. Always contains 1.0.",
                "classification" to "The classified elements.",
                "groundTruth" to "The expected elements.",
                "confusionMatrixSum" to
                    "The total number of elements that could have been classified. Optional; without it the metrics that need true negatives are " +
                    "null. Must be at least the number of classified and expected elements.",
                "classificationMetricsRequests" to "The classification tasks to calculate metrics for and aggregate."
            )

        const val SINGLE_RESULT_EXAMPLE = """{
          "truePositives": [
            "A",
            "B"
          ],
          "falsePositives": [
            "C",
            "D",
            "E"
          ],
          "falseNegatives": [],
          "trueNegatives": 15,
          "precision": 0.4,
          "recall": 1.0,
          "f1": 0.5714285714285715,
          "fbetaScores": {
            "0.5": 0.45454545454545453,
            "1.0": 0.5714285714285715,
            "2.0": 0.7692307692307692
          },
          "accuracy": 0.85,
          "specificity": 0.8333333333333334,
          "phiCoefficient": 0.5773502691896257,
          "phiCoefficientMax": 0.5773502691896257,
          "phiOverPhiMax": 1.0,
          "confusionMatrix": {
            "truePositives": 2,
            "falsePositives": 3,
            "falseNegatives": 0,
            "trueNegatives": 15
          }
        }"""

        const val AGGREGATION_EXAMPLE = """{
          "singleResults": [
            {
              "truePositives": [
                "A",
                "B"
              ],
              "falsePositives": [
                "C",
                "D",
                "E"
              ],
              "falseNegatives": [],
              "trueNegatives": 15,
              "precision": 0.4,
              "recall": 1.0,
              "f1": 0.5714285714285715,
              "fbetaScores": {
                "0.5": 0.45454545454545453,
                "1.0": 0.5714285714285715,
                "2.0": 0.7692307692307692
              },
              "accuracy": 0.85,
              "specificity": 0.8333333333333334,
              "phiCoefficient": 0.5773502691896257,
              "phiCoefficientMax": 0.5773502691896257,
              "phiOverPhiMax": 1.0,
              "confusionMatrix": {
                "truePositives": 2,
                "falsePositives": 3,
                "falseNegatives": 0,
                "trueNegatives": 15
              }
            },
            {
              "truePositives": [
                "F"
              ],
              "falsePositives": [],
              "falseNegatives": [
                "G",
                "H"
              ],
              "trueNegatives": 17,
              "precision": 1.0,
              "recall": 0.3333333333333333,
              "f1": 0.5,
              "fbetaScores": {
                "0.5": 0.7142857142857143,
                "1.0": 0.5,
                "2.0": 0.3846153846153846
              },
              "accuracy": 0.9,
              "specificity": 1.0,
              "phiCoefficient": 0.5461186812727502,
              "phiCoefficientMax": 0.5461186812727502,
              "phiOverPhiMax": 1.0,
              "confusionMatrix": {
                "truePositives": 1,
                "falsePositives": 0,
                "falseNegatives": 2,
                "trueNegatives": 17
              }
            }
          ],
          "weights": [
            2,
            3
          ],
          "macroAverage": {
            "type": "MACRO_AVERAGE",
            "confusionMatrix": {
              "truePositives": 3,
              "falsePositives": 3,
              "falseNegatives": 2,
              "trueNegatives": 32
            },
            "precision": 0.7,
            "recall": 0.6666666666666666,
            "f1": 0.5357142857142858,
            "fbetaScores": {
              "0.5": 0.5844155844155844,
              "1.0": 0.5357142857142858,
              "2.0": 0.5769230769230769
            },
            "accuracy": 0.875,
            "specificity": 0.9166666666666667,
            "phiCoefficient": 0.5617344752311879,
            "phiCoefficientMax": 0.5617344752311879,
            "phiOverPhiMax": 1.0
          },
          "weightedAverage": {
            "type": "WEIGHTED_AVERAGE",
            "confusionMatrix": {
              "truePositives": 3,
              "falsePositives": 3,
              "falseNegatives": 2,
              "trueNegatives": 32
            },
            "precision": 0.76,
            "recall": 0.6,
            "f1": 0.5285714285714287,
            "fbetaScores": {
              "0.5": 0.6103896103896104,
              "1.0": 0.5285714285714287,
              "2.0": 0.5384615384615384
            },
            "accuracy": 0.8800000000000001,
            "specificity": 0.9333333333333333,
            "phiCoefficient": 0.5586113164395005,
            "phiCoefficientMax": 0.5586113164395005,
            "phiOverPhiMax": 1.0
          },
          "microAverage": {
            "type": "MICRO_AVERAGE",
            "confusionMatrix": {
              "truePositives": 3,
              "falsePositives": 3,
              "falseNegatives": 2,
              "trueNegatives": 32
            },
            "precision": 0.5,
            "recall": 0.6,
            "f1": 0.5454545454545454,
            "fbetaScores": {
              "0.5": 0.5172413793103449,
              "1.0": 0.5454545454545454,
              "2.0": 0.5769230769230769
            },
            "accuracy": 0.875,
            "specificity": 0.9142857142857143,
            "phiCoefficient": 0.4763305116224668,
            "phiCoefficientMax": 0.8997354108424374,
            "phiOverPhiMax": 0.5294117647058824
          },
          "betas": [
            0.5,
            1.0,
            2.0
          ],
          "confusionMatrix": {
            "truePositives": 3,
            "falsePositives": 3,
            "falseNegatives": 2,
            "trueNegatives": 32
          }
        }"""
    }
}
