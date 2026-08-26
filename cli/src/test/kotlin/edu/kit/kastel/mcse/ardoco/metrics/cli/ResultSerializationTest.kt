package edu.kit.kastel.mcse.ardoco.metrics.cli

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.readValue
import edu.kit.kastel.mcse.ardoco.metrics.ClassificationMetricsCalculator
import edu.kit.kastel.mcse.ardoco.metrics.result.SingleClassificationResult
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.function.Executable

class ResultSerializationTest {
    private val calculator = ClassificationMetricsCalculator.Instance
    private val mapper = createObjectMapper()

    private fun result() = calculator.calculateMetrics(setOf("a", "b", "c"), setOf("a", "b", "d", "e"), 10, listOf(0.5, 2.0))

    @Test
    fun singleResultJsonShapeTest() {
        val tree = mapper.readTree(mapper.writeValueAsString(result()))
        assertAll(
            // The property has to be named fbetaScores: Jackson's legacy bean naming would mangle a getFScores() getter to "fscores" while
            // jackson-module-kotlin reports the constructor parameter as "fScores", and the mismatch would be silently dropped on read.
            Executable { assertTrue(tree.has("fbetaScores")) { "expected an fbetaScores property in $tree" } },
            Executable {
                assertEquals(
                    listOf("0.5", "1.0", "2.0"),
                    tree
                        .get("fbetaScores")
                        .fieldNames()
                        .asSequence()
                        .toList()
                )
            },
            Executable { assertEquals(0.5714285714285715, tree.get("fbetaScores").get("1.0").doubleValue(), 1e-12) },
            Executable { assertEquals(0.5714285714285715, tree.get("f1").doubleValue(), 1e-12) },
            Executable { assertTrue(tree.has("confusionMatrix")) { "expected a derived confusionMatrix property in $tree" } },
            Executable { assertEquals(2, tree.get("confusionMatrix").get("truePositives").intValue()) },
            Executable { assertEquals(1, tree.get("confusionMatrix").get("falsePositives").intValue()) },
            Executable { assertEquals(2, tree.get("confusionMatrix").get("falseNegatives").intValue()) },
            Executable { assertEquals(5, tree.get("confusionMatrix").get("trueNegatives").intValue()) },
            // total() is a function, so the confusion matrix serializes as exactly its four counts and stays readable by a strict mapper.
            Executable {
                assertEquals(
                    listOf("truePositives", "falsePositives", "falseNegatives", "trueNegatives"),
                    tree
                        .get("confusionMatrix")
                        .fieldNames()
                        .asSequence()
                        .toList()
                )
            }
        )
    }

    @Test
    fun singleResultRoundTripTest() {
        // Enabling FAIL_ON_UNKNOWN_PROPERTIES proves that the derived read-only properties are skipped rather than rejected.
        val strictMapper = createObjectMapper().rebuild().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build()
        val original = result()
        val roundTripped: SingleClassificationResult<String> = strictMapper.readValue(strictMapper.writeValueAsString(original))
        assertEquals(original, roundTripped)
    }

    @Test
    fun doubleKeyFidelityTest() {
        val original = calculator.calculateMetrics(setOf("a", "b"), setOf("a", "c"), 8, listOf(0.1, 0.5, 2.5))
        val roundTripped: SingleClassificationResult<String> = mapper.readValue(mapper.writeValueAsString(original))
        assertAll(
            Executable { assertEquals(setOf(0.1, 0.5, 1.0, 2.5), roundTripped.fbetaScores.keys) },
            Executable { assertEquals(original.fbetaScores.getValue(0.1), roundTripped.fbetaScores.getValue(0.1)) },
            Executable { assertEquals(original.fbetaScores.getValue(2.5), roundTripped.fbetaScores.getValue(2.5)) }
        )
    }

