/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.tools.dataset

import java.io.File
import kotlin.system.exitProcess

private const val TABLE_VALID = 0
private const val TABLE_INVALID = 1
private const val USAGE_PROBLEM = 2

/** File name suffix of Islamic Iran override tables (D-07); such files are not event dataset files. */
internal const val OVERRIDES_SUFFIX = "-overrides.json"

/**
 * `validateOverrides <schema.json> <dataset directory>`: validates every `*-overrides.json` file; exit code 0 valid, 1
 * issues, 2 usage error.
 */
fun main(args: Array<String>) {
    exitProcess(runOverridesCli(args.toList()) { println(it) })
}

/** The override validator CLI without process exit: returns the exit code and reports through [print]. */
internal fun runOverridesCli(
    args: List<String>,
    print: (String) -> Unit,
): Int {
    val schemaFile = args.getOrNull(0)?.let(::File)
    val directory = args.getOrNull(1)?.let(::File)
    if (args.size != 2 || schemaFile?.isFile != true || directory?.isDirectory != true) {
        print("usage: validateOverrides <schema.json> <dataset directory> (both must exist)")
        return USAGE_PROBLEM
    }
    val files = overrideFiles(directory)
    val issues = OverridesValidator(schemaFile.readText()).validate(files)
    issues.forEach { print(it.toString()) }
    print("${files.size} override file(s) checked, ${issues.size} issue(s)")
    return if (issues.isEmpty()) TABLE_VALID else TABLE_INVALID
}

/** Every `*-overrides.json` file under [directory], keyed by its relative path, in path order. */
internal fun overrideFiles(directory: File): Map<String, String> =
    directory
        .walkTopDown()
        .filter { it.isFile && it.name.endsWith(OVERRIDES_SUFFIX) }
        .sortedBy { it.invariantSeparatorsPath }
        .associate { it.relativeTo(directory).invariantSeparatorsPath to it.readText() }
