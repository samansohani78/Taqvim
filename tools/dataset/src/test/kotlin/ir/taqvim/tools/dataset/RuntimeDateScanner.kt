/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.tools.dataset

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import org.w3c.dom.Element
import org.w3c.dom.Node

/**
 * Finds calendar dates typed into the files a build packages (REVIEW R09), for [NoPerYearManualDataTest].
 *
 * Runtime inputs are every source set that is not a test set (`main`, `debug`, any future `release` or flavour set) of
 * every shipped module: all of `res/` with its qualifiers, `assets/`, `resources/`, and the Kotlin sources. Text files are
 * searched for ISO dates outside comments; XML is also parsed for elements whose attributes name a year, month and day;
 * JSON is parsed for objects with numeric year, month and day; and Kotlin files are judged by what they hold rather than
 * by their names, so a table of date literals in an innocently named file is found too.
 */
internal class RuntimeDateScanner(
    private val root: File,
) {
    /** `path:line: text` for every date found in a packaged runtime data file not matched by [allowed]. */
    fun datedData(
        allowed: Collection<Regex>,
        extraFiles: List<File> = emptyList(),
    ): List<String> =
        (runtimeDataFiles() + extraFiles)
            .filterNot { file -> allowed.any { it.matches(relative(file)) } }
            .flatMap(::datesIn)

    /** Runtime Kotlin sources that hold a table of date literals, with the count, unless matched by [allowed]. */
    fun dateTables(allowed: Collection<Regex>): List<String> =
        runtimeSourceSets()
            .flatMap { File(it, "kotlin").walkTopDown().filter { f -> f.isFile && f.extension == "kt" }.toList() }
            .filterNot { file -> allowed.any { it.matches(relative(file)) } }
            .mapNotNull { file ->
                val count = dateLiteralCount(file.readText())
                if (count >= TABLE_THRESHOLD) "${relative(file)}: $count date literals" else null
            }

    /** Every file under the runtime `res`, `assets` and `resources` directories, text formats only. */
    fun runtimeDataFiles(): List<File> =
        runtimeSourceSets()
            .flatMap { set -> DATA_DIRECTORIES.map { File(set, it) } }
            .filter { it.isDirectory }
            .flatMap { directory -> directory.walkTopDown().filter { it.isFile && it.isText() }.toList() }

    /** Kotlin source roots of the runtime source sets (`src/<set>/kotlin`). */
    fun runtimeKotlinRoots(): List<File> = runtimeSourceSets().map { File(it, "kotlin") }.filter { it.isDirectory }

    private fun runtimeSourceSets(): List<File> =
        moduleDirectories().flatMap { module ->
            File(module, "src").listFiles().orEmpty().filter { it.isDirectory && !isTestSet(it.name) }
        }

    private fun moduleDirectories(): List<File> =
        root
            .walkTopDown()
            .onEnter { it.name !in SKIPPED_DIRECTORIES && !it.name.startsWith(".") }
            .filter { it.isDirectory && File(it, BUILD_SCRIPT).isFile }
            .filterNot { relative(it).substringBefore('/') in BUILD_ONLY_ROOTS }
            .toList()

    private fun datesIn(file: File): List<String> {
        val text = file.readText()
        val lines =
            text
                .lines()
                .withIndex()
                .filterNot { (_, line) -> COMMENT_PREFIXES.any { line.trimStart().startsWith(it) } }
                .filter { (_, line) -> ISO_DATE.containsMatchIn(line) }
                .map { (index, line) -> "${relative(file)}:${index + 1}: ${line.trim().take(PREVIEW)}" }
        val structured =
            when (file.extension) {
                "json" -> jsonDateObjects(text).map { "${relative(file)}: JSON date object $it" }
                "xml" -> xmlDateElements(file).map { "${relative(file)}: XML date element <$it>" }
                else -> emptyList()
            }
        return lines + structured
    }

    private fun relative(file: File): String = file.relativeTo(root).invariantSeparatorsPath

    companion object {
        /** A file with at least this many date literals is a table (single epochs and anchors stay below it). */
        const val TABLE_THRESHOLD = 4
        const val BUILD_SCRIPT = "build.gradle.kts"
        const val PREVIEW = 80
        val ISO_DATE = Regex("""\b\d{4}-\d{2}-\d{2}\b""")
        val COMMENT_PREFIXES = listOf("#", "!", "//", "*", "/*", "<!--")
        val DATA_DIRECTORIES = listOf("res", "assets", "resources")
        val SKIPPED_DIRECTORIES = setOf("build", "node_modules", "usno-data", "gradle")

        /** Modules that never ship in the app: dataset tooling, build logic, lint rules, architecture and benchmarks. */
        val BUILD_ONLY_ROOTS = setOf("tools", "build-logic", "lint", "konsist", "benchmark")
        private val TEXT_EXTENSIONS = setOf("xml", "json", "txt", "tsv", "csv", "properties", "yaml", "yml", "ics")
        private val DATE_KEYS = listOf("year", "month", "day")

        /** `(year, month, day)` literal triples with a four-digit year, and ISO date strings, in Kotlin code. */
        private val DATE_TRIPLE = Regex("""\(\s*(?:\w+\s*,\s*)?[1-2]\d{3}\s*,\s*\d{1,2}\s*,\s*\d{1,2}\s*\)""")
        private val DATE_STRING = Regex(""""\d{4}-\d{2}-\d{2}"""")
        private val LINE_COMMENT = Regex("""//.*""")

        /** Resource XML never declares a DOCTYPE; refusing one keeps the scan from resolving external entities. */
        private val XML_FACTORY =
            DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            }
        private val BLOCK_COMMENT = Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL)

        fun isTestSet(name: String): Boolean = name.startsWith("test") || name.startsWith("androidTest")

        fun File.isText(): Boolean = extension in TEXT_EXTENSIONS

        /** How many date literals the Kotlin [source] holds outside comments. */
        fun dateLiteralCount(source: String): Int {
            val code = source.replace(BLOCK_COMMENT, "").replace(LINE_COMMENT, "")
            return DATE_TRIPLE.findAll(code).count() + DATE_STRING.findAll(code).count()
        }

        /** Every JSON object in [text] with numeric year, month and day, as its compact text. */
        fun jsonDateObjects(text: String): List<String> {
            val document = runCatching { Json.parseToJsonElement(text) }.getOrNull() ?: return emptyList()
            return objectsOf(document).filter(::isDateObject).map { it.toString().take(PREVIEW) }
        }

        /** The tag names of the XML elements in [file] whose attributes give a year, a month and a day. */
        fun xmlDateElements(file: File): List<String> {
            val document =
                runCatching { XML_FACTORY.newDocumentBuilder().parse(file) }.getOrNull() ?: return emptyList()
            return elementsOf(document.documentElement).filter(::isDateElement).map { it.tagName }
        }

        private fun objectsOf(element: JsonElement): List<JsonObject> =
            when (element) {
                is JsonObject -> listOf(element) + element.values.flatMap(::objectsOf)
                is JsonArray -> element.flatMap(::objectsOf)
                else -> emptyList()
            }

        private fun isDateObject(candidate: JsonObject): Boolean =
            DATE_KEYS.all { key -> (candidate[key] as? JsonPrimitive)?.intOrNull != null }

        private fun elementsOf(element: Element): List<Element> {
            val children = element.childNodes
            val nested =
                (0 until children.length)
                    .map(children::item)
                    .filter { it.nodeType == Node.ELEMENT_NODE }
                    .flatMap { elementsOf(it as Element) }
            return listOf(element) + nested
        }

        private fun isDateElement(element: Element): Boolean {
            val names =
                (0 until element.attributes.length).map {
                    element.attributes.item(it).localName
                        ?: element.attributes.item(it).nodeName
                }
            return DATE_KEYS.all { key -> names.any { it.equals(key, ignoreCase = true) } }
        }
    }
}
