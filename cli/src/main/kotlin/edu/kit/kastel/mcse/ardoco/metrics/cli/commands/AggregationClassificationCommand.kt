package edu.kit.kastel.mcse.ardoco.metrics.cli.commands

import com.fasterxml.jackson.module.kotlin.readValue
import edu.kit.kastel.mcse.ardoco.metrics.ClassificationMetricsCalculator
import edu.kit.kastel.mcse.ardoco.metrics.cli.createObjectMapper
import edu.kit.kastel.mcse.ardoco.metrics.result.SingleClassificationResult
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.io.IOException
import java.util.concurrent.Callable

@Command(
    name = "aggCl",
    description = [
        "Aggregate results of multiple classifications. I.e., Macro Average + WeightedAverage + Micro Average. " +
            "The F-beta scores to aggregate are taken from the result files."
    ],
    mixinStandardHelpOptions = true
)
class AggregationClassificationCommand : Callable<Int> {
    @Option(names = ["-d", "--directory"], description = ["The directory with the classification results"], required = true)
    lateinit var directoryWithResults: String

    @Option(names = ["-o", "--output"], description = ["The output file"])
    var outputFile: String? = null

    override fun call(): Int {
        val directory = java.io.File(directoryWithResults)
        if (!directory.isDirectory) {
            println("The provided path is not a directory")
            return 1
        }
        val oom = createObjectMapper()
        // File.listFiles() returns entries in filesystem order, which differs between platforms and runs. Sorting by name keeps the aggregated
        // results, and therefore the weights derived from them, in a reproducible order. Hidden files are skipped so that bookkeeping entries such as
        // .DS_Store do not have to be cleaned up first; anything else has to parse, because silently skipping a file would aggregate over fewer
        // results than the caller asked for.
        val files =
            directory
                .listFiles()
                ?.filter { it.isFile && !it.isHidden && !it.name.startsWith(".") }
                ?.sortedBy { it.name }
                ?: emptyList()
        val results = mutableListOf<SingleClassificationResult<String>>()
        for (file in files) {
            try {
                results += oom.readValue<SingleClassificationResult<String>>(file)
            } catch (notAResult: IOException) {
                println("Could not read '${file.name}' as a classification result: ${notAResult.message}")
                println("Every non-hidden file in the directory has to be a classification result. Move the other files elsewhere and try again.")
                return 1
            }
        }
        if (results.isEmpty()) {
            println("No classification results found")
            return 1
        }
        val classificationMetrics = ClassificationMetricsCalculator.Instance
        val aggregation =
            try {
                classificationMetrics.calculateAverages(results)
            } catch (invalidInput: IllegalArgumentException) {
                println(invalidInput.message)
                return 1
            }
        println("Aggregated F-beta scores for betas: ${aggregation.betas.joinToString(", ")}")
        aggregation.prettyPrint()
        outputFile?.let {
            val outputFileObj = java.io.File(it)
            oom.writeValue(outputFileObj, aggregation)
        }
        return 0
    }
}
