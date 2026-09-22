package edu.kit.kastel.mcse.ardoco.metrics.rest

import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.info.License
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@OpenAPIDefinition(
    info =
        Info(
            title = "ARDoCo: Metrics",
            description = "This tool provides functionality to calculate and aggregate classification metrics for various tasks.",
            license = License(name = "MIT License", url = "https://github.com/ardoco/metrics/blob/main/LICENSE.md")
        ),
    externalDocs = ExternalDocumentation(description = "ARDoCo: Metrics - Wiki", url = "https://github.com/ardoco/metrics/wiki")
)
@SpringBootApplication
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
