/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.konsist

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/** Reads Android string resources of a source tree without the Android toolchain (T-204). */
object StringResources {
    private const val DEFAULT_FOLDER = "values"
    private const val FARSI_FOLDER = "values-fa"
    private val RESOURCE_TAGS = setOf("string", "plurals", "string-array")
    private val SKIPPED_DIRS = setOf("build", ".gradle", ".git", ".kotlin", ".idea", "resources")

    /** `resDir: name` for every translatable default resource with no counterpart in `values-fa`, sorted. */
    fun missingFarsi(root: File): List<String> =
        resourceDirectories(root)
            .flatMap { res ->
                val missing = names(File(res, DEFAULT_FOLDER)) - names(File(res, FARSI_FOLDER))
                missing.map { "${res.relativeTo(root).invariantSeparatorsPath}: $it" }
            }.sorted()

    /** Number of translatable resources in every default `values` folder under [root]. */
    fun defaultCount(root: File): Int = resourceDirectories(root).sumOf { names(File(it, DEFAULT_FOLDER)).size }

    /** Every `res` directory under [root] that has a default `values` folder, skipping build and tool folders. */
    fun resourceDirectories(root: File): List<File> =
        root
            .walkTopDown()
            .onEnter { it == root || it.name !in SKIPPED_DIRS }
            .filter { it.isDirectory && it.name == "res" && File(it, DEFAULT_FOLDER).isDirectory }
            .toList()

    private fun names(folder: File): Set<String> =
        folder
            .listFiles { file -> file.extension == "xml" }
            .orEmpty()
            .flatMap(::translatableNames)
            .toSet()

    private fun translatableNames(file: File): List<String> {
        val factory =
            DocumentBuilderFactory.newInstance().apply {
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            }
        val resources = factory.newDocumentBuilder().parse(file).documentElement
        return (0 until resources.childNodes.length)
            .map(resources.childNodes::item)
            .filterIsInstance<Element>()
            .filter { it.tagName in RESOURCE_TAGS && it.getAttribute("translatable") != "false" }
            .map { it.getAttribute("name") }
    }
}
