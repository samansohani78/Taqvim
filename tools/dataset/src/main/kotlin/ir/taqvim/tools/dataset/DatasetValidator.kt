/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.tools.dataset

import com.networknt.schema.Schema
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SpecificationVersion
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import tools.jackson.databind.json.JsonMapper

/**
 * Validates dataset files (D-01): first each file against the JSON Schema [schemaText] (draft 2020-12), then the
 * records of all structurally valid files together with [SemanticChecks]. Never throws for any input text.
 */
class DatasetValidator(
    schemaText: String,
) {
    private val schema: Schema =
        SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12).getSchema(schemaText)
    private val mapper = JsonMapper()

    /** Issues of [files] (file name → JSON text) validated as one dataset; empty when the dataset is valid. */
    fun validate(files: Map<String, String>): List<DatasetIssue> {
        val checked = files.map { (name, text) -> checkFile(name, text) }
        val structural = checked.flatMap { it.issues }
        val records = checked.filter { it.issues.isEmpty() }.flatMap { it.records }
        return structural + SemanticChecks.check(records)
    }

    private fun checkFile(
        name: String,
        text: String,
    ): FileResult {
        val tree = runCatching { mapper.readTree(text) }.getOrNull()
        val document = runCatching { Json.parseToJsonElement(text) }.getOrNull()
        if (tree == null || document == null) {
            return FileResult(listOf(DatasetIssue(name, "$", IssueKind.MALFORMED_JSON, MALFORMED_MESSAGE)))
        }
        val schemaIssues =
            runCatching { schema.validate(tree) }
                .map { errors ->
                    errors.map { DatasetIssue(name, it.instanceLocation.toString(), IssueKind.SCHEMA, it.message) }
                }.getOrElse {
                    listOf(
                        DatasetIssue(name, "$", IssueKind.SCHEMA, "schema evaluation failed: ${it.message}"),
                    )
                }
        val events = ((document as? JsonObject)?.get("events") as? JsonArray).orEmpty()
        val records =
            events.mapIndexedNotNull { index, event ->
                (event as? JsonObject)?.let { EventRecord(name, "$.events[$index]", it) }
            }
        return FileResult(schemaIssues, records)
    }

    private data class FileResult(
        val issues: List<DatasetIssue>,
        val records: List<EventRecord> = emptyList(),
    )

    private companion object {
        const val MALFORMED_MESSAGE = "not a single well-formed JSON document"
    }
}
