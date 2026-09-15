/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.konsist

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Comment
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.Text

/** One translatable resource: a `string`, `plurals` or `string-array`, and whether a translator comment precedes it. */
sealed interface StringEntry {
    val hasComment: Boolean

    /** Every text of the entry, for checks that apply to all of them. */
    val texts: List<String>

    data class Single(
        val text: String,
        override val hasComment: Boolean,
    ) : StringEntry {
        override val texts: List<String> get() = listOf(text)
    }

    /** Plural items keyed by Android quantity (`zero`, `one`, `two`, `few`, `many`, `other`). */
    data class Plural(
        val items: Map<String, String>,
        override val hasComment: Boolean,
    ) : StringEntry {
        override val texts: List<String> get() = items.values.toList()
    }

    data class Array(
        val items: List<String>,
        override val hasComment: Boolean,
    ) : StringEntry {
        override val texts: List<String> get() = items
    }
}

/**
 * The string resources of one `values*` folder.
 *
 * @property resDir the `res` directory relative to the project root, e.g. `feature/about/src/main/res`.
 * @property language the language subtag of the folder qualifier, or `null` for the default (English source) folder.
 */
data class ResourceFolder(
    val resDir: String,
    val language: String?,
    val entries: Map<String, StringEntry>,
)

/** Reads every translatable string resource of a source tree, per `res` directory and language (T-1702). */
object TranslationCatalog {
    private val RESOURCE_TAGS = setOf("string", "plurals", "string-array")

    /** Android configuration qualifiers of two or three letters that are not languages. */
    private val NON_LANGUAGE_QUALIFIERS = setOf("car")
    private val LANGUAGE = Regex("^[a-z]{2,3}$")

    /** Every `values` and language-qualified `values-*` folder under [root], sorted by res dir then language. */
    fun folders(root: File): List<ResourceFolder> =
        StringResources
            .resourceDirectories(root)
            .flatMap { res ->
                res
                    .listFiles { file -> file.isDirectory && file.name.startsWith("values") }
                    .orEmpty()
                    .mapNotNull { folder -> languageOf(folder.name)?.let { folder to it } }
                    .map { (folder, language) ->
                        ResourceFolder(
                            resDir = res.relativeTo(root).invariantSeparatorsPath,
                            language = language.ifEmpty { null },
                            entries = entriesOf(folder),
                        )
                    }
            }.sortedWith(compareBy({ it.resDir }, { it.language.orEmpty() }))

    /**
     * Language of a resource folder name: `""` for `values`, the subtag for `values-fa`, `values-fa-rIR` or
     * `values-b+az+Latn`, and `null` for folders that are not language-qualified (`values-night`, `values-v28`).
     */
    fun languageOf(folderName: String): String? {
        val qualifiers = folderName.removePrefix("values").removePrefix("-")
        val first = qualifiers.substringBefore('-')
        return when {
            folderName == "values" -> ""
            first.startsWith("b+") -> first.removePrefix("b+").substringBefore('+').lowercase()
            LANGUAGE.matches(first) && first !in NON_LANGUAGE_QUALIFIERS -> first
            else -> null
        }
    }

    private fun entriesOf(folder: File): Map<String, StringEntry> =
        folder
            .listFiles { file -> file.extension == "xml" }
            .orEmpty()
            .sortedBy { it.name }
            .flatMap(::entriesOfFile)
            .toMap()

    private fun entriesOfFile(file: File): List<Pair<String, StringEntry>> {
        val factory =
            DocumentBuilderFactory.newInstance().apply {
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            }
        val nodes =
            factory
                .newDocumentBuilder()
                .parse(file)
                .documentElement.childNodes
        var commented = false
        val entries = mutableListOf<Pair<String, StringEntry>>()
        for (index in 0 until nodes.length) {
            val node = nodes.item(index)
            when {
                node is Comment -> {
                    commented = true
                }

                node is Text && node.data.isBlank() -> {
                    // Whitespace between a comment and its resource keeps the comment attached.
                }

                node is Element -> {
                    entryOf(node, commented)?.let { entries += node.getAttribute("name") to it }
                    commented = false
                }

                else -> {
                    commented = false
                }
            }
        }
        return entries
    }

    private fun entryOf(
        element: Element,
        commented: Boolean,
    ): StringEntry? =
        when {
            element.tagName !in RESOURCE_TAGS || element.getAttribute("translatable") == "false" -> {
                null
            }

            element.tagName == "plurals" -> {
                StringEntry.Plural(
                    children(element).associate { it.getAttribute("quantity") to it.textContent },
                    commented,
                )
            }

            element.tagName == "string-array" -> {
                StringEntry.Array(children(element).map { it.textContent }, commented)
            }

            else -> {
                StringEntry.Single(element.textContent, commented)
            }
        }

    private fun children(element: Element): List<Element> =
        (0 until element.childNodes.length)
            .map(element.childNodes::item)
            .filter { it.nodeType == Node.ELEMENT_NODE }
            .filterIsInstance<Element>()
}
