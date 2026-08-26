package edu.kit.kastel.mcse.ardoco.metrics.cli

import com.fasterxml.jackson.module.kotlin.readValue
import edu.kit.kastel.mcse.ardoco.metrics.ClassificationMetricsCalculator
import edu.kit.kastel.mcse.ardoco.metrics.cli.commands.AggregationClassificationCommand
import edu.kit.kastel.mcse.ardoco.metrics.cli.commands.ClassificationCommand
import edu.kit.kastel.mcse.ardoco.metrics.result.SingleClassificationResult
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.junit.jupiter.api.io.TempDir
import picocli.CommandLine
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.writeText

class ClassificationCommandTest {
    private val calculator = ClassificationMetricsCalculator.Instance
    private val mapper = createObjectMapper()

    @Test
    fun registeredSubcommandsTest() {
        // Guards that the removed rank commands stay removed.
        assertEquals(setOf("classification", "aggCl"), createCommandLine().subcommands.keys)
    }

    @Test
    fun betaOptionParsingTest() {
        val commaSeparated = ClassificationCommand()
        CommandLine(commaSeparated).parseArgs("-c", "c.txt", "-g", "g.txt", "-b", "0.5,2.0")
        val repeated = ClassificationCommand()
        CommandLine(repeated).parseArgs("-c", "c.txt", "-g", "g.txt", "-b", "0.5", "-b", "2.0")
        val none = ClassificationCommand()
        CommandLine(none).parseArgs("-c", "c.txt", "-g", "g.txt")
        assertAll(
            Executable { assertEquals(listOf(0.5, 2.0), commaSeparated.betas) },
            Executable { assertEquals(listOf(0.5, 2.0), repeated.betas) },
            Executable { assertEquals(emptyList<Double>(), none.betas) }
        )
    }

    @Test
    fun classificationCommandTest(
        @TempDir dir: Path
    ) {
        val classification = dir.resolve("classification.txt").also { it.writeText("a\nb\nc\n") }
        val groundTruth = dir.resolve("groundTruth.txt").also { it.writeText("a\nb\nd\ne\n") }
        val output = dir.resolve("result.json")

        val exitCode =
            CommandLine(ClassificationCommand()).execute(
                "-c",
                classification.toString(),
                "-g",
                groundTruth.toString(),
                "-s",
                "10",
                "-b",
                "0.5,2.0",
                "-o",
                output.toString()
            )

        val written: SingleClassificationResult<String> = mapper.readValue(output.toFile())
        val expected = calculator.calculateMetrics(setOf("a", "b", "c"), setOf("a", "b", "d", "e"), 10, listOf(0.5, 2.0))
        assertAll(
            Executable { assertEquals(0, exitCode) },
            Executable { assertEquals(expected, written) },
            Executable { assertEquals(setOf(0.5, 1.0, 2.0), written.fbetaScores.keys) }
        )
    }

    @Test
    fun classificationCommandWithoutBetasAndSumTest(
        @TempDir dir: Path
    ) {
        val classification = dir.resolve("classification.txt").also { it.writeText("a\nb\nc\n") }
        val groundTruth = dir.resolve("groundTruth.txt").also { it.writeText("a\nb\nd\ne\n") }
        val output = dir.resolve("result.json")

        val exitCode =
            CommandLine(ClassificationCommand())
                .execute("-c", classification.toString(), "-g", groundTruth.toString(), "-o", output.toString())

        val written: SingleClassificationResult<String> = mapper.readValue(output.toFile())
        assertAll(
            Executable { assertEquals(0, exitCode) },
            Executable { assertEquals(setOf(1.0), written.fbetaScores.keys) },
            Executable { assertEquals(null, written.trueNegatives) },
            Executable { assertEquals(null, written.accuracy) }
        )
    }

    @Test
    fun classificationCommandWithHeaderTest(
        @TempDir dir: Path
    ) {
        val classification = dir.resolve("classification.txt").also { it.writeText("header\na\nb\nc\n") }
        val groundTruth = dir.resolve("groundTruth.txt").also { it.writeText("header\na\nb\nd\ne\n") }
        val output = dir.resolve("result.json")

        val exitCode =
            CommandLine(ClassificationCommand())
                .execute("-c", classification.toString(), "-g", groundTruth.toString(), "--header", "-o", output.toString())

        val written: SingleClassificationResult<String> = mapper.readValue(output.toFile())
        assertAll(
            Executable { assertEquals(0, exitCode) },
            Executable { assertEquals(setOf("a", "b"), written.truePositives) },
            Executable { assertEquals(setOf("c"), written.falsePositives) },
            Executable { assertEquals(setOf("d", "e"), written.falseNegatives) }
        )
    }

    @Test
    fun commandsWithoutOutputFileTest(
        @TempDir dir: Path
    ) {
        val classification = dir.resolve("classification.txt").also { it.writeText("a\nb\nc\n") }
        val groundTruth = dir.resolve("groundTruth.txt").also { it.writeText("a\nb\nd\ne\n") }
        val resultsDir = dir.resolve("results").also { it.createDirectory() }
        resultsDir.resolve("first.json").writeText(
            mapper.writeValueAsString(calculator.calculateMetrics(setOf("a", "b"), setOf("a", "c"), 8))
        )

        val classificationExit =
            CommandLine(ClassificationCommand()).execute("-c", classification.toString(), "-g", groundTruth.toString(), "-s", "10")
        val aggregationExit = CommandLine(AggregationClassificationCommand()).execute("-d", resultsDir.toString())

        assertAll(
            Executable { assertEquals(0, classificationExit) },
            Executable { assertEquals(0, aggregationExit) },
            // Nothing is written when -o is omitted.
            Executable { assertEquals(1, resultsDir.toFile().listFiles()!!.size) }
        )
    }

