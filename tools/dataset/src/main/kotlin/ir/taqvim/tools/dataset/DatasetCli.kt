/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.tools.dataset

import java.io.File
import kotlin.system.exitProcess

private const val VALID = 0
private const val INVALID = 1
private const val USAGE_ERROR = 2

/** `validate <schema.json> <dataset directory>`: prints every issue; exit code 0 valid, 1 issues, 2 usage error. */
fun main(args: Array<String>) {
    exitProcess(runCli(args.toList()) { println(it) })
}

/** The CLI without process exit: returns the exit code and reports through [print]. */
internal fun runCli(
    args: List<String>,
    print: (String) -> Unit,
): Int {
    val schemaFile = args.getOrNull(0)?.let(::File)
    val directory = args.getOrNull(1)?.let(::File)
    if (args.size != 2 || schemaFile?.isFile != true || directory?.isDirectory != true) {
        print("usage: validate <schema.json> <dataset directory> (both must exist)")
        return USAGE_ERROR
    }
    val files = datasetFiles(schemaFile, directory)
    val issues = DatasetValidator(schemaFile.readText()).validate(files)
    issues.forEach { print(it.toString()) }
    print("${files.size} dataset file(s) checked, ${issues.size} issue(s)")
    return if (issues.isEmpty()) VALID else INVALID
}
