package edu.kit.kastel.mcse.ardoco.metrics.cli.commands

import com.fasterxml.jackson.module.kotlin.readValue
import edu.kit.kastel.mcse.ardoco.metrics.ClassificationMetricsCalculator
import edu.kit.kastel.mcse.ardoco.metrics.cli.createObjectMapper
import edu.kit.kastel.mcse.ardoco.metrics.result.SingleClassificationResult
import picocli.CommandLine.Command
import picocli.CommandLine.Option
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
        // results, and therefore the weights derived from them, in a reproducible order.
        val results: List<SingleClassificationResult<String>> =
            directory
                .listFiles()
                ?.filter { it.isFile }
                ?.sortedBy { it.name }
                ?.map {
                    oom.readValue(
                        it.inputStream()
                    )
                } ?: emptyList()
        if (results.isEmpty()) {
            println("No classification results found")
            return 1
        }
        val classificationMetrics = ClassificationMetricsCalculator.Instance
        val aggregation = classificationMetrics.calculateAverages(results)
        println("Aggregated F-beta scores for betas: ${aggregation.betas.joinToString(", ")}")
        aggregation.prettyPrint()
        outputFile?.let {
            val outputFileObj = java.io.File(it)
            oom.writeValue(outputFileObj, aggregation)
        }
        return 0
    }
}
