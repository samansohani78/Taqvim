/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.konsist

import java.io.File

/** One translated `strings.xml` and whether it carries the `MT: needs review` marker (docs/i18n/TRANSLATING.md). */
data class MachineTranslationFile(
    val path: String,
    val marked: Boolean,
)

/**
 * The machine-translation review markers and the Weblate component list (T-1702).
 *
 * Both exist so a language can be reviewed as a whole: the marker says a language's text is still machine output, and
 * `.weblate` is what puts a module's strings in front of a translator at all. A module that is in neither is
 * translated by nobody and says so to nobody.
 */
object MachineTranslations {
    private const val MARKER = "MT: needs review"
    private const val SOURCE_FOLDER = "values"

    /** Languages that are not machine-translated: the reviewed source pair (docs/i18n/TRANSLATING.md §1). */
    private val REVIEWED = setOf("", "fa")

    /** Every machine-translated file, by language subtag. */
    fun markersByLanguage(root: File): Map<String, List<MachineTranslationFile>> =
        StringResources
            .resourceDirectories(root)
            .flatMap { res ->
                res
                    .listFiles { file -> file.isDirectory && file.name.startsWith(SOURCE_FOLDER) }
                    .orEmpty()
                    .mapNotNull { folder ->
                        val language = TranslationCatalog.languageOf(folder.name) ?: return@mapNotNull null
                        val file = File(folder, "strings.xml")
                        if (language in REVIEWED || !file.isFile) return@mapNotNull null
                        language to
                            MachineTranslationFile(
                                path = file.relativeTo(root).invariantSeparatorsPath,
                                marked = MARKER in file.readText(),
                            )
                    }
            }.groupBy({ it.first }, { it.second })

    /** Module paths listed under `[components]` in `.weblate`. */
    fun weblateComponents(root: File): Set<String> {
        val lines = File(root, ".weblate").readLines()
        val start = lines.indexOfFirst { it.trim() == "[components]" }
        if (start < 0) return emptySet()
        return lines
            .drop(start + 1)
            .takeWhile { !it.trim().startsWith("[") }
            .mapNotNull { line ->
                line
                    .substringBefore('#')
                    .substringBefore('=')
                    .trim()
                    .ifEmpty { null }
            }.toSet()
    }

    /** Module paths that ship English source strings, which is what makes a module translatable. */
    fun modulesWithSourceStrings(root: File): Set<String> =
        StringResources
            .resourceDirectories(root)
            .filter { File(it, "$SOURCE_FOLDER/strings.xml").isFile }
            .map {
                it.parentFile.parentFile.parentFile
                    .relativeTo(root)
                    .invariantSeparatorsPath
            }.toSet()
}
