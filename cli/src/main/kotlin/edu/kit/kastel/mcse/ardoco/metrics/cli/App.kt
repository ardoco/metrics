package edu.kit.kastel.mcse.ardoco.metrics.cli

import edu.kit.kastel.mcse.ardoco.metrics.cli.commands.AggregationClassificationCommand
import edu.kit.kastel.mcse.ardoco.metrics.cli.commands.ClassificationCommand
import picocli.CommandLine

fun main(args: Array<String>) {
    val rootCommand = RootCommand()
    CommandLine(rootCommand)
        .addSubcommand("classification", ClassificationCommand())
        .addSubcommand("aggCl", AggregationClassificationCommand())
        .execute(*args)
}

@CommandLine.Command(
    name = "ArDoCo Metrics",
    mixinStandardHelpOptions = true,
    description = ["CLI for ArDoCo Metrics"]
)
class RootCommand