    @Test
    fun classificationCommandWithBlankLinesTest(
        @TempDir dir: Path
    ) {
        val classification = dir.resolve("classification.txt").also { it.writeText("a\n\n  \nb\n") }
        val groundTruth = dir.resolve("groundTruth.txt").also { it.writeText("\na\n\nc\n") }
        val output = dir.resolve("result.json")

        val exitCode =
            CommandLine(ClassificationCommand())
                .execute("-c", classification.toString(), "-g", groundTruth.toString(), "-o", output.toString())

        val written: SingleClassificationResult<String> = mapper.readValue(output.toFile())
        assertAll(
            Executable { assertEquals(0, exitCode) },
            Executable { assertEquals(setOf("a"), written.truePositives) },
            Executable { assertEquals(setOf("b"), written.falsePositives) },
            Executable { assertEquals(setOf("c"), written.falseNegatives) }
        )
    }

    @Test
    fun classificationCommandWithMissingFileTest(
        @TempDir dir: Path
    ) {
        val groundTruth = dir.resolve("groundTruth.txt").also { it.writeText("a\n") }
        val exitCode =
            CommandLine(ClassificationCommand())
                .execute("-c", dir.resolve("missing.txt").toString(), "-g", groundTruth.toString())
        assertEquals(1, exitCode)
    }

    @Test
    fun aggregationCommandTest(
        @TempDir dir: Path
    ) {
        val resultsDir = dir.resolve("results").also { it.createDirectory() }
        val first = calculator.calculateMetrics(setOf("a", "b", "c"), setOf("a", "b", "d", "e"), 10, listOf(2.0))
        val second = calculator.calculateMetrics(setOf("f", "g", "h", "i"), setOf("f"), 12, listOf(2.0))
        resultsDir.resolve("first.json").writeText(mapper.writeValueAsString(first))
        resultsDir.resolve("second.json").writeText(mapper.writeValueAsString(second))
        val output = dir.resolve("aggregation.json")

        val exitCode = CommandLine(AggregationClassificationCommand()).execute("-d", resultsDir.toString(), "-o", output.toString())

        val expected = calculator.calculateAverages(listOf(first, second))
        val tree = mapper.readTree(output.toFile())
        assertAll(
            Executable { assertEquals(0, exitCode) },
            Executable { assertEquals(expected.macroAverage.precision, tree.get("macroAverage").get("precision").doubleValue(), 1e-12) },
            Executable { assertEquals(expected.weightedAverage.f1, tree.get("weightedAverage").get("f1").doubleValue(), 1e-12) },
            Executable { assertEquals(expected.microAverage.recall, tree.get("microAverage").get("recall").doubleValue(), 1e-12) },
            Executable {
                assertEquals(
                    expected.confusionMatrix.truePositives,
                    tree.get("confusionMatrix").get("truePositives").intValue()
                )
            },
            Executable { assertEquals(listOf(1.0, 2.0), tree.get("betas").map { it.doubleValue() }) },
            Executable { assertEquals(expected.weights, tree.get("weights").map { it.intValue() }) },
            Executable { assertEquals(2, tree.get("singleResults").size()) }
        )
    }

    @Test
    fun aggregationCommandReadsLegacyResultFilesTest(
        @TempDir dir: Path
    ) {
        val resultsDir = dir.resolve("results").also { it.createDirectory() }
        val current = calculator.calculateMetrics(setOf("a", "b", "c"), setOf("a", "b", "d", "e"), 10, listOf(2.0))
        resultsDir.resolve("current.json").writeText(mapper.writeValueAsString(current))
        // A file written by 0.2.x: f1 but no fbetaScores and no confusionMatrix.
        resultsDir.resolve("legacy.json").writeText(
            """
            {
              "truePositives" : [ "f" ],
              "falsePositives" : [ "g", "h", "i" ],
              "falseNegatives" : [ ],
              "trueNegatives" : 8,
              "precision" : 0.25,
              "recall" : 1.0,
              "f1" : 0.4,
              "accuracy" : 0.75,
              "specificity" : 0.7272727272727273,
              "phiCoefficient" : 0.42640143271122083,
              "phiCoefficientMax" : 0.42640143271122083,
              "phiOverPhiMax" : 1.0
            }
            """.trimIndent()
        )
        val output = dir.resolve("aggregation.json")

        val exitCode = CommandLine(AggregationClassificationCommand()).execute("-d", resultsDir.toString(), "-o", output.toString())

        val tree = mapper.readTree(output.toFile())
        assertAll(
            Executable { assertEquals(0, exitCode) },
            // The beta missing from the legacy file is recalculated from its precision and recall.
            Executable { assertEquals(listOf(1.0, 2.0), tree.get("betas").map { it.doubleValue() }) },
            Executable { assertEquals(2, tree.get("singleResults").size()) },
            Executable { assertEquals(3, tree.get("confusionMatrix").get("truePositives").intValue()) },
            Executable { assertEquals(13, tree.get("confusionMatrix").get("trueNegatives").intValue()) }
        )
    }

    @Test
    fun aggregationCommandWithNonDirectoryTest(
        @TempDir dir: Path
    ) {
        val file = dir.resolve("not-a-directory.txt").also { it.writeText("a\n") }
        assertEquals(1, CommandLine(AggregationClassificationCommand()).execute("-d", file.toString()))
    }

    @Test
    fun aggregationCommandWithEmptyDirectoryTest(
        @TempDir dir: Path
    ) {
        val emptyDir = dir.resolve("empty").also { it.createDirectory() }
        assertEquals(1, CommandLine(AggregationClassificationCommand()).execute("-d", emptyDir.toString()))
    }
}
