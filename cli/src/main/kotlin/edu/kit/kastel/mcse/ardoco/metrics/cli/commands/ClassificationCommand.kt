package edu.kit.kastel.mcse.ardoco.metrics.cli.commands

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.util.concurrent.Callable

@Command(name = "classification", description = ["Calculates classification metrics"], mixinStandardHelpOptions = true)
class ClassificationCommand : Callable<Int> {
    @Option(names = ["-c", "--classification"], description = ["The classification file"], required = true)
    lateinit var classificationFile: String

    @Option(names = ["-g", "--ground-truth"], description = ["The ground truth file"], required = true)
    lateinit var groundTruthFile: String

    @Option(names = ["--header"], description = ["Whether the files have a header"])
    var fileHeader: Boolean = false

    @Option(names = ["-s", "--sum"], description = ["The sum of the confusion matrix"])
    var confusionMatrixSum: Int = -1

    @Option(names = ["-o", "--output"], description = ["The output file"])
    var outputFile: String? = null

    override fun call(): Int {
        println("Calculating classification metrics")
        val classificationFileObj = java.io.File(classificationFile)
        val groundTruthFileObj = java.io.File(groundTruthFile)
        if (!classificationFileObj.exists() || !groundTruthFileObj.exists()) {
            println("Classification file or ground truth file does not exist")
            return 1
        }
        val classification =
            classificationFileObj
                .readLines()
                .filter { it.isNotBlank() }
                .drop(if (fileHeader) 1 else 0)
                .toSet()
        val groundTruth =
            groundTruthFileObj
                .readLines()
                .filter { it.isNotBlank() }
                .drop(if (fileHeader) 1 else 0)
                .toSet()
        val classificationMetrics = edu.kit.kastel.mcse.ardoco.metrics.ClassificationMetricsCalculator.Instance
        val result =
            classificationMetrics.calculateMetrics(
                classification,
                groundTruth,
                if (confusionMatrixSum < 0) null else confusionMatrixSum
            )
        result.prettyPrint()
        outputFile?.let {
            val outputFileObj = java.io.File(it)
            val oom = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).registerKotlinModule()
            oom.writeValue(outputFileObj, result)
        }
        return 0
    }
}
