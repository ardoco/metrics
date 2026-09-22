package edu.kit.kastel.mcse.ardoco.metrics.cli

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule

/**
 * Creates the object mapper used to read and write result files.
 *
 * `NullIsSameAsDefault` lets an explicit `null` fall back to the Kotlin default value, so a hand-edited file with `"fbetaScores": null` still loads.
 * Unknown properties are ignored so that files written by another version of this tool remain readable.
 */
internal fun createObjectMapper(): JsonMapper =
    jsonMapper {
        addModule(kotlinModule { enable(KotlinFeature.NullIsSameAsDefault) })
        enable(SerializationFeature.INDENT_OUTPUT)
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }
