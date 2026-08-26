package edu.kit.kastel.mcse.ardoco.metrics.rest

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Guards that the Swagger UI stays complete: every schema property documented, examples on the request bodies and the result schemas, and a
 * documented error response on both operations.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocumentationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    private lateinit var apiDocs: JsonNode

    @BeforeEach
    fun fetchApiDocs() {
        val body =
            mockMvc
                .perform(get("/v3/api-docs"))
                .andExpect(status().isOk)
                .andReturn()
                .response.contentAsString
        apiDocs = ObjectMapper().readTree(body)
    }

    @Test
    fun swaggerUiIsServedTest() {
        assertAll(
            Executable { mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk) },
            Executable { mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk) }
        )
    }

    @Test
    fun apiInfoIsPresentTest() {
        val info = apiDocs.get("info")
        assertAll(
            Executable { assertEquals("ARDoCo: Metrics", info.get("title").textValue()) },
            Executable { assertTrue(info.get("description").textValue().isNotBlank()) },
            Executable {
                assertTrue(
                    info
                        .get("license")
                        .get("name")
                        .textValue()
                        .isNotBlank()
                )
            },
            Executable {
                assertTrue(
                    apiDocs
                        .get("externalDocs")
                        .get("url")
                        .textValue()
                        .isNotBlank()
                )
            }
        )
    }

    @Test
    fun everySchemaPropertyIsDescribedTest() {
        val schemas = apiDocs.get("components").get("schemas")
        val undocumented =
            schemas
                .fieldNames()
                .asSequence()
                .flatMap { schemaName ->
                    val properties = schemas.get(schemaName).get("properties") ?: return@flatMap emptySequence<String>()
                    properties
                        .fieldNames()
                        .asSequence()
                        .filter {
                            properties
                                .get(it)
                                .get("description")
                                ?.textValue()
                                .isNullOrBlank()
                        }.map { "$schemaName.$it" }
                }.toList()
        assertTrue(undocumented.isEmpty()) { "Schema properties without a description: $undocumented" }
    }

    @Test
    fun everySchemaHasADescriptionTest() {
        val schemas = apiDocs.get("components").get("schemas")
        val undocumented =
            schemas
                .fieldNames()
                .asSequence()
                .filter {
                    schemas
                        .get(it)
                        .get("description")
                        ?.textValue()
                        .isNullOrBlank()
                }.toList()
        assertTrue(undocumented.isEmpty()) { "Schemas without a description: $undocumented" }
    }

    @Test
    fun resultSchemasCarryAnExampleTest() {
        val schemas = apiDocs.get("components").get("schemas")
        assertAll(
            Executable {
                val example = schemas.get("SingleClassificationResultString").get("example")
                assertAll(
                    Executable { assertTrue(example != null) { "the single result schema needs an example" } },
                    Executable { assertTrue(example.has("fbetaScores")) { "the example must show the F-beta scores" } },
                    Executable { assertTrue(example.has("confusionMatrix")) { "the example must show the confusion matrix" } }
                )
            },
            Executable {
                val example = schemas.get("ClassificationAggregationResultString").get("example")
                assertAll(
                    Executable { assertTrue(example != null) { "the aggregation schema needs an example" } },
                    Executable { assertTrue(example.has("macroAverage")) { "the example must show the macro average" } },
                    Executable { assertTrue(example.has("weightedAverage")) { "the example must show the weighted average" } },
                    Executable { assertTrue(example.has("microAverage")) { "the example must show the micro average" } }
                )
            }
        )
    }

    @Test
    fun operationsAreDocumentedTest() {
        val operations =
            listOf(
                "/classification-metrics" to "post",
                "/classification-metrics/average" to "post"
            )
        assertAll(
            operations.flatMap { (path, method) ->
                val operation = apiDocs.get("paths").get(path).get(method)
                listOf(
                    Executable { assertTrue(operation.get("summary").textValue().isNotBlank()) { "$path $method needs a summary" } },
                    Executable { assertTrue(operation.get("description").textValue().isNotBlank()) { "$path $method needs a description" } },
                    Executable {
                        val examples =
                            operation
                                .get("requestBody")
                                .get("content")
                                .get("application/json")
                                .get("examples")
                        assertTrue(examples != null && examples.size() >= 2) { "$path $method needs at least two request examples" }
                    },
                    Executable {
                        val ok = operation.get("responses").get("200")
                        val schema =
                            ok
                                .get("content")
                                .get("application/json")
                                .get("schema")
                                .get("\$ref")
                                .textValue()
                        assertAll(
                            Executable { assertTrue(ok.get("description").textValue().isNotBlank()) { "$path $method needs a 200 description" } },
                            Executable {
                                assertTrue(
                                    schema.endsWith("String")
                                ) { "$path $method should keep the generic result schema, got $schema" }
                            }
                        )
                    },
                    Executable {
                        val badRequest = operation.get("responses").get("400")
                        assertTrue(badRequest != null && badRequest.get("description").textValue().isNotBlank()) {
                            "$path $method needs a documented 400 response"
                        }
                    }
                )
            }
        )
    }

    @Test
    fun healthEndpointIsDocumentedTest() {
        val operation = apiDocs.get("paths").get("/classification-metrics").get("get")
        assertAll(
            Executable { assertTrue(operation.get("summary").textValue().isNotBlank()) },
            Executable {
                assertTrue(
                    operation
                        .get("responses")
                        .get("200")
                        .get("description")
                        .textValue()
                        .isNotBlank()
                )
            }
        )
    }

    @Test
    fun examplesMatchTheirSchemaTest() {
        // An example that does not satisfy its own schema cannot be used by Swagger clients or schema validators, and silently misdocuments the API.
        val schemas = apiDocs.get("components").get("schemas")
        listOf("SingleClassificationResultString", "ClassificationAggregationResultString").forEach { schemaName ->
            val schema = schemas.get(schemaName)
            validate(schema.get("example"), schema, schemas, schemaName)
        }
    }

    @Test
    fun requestExamplesMatchTheirSchemaTest() {
        val schemas = apiDocs.get("components").get("schemas")
        listOf("/classification-metrics", "/classification-metrics/average").forEach { path ->
            val content =
                apiDocs
                    .get("paths")
                    .get(path)
                    .get("post")
                    .get("requestBody")
                    .get("content")
                    .get("application/json")
            val schema =
                schemas.get(
                    content
                        .get("schema")
                        .get("${"\$"}ref")
                        .textValue()
                        .substringAfterLast('/')
                )
            content.get("examples").fields().forEach { (name, example) ->
                validate(example.get("value"), schema, schemas, "$path example '$name'")
            }
        }
    }

    /** Checks that [example] only uses properties the schema declares and that their kinds agree, resolving `${'$'}ref`s along the way. */
    private fun validate(
        example: JsonNode?,
        schema: JsonNode?,
        schemas: JsonNode,
        where: String
    ) {
        if (example == null || schema == null) return
        val resolved = schema.get("${"\$"}ref")?.textValue()?.let { schemas.get(it.substringAfterLast('/')) } ?: schema
        when {
            example.isObject -> {
                val properties = resolved.get("properties") ?: return
                example.fields().forEach { (name, value) ->
                    val property = properties.get(name)
                    assertTrue(property != null) { "$where: the example has a property '$name' that the schema does not declare" }
                    validate(value, property, schemas, "$where.$name")
                }
            }

            example.isArray -> {
                val items = resolved.get("items") ?: return
                example.forEachIndexed { index, item -> validate(item, items, schemas, "$where[$index]") }
            }

            else -> {
                val type = resolved.get("type")?.textValue() ?: return
                val matches =
                    when (type) {
                        "object" -> example.isObject
                        "array" -> example.isArray
                        "string" -> example.isTextual
                        "integer" -> example.isIntegralNumber
                        "number" -> example.isNumber
                        "boolean" -> example.isBoolean
                        else -> true
                    }
                assertTrue(matches || example.isNull) { "$where: the example is ${example.nodeType} but the schema declares '$type'" }
            }
        }
    }

    @Test
    fun noDuplicateResultSchemasTest() {
        // A @Schema(implementation = ...) on a response would add a second, raw-typed copy of the result schemas next to the generic ones.
        val names =
            apiDocs
                .get("components")
                .get("schemas")
                .fieldNames()
                .asSequence()
                .toList()
        assertAll(
            Executable { assertTrue(!names.contains("SingleClassificationResult")) { "raw-typed duplicate schema in $names" } },
            Executable { assertTrue(!names.contains("SingleClassificationResultObject")) { "object-typed duplicate schema in $names" } },
            Executable { assertTrue(!names.contains("ClassificationAggregationResult")) { "raw-typed duplicate schema in $names" } },
            Executable { assertEquals(names.size, names.toSet().size) }
        )
    }
}
