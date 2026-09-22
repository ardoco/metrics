package edu.kit.kastel.mcse.ardoco.metrics.cli.commands

import edu.kit.kastel.mcse.ardoco.metrics.ClassificationMetricsCalculator
import edu.kit.kastel.mcse.ardoco.metrics.cli.createObjectMapper
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
    var confusionMatrixSum: Int? = null

    @Option(
        names = ["-b", "--beta"],
        description = ["Betas of additional F-beta scores; repeatable or comma-separated. The F1-score is always calculated."],
        split = ",",
        paramLabel = "<beta>"
    )
    var betas: MutableList<Double> = mutableListOf()

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
        val classificationMetrics = ClassificationMetricsCalculator.Instance
        val result =
            try {
                classificationMetrics.calculateMetrics(classification, groundTruth, confusionMatrixSum, betas)
            } catch (invalidInput: IllegalArgumentException) {
                println(invalidInput.message)
                return 1
            }
        result.prettyPrint()
        outputFile?.let {
            val outputFileObj = java.io.File(it)
            createObjectMapper().writeValue(outputFileObj, result)
        }
        return 0
    }
}
