package edu.kit.kastel.mcse.ardoco.metrics.cli

import edu.kit.kastel.mcse.ardoco.metrics.cli.commands.AggregationClassificationCommand
import edu.kit.kastel.mcse.ardoco.metrics.cli.commands.ClassificationCommand
import picocli.CommandLine
import kotlin.system.exitProcess

/**
 * Runs the command line and exits with the status the executed command returned, so that a failure is visible to the caller and not just printed.
 */
fun main(args: Array<String>) {
    exitProcess(createCommandLine().execute(*args))
}

/** Creates the command line with all supported subcommands. */
internal fun createCommandLine(): CommandLine =
    CommandLine(RootCommand())
        .addSubcommand("classification", ClassificationCommand())
        .addSubcommand("aggCl", AggregationClassificationCommand())

@CommandLine.Command(
    name = "ARDoCo Metrics",
    mixinStandardHelpOptions = true,
    description = ["CLI for ARDoCo Metrics"]
)
class RootCommand
