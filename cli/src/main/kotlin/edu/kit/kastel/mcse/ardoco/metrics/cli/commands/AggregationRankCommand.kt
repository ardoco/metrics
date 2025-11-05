package edu.kit.kastel.mcse.ardoco.metrics.cli.commands

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import edu.kit.kastel.mcse.ardoco.metrics.RankMetricsCalculator
import edu.kit.kastel.mcse.ardoco.metrics.result.SingleRankMetricsResult
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.util.concurrent.Callable

@Command(
    name = "aggRnk",
    description = ["Aggregate results of multiple rank metrics runs. I.e., Macro Average + WeightedAverage"],
    mixinStandardHelpOptions = true
)
class AggregationRankCommand : Callable<Int> {
    @Option(names = ["-d", "--directory"], description = ["The directory with the rank results"], required = true)
    lateinit var directoryWithResults: String

    @Option(names = ["-o", "--output"], description = ["The output file"])
    var outputFile: String? = null

    override fun call(): Int {
        val directory = java.io.File(directoryWithResults)
        if (!directory.isDirectory) {
            println("The provided path is not a directory")
            return 1
        }
        val oom = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).registerKotlinModule()
        val results: List<SingleRankMetricsResult> =
            directory.listFiles()?.filter { it.isFile }?.map {
                oom.readValue(
                    it.inputStream()
                )
            } ?: emptyList()
        if (results.isEmpty()) {
            println("No classification results found")
            return 1
        }
        val rankMetrics = RankMetricsCalculator.Instance
        val average = rankMetrics.calculateAverages(results)
        average.forEach { it.prettyPrint() }
        outputFile?.let {
            val outputFileObj = java.io.File(it)
            oom.writeValue(outputFileObj, average)
        }
        return 0
    }
}
