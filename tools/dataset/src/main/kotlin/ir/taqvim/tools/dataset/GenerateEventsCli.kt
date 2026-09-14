/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.tools.dataset

import java.io.File
import kotlin.system.exitProcess

private const val GENERATED = 0
private const val DATASET_INVALID = 1
private const val WRONG_USAGE = 2
private const val ARGUMENT_COUNT = 3
private const val USAGE = "usage: generateEvents <schema.json> <dataset directory> <output directory>"

/**
 * `generateEvents <schema.json> <dataset directory> <output directory>`: exit code 0 when written, 1 when the dataset
 * is invalid (nothing is written), 2 on wrong usage.
 */
fun main(args: Array<String>) {
    exitProcess(runGenerateCli(args.toList()) { println(it) })
}

/** The generator CLI without process exit: validates first and refuses to generate from an invalid dataset. */
internal fun runGenerateCli(
    args: List<String>,
    print: (String) -> Unit,
): Int {
    val schemaFile = args.getOrNull(0)?.let(::File)?.takeIf { it.isFile }
    val directory = args.getOrNull(1)?.let(::File)?.takeIf { it.isDirectory }
    val output = args.getOrNull(2)?.let(::File)?.takeIf { args.size == ARGUMENT_COUNT }
    if (schemaFile == null || directory == null || output == null) {
        print(USAGE)
        return WRONG_USAGE
    }
    val files = datasetFiles(schemaFile, directory)
    val issues = DatasetValidator(schemaFile.readText()).validate(files)
    if (issues.isNotEmpty()) {
        issues.forEach { print(it.toString()) }
        print("refusing to generate: ${issues.size} issue(s)")
        return DATASET_INVALID
    }
    val sources = EventsCodeGenerator.generate(files)
    output.mkdirs()
    val names = sources.map { it.fileName }.toSet()
    output.listFiles { file -> file.extension == "kt" && file.name !in names }.orEmpty().forEach { it.delete() }
    sources.forEach { File(output, it.fileName).writeText(it.content) }
    print("generated ${sources.size} source file(s) from ${files.size} dataset file(s)")
    return GENERATED
}

/** Schema files such as `events.v1.json` or `islamic-iran-overrides.v1.json`. */
private val SCHEMA_FILE_NAME = Regex("""\.v\d+\.json$""")

/**
 * Every event dataset file under [directory], keyed by its relative path, in path order: `*.json` except [schemaFile],
 * other schema files (`*.vN.json`) and Islamic Iran override tables (`*-overrides.json`, D-07).
 */
internal fun datasetFiles(
    schemaFile: File,
    directory: File,
): Map<String, String> =
    directory
        .walkTopDown()
        .filter { it.isFile && it.extension == "json" && it.canonicalFile != schemaFile.canonicalFile }
        .filterNot { SCHEMA_FILE_NAME.containsMatchIn(it.name) || it.name.endsWith(OVERRIDES_SUFFIX) }
        .sortedBy { it.invariantSeparatorsPath }
        .associate { it.relativeTo(directory).invariantSeparatorsPath to it.readText() }
