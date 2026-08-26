package edu.kit.kastel.mcse.ardoco.metrics.rest.controller

import edu.kit.kastel.mcse.ardoco.metrics.ClassificationMetricsCalculator
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.function.Executable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

@SpringBootTest
@AutoConfigureMockMvc
class ClassificationMetricsControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var handlerMapping: RequestMappingHandlerMapping

    private val calculator = ClassificationMetricsCalculator.Instance

    private fun postJson(
        path: String,
        body: String
    ) = mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))

    @Test
    fun runningTest() {
        mockMvc
            .perform(get("/classification-metrics"))
            .andExpect(status().isOk)
            .andExpect(content().string("ClassificationMetricsController is running"))
    }

    @Test
    fun singleMetricsTest() {
        val expected = calculator.calculateMetrics(setOf("a", "b", "c"), setOf("a", "b", "d", "e"), 10)
        postJson(
            "/classification-metrics",
            """{ "classification": ["a", "b", "c"], "groundTruth": ["a", "b", "d", "e"], "confusionMatrixSum": 10 }"""
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.precision").value(expected.precision))
            .andExpect(jsonPath("$.recall").value(expected.recall))
            .andExpect(jsonPath("$.f1").value(expected.f1))
            .andExpect(jsonPath("$.accuracy").value(expected.accuracy!!))
            .andExpect(jsonPath("$.specificity").value(expected.specificity!!))
            .andExpect(jsonPath("$.phiCoefficient").value(expected.phiCoefficient!!))
            .andExpect(jsonPath("$.fbetaScores['1.0']").value(expected.f1))
            .andExpect(jsonPath("$.confusionMatrix.truePositives").value(2))
            .andExpect(jsonPath("$.confusionMatrix.trueNegatives").value(5))
    }

    @Test
    fun singleMetricsWithoutConfusionMatrixSumTest() {
        postJson(
            "/classification-metrics",
            """{ "classification": ["a", "b", "c"], "groundTruth": ["a", "b", "d", "e"] }"""
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.trueNegatives").value(null as String?))
            .andExpect(jsonPath("$.accuracy").value(null as String?))
            .andExpect(jsonPath("$.confusionMatrix.trueNegatives").value(null as String?))
            .andExpect(jsonPath("$.fbetaScores['1.0']").exists())
    }

    @Test
    fun singleMetricsWithBetasTest() {
        val expected = calculator.calculateMetrics(setOf("a", "b", "c"), setOf("a", "b", "d", "e"), 10, listOf(0.5, 2.0))
        postJson(
            "/classification-metrics",
            """
            { "classification": ["a", "b", "c"], "groundTruth": ["a", "b", "d", "e"], "confusionMatrixSum": 10, "betas": [2.0, 0.5] }
            """.trimIndent()
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.fbetaScores['0.5']").value(expected.fbetaScores.getValue(0.5)))
            .andExpect(jsonPath("$.fbetaScores['1.0']").value(expected.fbetaScores.getValue(1.0)))
            .andExpect(jsonPath("$.fbetaScores['2.0']").value(expected.fbetaScores.getValue(2.0)))
    }

    @Test
    fun averageTest() {
        val first = calculator.calculateMetrics(setOf("a", "b"), setOf("a", "c"), 8)
        val second = calculator.calculateMetrics(setOf("d", "e"), setOf("d"), 8)
        val expected = calculator.calculateAverages(listOf(first, second))
        postJson(
            "/classification-metrics/average",
            """
            {
              "classificationMetricsRequests": [
                { "classification": ["a", "b"], "groundTruth": ["a", "c"], "confusionMatrixSum": 8 },
                { "classification": ["d", "e"], "groundTruth": ["d"], "confusionMatrixSum": 8 }
              ]
            }
            """.trimIndent()
        ).andExpect(status().isOk)
            // Named access instead of an array the caller has to filter.
            .andExpect(jsonPath("$.macroAverage.type").value("MACRO_AVERAGE"))
            .andExpect(jsonPath("$.weightedAverage.type").value("WEIGHTED_AVERAGE"))
            .andExpect(jsonPath("$.microAverage.type").value("MICRO_AVERAGE"))
            .andExpect(jsonPath("$.macroAverage.precision").value(expected.macroAverage.precision))
            .andExpect(jsonPath("$.microAverage.recall").value(expected.microAverage.recall))
            .andExpect(jsonPath("$.confusionMatrix.truePositives").value(expected.confusionMatrix.truePositives))
            .andExpect(jsonPath("$.weights").value(expected.weights))
            .andExpect(jsonPath("$.betas[0]").value(1.0))
            .andExpect(jsonPath("$.singleResults.length()").value(2))
    }

    @Test
    fun averageWithWeightsAndBetasTest() {
        val first = calculator.calculateMetrics(setOf("a", "b"), setOf("a", "c"), 8, listOf(2.0))
        val second = calculator.calculateMetrics(setOf("d", "e"), setOf("d"), 8, listOf(2.0))
        val expected = calculator.calculateAverages(listOf(first, second), listOf(3, 7), listOf(2.0))
        postJson(
            "/classification-metrics/average",
            """
            {
              "classificationMetricsRequests": [
                { "classification": ["a", "b"], "groundTruth": ["a", "c"], "confusionMatrixSum": 8 },
                { "classification": ["d", "e"], "groundTruth": ["d"], "confusionMatrixSum": 8 }
              ],
              "weights": [3, 7],
              "betas": [2.0]
            }
            """.trimIndent()
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.weights[0]").value(3))
            .andExpect(jsonPath("$.weights[1]").value(7))
            .andExpect(jsonPath("$.weightedAverage.precision").value(expected.weightedAverage.precision))
            .andExpect(jsonPath("$.weightedAverage.fbetaScores['2.0']").value(expected.weightedAverage.fbetaScores.getValue(2.0)))
            .andExpect(jsonPath("$.microAverage.fbetaScores['2.0']").value(expected.microAverage.fbetaScores.getValue(2.0)))
    }

    @Test
    fun invalidBetaIsRejectedTest() {
        postJson(
            "/classification-metrics",
            """{ "classification": ["a"], "groundTruth": ["a"], "betas": [0] }"""
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun mismatchedWeightsAreRejectedTest() {
        postJson(
            "/classification-metrics/average",
            """
            {
              "classificationMetricsRequests": [
                { "classification": ["a"], "groundTruth": ["a"] },
                { "classification": ["b"], "groundTruth": ["b"] }
              ],
              "weights": [1]
            }
            """.trimIndent()
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun perRequestBetasAreRejectedTest() {
        postJson(
            "/classification-metrics/average",
            """
            {
              "classificationMetricsRequests": [
                { "classification": ["a"], "groundTruth": ["a"], "betas": [2.0] }
              ]
            }
            """.trimIndent()
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun tooSmallConfusionMatrixSumIsRejectedTest() {
        postJson(
            "/classification-metrics",
            """{ "classification": ["a", "b"], "groundTruth": ["c", "d"], "confusionMatrixSum": 2 }"""
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun mixedTrueNegativesAreRejectedTest() {
        postJson(
            "/classification-metrics/average",
            """
            {
              "classificationMetricsRequests": [
                { "classification": ["a"], "groundTruth": ["a"], "confusionMatrixSum": 4 },
                { "classification": ["b"], "groundTruth": ["b"] }
              ]
            }
            """.trimIndent()
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun homeRedirectsToSwaggerTest() {
        mockMvc
            .perform(get("/"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/swagger-ui/index.html"))
    }

    @Test
    fun springStatusCodesArePreservedTest() {
        // Handler extends ResponseEntityExceptionHandler, so the exceptions Spring MVC raises itself keep their own status instead of being
        // swallowed by the catch-all and reported as 500.
        assertAll(
            // Unknown path.
            Executable { mockMvc.perform(get("/does-not-exist")).andExpect(status().isNotFound) },
            // Unsupported method on a known path.
            Executable { mockMvc.perform(delete("/classification-metrics")).andExpect(status().isMethodNotAllowed) },
            // Malformed request body.
            Executable {
                postJson("/classification-metrics", "{ not json }").andExpect(status().isBadRequest)
            },
            // A required property missing from the body.
            Executable {
                postJson("/classification-metrics", """{ "classification": ["a"] }""").andExpect(status().isBadRequest)
            }
        )
    }

    @Test
    fun rankMetricsAreGoneTest() {
        val mappings = handlerMapping.handlerMethods.keys.map { it.toString() }
        assertAll(
            Executable {
                assertTrue(mappings.none { it.contains("rank", ignoreCase = true) }) { "found a rank mapping in $mappings" }
            },
            Executable {
                assertTrue(mappings.any { it.contains("/classification-metrics") }) { "expected the classification mapping in $mappings" }
            },
            Executable {
                assertThrows<ClassNotFoundException> {
                    Class.forName("edu.kit.kastel.mcse.ardoco.metrics.rest.controller.RankMetricsController")
                }
            }
        )
    }
}
