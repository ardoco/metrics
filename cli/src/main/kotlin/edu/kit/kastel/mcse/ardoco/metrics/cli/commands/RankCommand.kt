package edu.kit.kastel.mcse.ardoco.metrics.cli.commands

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import edu.kit.kastel.mcse.ardoco.metrics.RankMetricsCalculator
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.util.concurrent.Callable

@Command(name = "rank", description = ["Calculates rank metrics"], mixinStandardHelpOptions = true)
class RankCommand : Callable<Int> {
    @Option(names = ["-r", "--ranked-list-directory"], description = ["The directory of the ranked list files"], required = true)
    lateinit var rankedListDirectory: String

    @Option(names = ["-g", "--ground-truth"], description = ["The ground truth file"], required = true)
    lateinit var groundTruthFile: String

    @Option(names = ["--header"], description = ["Whether the files have a header"])
    var fileHeader: Boolean = false

    @Option(names = ["--ranked-relevance-list-directory", "-rrl"], description = ["The directory of the ranked relevance list files"])
    var rankedRelevanceListDirectory: String? = null

    @Option(names = ["-b", "--bigger-is-more-similar"], description = ["Whether the relevance scores are more similar if bigger"])
    var biggerIsMoreSimilar: Boolean? = null

    @Option(names = ["-o", "--output"], description = ["The output file"])
    var outputFile: String? = null

    override fun call(): Int {
        println("Calculating rank metrics")
        val rankedListDirectoryFile = java.io.File(rankedListDirectory)
        val groundTruthFileObj = java.io.File(groundTruthFile)
        if (!rankedListDirectoryFile.exists() || !groundTruthFileObj.exists()) {
            println("The directory of the ranked list files or ground truth file does not exist")
            return 1
        }
        if (!rankedListDirectoryFile.isDirectory) {
            println("The provided path is not a directory")
            return 1
        }
        val rankedResults: List<List<String>> =
            rankedListDirectoryFile
                .listFiles()
                ?.filter { file ->
                    file.isFile
                }?.map { file -> file.readLines().filter { it.isNotBlank() }.drop(if (fileHeader) 1 else 0) } ?: emptyList()
        if (rankedResults.isEmpty()) {
            println("No classification results found")
            return 1
        }
        val groundTruth =
            groundTruthFileObj
                .readLines()
                .filter { it.isNotBlank() }
                .drop(if (fileHeader) 1 else 0)
                .toSet()
        var relevanceBasedInput: RankMetricsCalculator.RelevanceBasedInput<Double>? = null
        if (rankedRelevanceListDirectory != null) {
            val rankedRelevanceListDirectoryFile = java.io.File(rankedRelevanceListDirectory!!)
            if (!rankedRelevanceListDirectoryFile.exists() || !rankedRelevanceListDirectoryFile.isDirectory) {
                println("The directory of the ranked relevance list files does not exist or is not a directory")
                return 1
            }
            val rankedRelevances =
                rankedRelevanceListDirectoryFile
                    .listFiles()
                    ?.filter { file ->
                        file.isFile
                    }?.map { file ->
                        file
                            .readLines()
                            .filter { it.isNotBlank() }
                            .map { it.toDouble() }
                            .drop(if (fileHeader) 1 else 0)
                    } ?: emptyList()
            if (rankedRelevances.isEmpty()) {
                println("No relevance scores found")
                return 1
            }
            if (biggerIsMoreSimilar == null) {
                throw IllegalArgumentException("Both 'ranked-relevance-list-directory' and 'bigger-is-more-similar' must be specified together.")
            }
            relevanceBasedInput =
                RankMetricsCalculator.RelevanceBasedInput(
                    rankedRelevances,
                    { it },
                    biggerIsMoreSimilar!!
                )
        }
        val rankMetrics = RankMetricsCalculator.Instance
        val result =
            rankMetrics.calculateMetrics(
                rankedResults,
                groundTruth,
                relevanceBasedInput
            )
        result.prettyPrint()
        outputFile?.let {
            val outputFileObj = java.io.File(it)
            val oom =
                ObjectMapper()
                    .enable(
                        SerializationFeature.INDENT_OUTPUT
                    ).registerKotlinModule()
            oom.writeValue(outputFileObj, result)
        }
        return 0
    }
}