    @Test
    fun legacyResultWithoutFbetaScoresIsReadableTest() {
        // A result file written by 0.2.x: it has f1 but neither fbetaScores nor confusionMatrix.
        val legacy =
            """
            {
              "truePositives" : [ "a", "b" ],
              "falsePositives" : [ "c" ],
              "falseNegatives" : [ "d", "e" ],
              "trueNegatives" : 5,
              "precision" : 0.6666666666666666,
              "recall" : 0.5,
              "f1" : 0.5714285714285715,
              "accuracy" : 0.7,
              "specificity" : 0.8333333333333334,
              "phiCoefficient" : 0.35634832254989885,
              "phiCoefficientMax" : 0.8017837257372732,
              "phiOverPhiMax" : 0.4444444444444445
            }
            """.trimIndent()
        val parsed: SingleClassificationResult<String> = mapper.readValue(legacy)
        assertAll(
            Executable { assertEquals(mapOf(1.0 to 0.5714285714285715), parsed.fbetaScores) },
            Executable { assertEquals(0.5714285714285715, parsed.f1) },
            Executable { assertEquals(2, parsed.confusionMatrix.truePositives) },
            Executable { assertEquals(5, parsed.confusionMatrix.trueNegatives) },
            Executable { assertEquals(10, parsed.confusionMatrix.total()) },
            // Not-requested betas are still available because precision and recall are known.
            Executable { assertEquals(0.5263157894736842, parsed.fbeta(2.0), 1e-12) }
        )
    }

    @Test
    fun legacyResultWithExplicitNullFbetaScoresIsReadableTest() {
        val legacy =
            """
            {
              "truePositives" : [ "a" ],
              "falsePositives" : [ ],
              "falseNegatives" : [ ],
              "trueNegatives" : null,
              "precision" : 1.0,
              "recall" : 1.0,
              "f1" : 1.0,
              "fbetaScores" : null,
              "accuracy" : null,
              "specificity" : null,
              "phiCoefficient" : null,
              "phiCoefficientMax" : null,
              "phiOverPhiMax" : null
            }
            """.trimIndent()
        val parsed: SingleClassificationResult<String> = mapper.readValue(legacy)
        assertEquals(mapOf(1.0 to 1.0), parsed.fbetaScores)
    }

    @Test
    fun negativeTrueNegativesAreRejectedOnReadTest() {
        val invalid =
            """
            {
              "truePositives" : [ "a" ],
              "falsePositives" : [ ],
              "falseNegatives" : [ ],
              "trueNegatives" : -3,
              "precision" : 1.0,
              "recall" : 1.0,
              "f1" : 1.0,
              "accuracy" : null,
              "specificity" : null,
              "phiCoefficient" : null,
              "phiCoefficientMax" : null,
              "phiOverPhiMax" : null
            }
            """.trimIndent()
        val thrown = assertThrows<Exception> { mapper.readValue<SingleClassificationResult<String>>(invalid) }
        var cause: Throwable? = thrown
        while (cause != null && cause !is IllegalArgumentException) {
            cause = cause.cause
        }
        assertNotNull(cause) { "expected an IllegalArgumentException in the cause chain of $thrown" }
    }

    @Test
    fun aggregationJsonShapeTest() {
        val aggregation = calculator.calculateAverages(listOf(result(), result()), listOf(2, 3), listOf(2.0))
        val json = mapper.writeValueAsString(aggregation)
        val tree = mapper.readTree(json)
        assertAll(
            Executable { assertTrue(tree.isObject) { "the aggregation must serialize as an object, not an array" } },
            Executable {
                assertEquals(
                    listOf("singleResults", "weights", "macroAverage", "weightedAverage", "microAverage", "confusionMatrix", "betas"),
                    tree.fieldNames().asSequence().toList()
                )
            },
            Executable { assertEquals(listOf(2, 3), tree.get("weights").map { it.intValue() }) },
            Executable { assertEquals(listOf(1.0, 2.0), tree.get("betas").map { it.doubleValue() }) },
            Executable { assertEquals("MACRO_AVERAGE", tree.get("macroAverage").get("type").textValue()) },
            Executable { assertTrue(tree.get("macroAverage").has("f1")) { "each aggregation must expose f1" } },
            Executable { assertTrue(tree.get("macroAverage").has("fbetaScores")) { "each aggregation must expose fbetaScores" } },
            Executable {
                assertEquals(
                    4,
                    tree
                        .get("microAverage")
                        .get("confusionMatrix")
                        .get("truePositives")
                        .intValue()
                )
            },
            // The inputs are stored once for the whole aggregation instead of once per aggregation type.
            Executable { assertEquals(1, Regex("\"singleResults\"").findAll(json).count()) },
            // Functions must not leak into the JSON.
            Executable { assertTrue(!tree.has("asList")) { "asList() must not be serialized" } },
            Executable { assertTrue(!tree.has("truePositives")) { "the element unions must not be serialized" } }
        )
    }
}
