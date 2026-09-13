/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.buildlogic.license

import java.io.File
import java.io.StringReader
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.xml.sax.InputSource

/**
 * Reads POMs from Gradle's module cache (`<gradleUserHome>/caches/modules-2/files-2.1`). A POM is immutable
 * per version and normal dependency resolution always caches it, so the cache is consulted before the
 * network. This also keeps the gate working when a repository is temporarily unreachable.
 */
object GradleModuleCache {
    /** Returns the cached POM file for [coordinate] under [filesRoot], or `null` if it is not cached. */
    fun findPom(
        filesRoot: File,
        coordinate: ModuleCoordinate,
    ): File? {
        val versionDir = File(filesRoot, "${coordinate.group}/${coordinate.name}/${coordinate.version}")
        val fileName = "${coordinate.name}-${coordinate.version}.pom"
        return versionDir
            .listFiles()
            .orEmpty()
            .sortedBy { it.name }
            .map { File(it, fileName) }
            .firstOrNull { it.isFile }
    }
}

/** License-relevant parts of a Maven POM. */
data class PomMetadata(
    val licenses: List<DeclaredLicense>,
    val parent: String?,
)

/** Extracts `<licenses>` and `<parent>` from a POM. DOCTYPEs are rejected to rule out XXE. */
object PomLicenseParser {
    private val WHITESPACE = Regex("""\s+""")

    /** Parses [pomXml]; throws on malformed XML or DOCTYPE declarations. */
    fun parse(pomXml: String): PomMetadata {
        val factory =
            DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = false
                isXIncludeAware = false
                isExpandEntityReferences = false
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            }
        val project = factory.newDocumentBuilder().parse(InputSource(StringReader(pomXml))).documentElement
        val licenses =
            project
                .children("licenses")
                .flatMap { it.children("license") }
                .flatMap { element -> declaredLicenses(element.text("name"), element.text("url")) }
        return PomMetadata(licenses = licenses, parent = project.children("parent").firstOrNull()?.coordinate())
    }

    private fun declaredLicenses(
        name: String?,
        url: String?,
    ): List<DeclaredLicense> {
        val names: List<String?> = name?.let(SpdxNormalizer::alternatives)?.ifEmpty { null } ?: listOf(null)
        return names.map { alternative ->
            DeclaredLicense(alternative, url, SpdxNormalizer.normalize(alternative, url))
        }
    }

    private fun Element.coordinate(): String? {
        val parts = listOf("groupId", "artifactId", "version").map { text(it) }
        return parts
            .takeIf { values -> values.all { !it.isNullOrBlank() && !it.contains("\${") } }
            ?.joinToString(":")
    }

    private fun Element.children(tag: String): List<Element> =
        (0 until childNodes.length).map(childNodes::item).filterIsInstance<Element>().filter { it.tagName == tag }

    private fun Element.text(tag: String): String? =
        children(tag)
            .firstOrNull()
            ?.textContent
            ?.replace(WHITESPACE, " ")
            ?.trim()
            ?.ifEmpty { null }
}

/**
 * Resolves declared licenses for modules, following `<parent>` POMs when a POM declares none
 * (Maven license inheritance).
 *
 * @param fetchPoms returns POM XML text for the requested `group:name:version` coordinates; missing
 *   entries mean the POM could not be retrieved.
 */
class PomLicenseResolver(
    private val fetchPoms: (Collection<String>) -> Map<String, String>,
    private val maxParentDepth: Int = DEFAULT_MAX_PARENT_DEPTH,
) {
    /** Returns the effective declared licenses for each coordinate (empty when none can be found). */
    fun resolve(coordinates: Collection<String>): Map<String, List<DeclaredLicense>> {
        val metadata = HashMap<String, PomMetadata?>()
        var pending: Set<String> = coordinates.toSet()
        repeat(maxParentDepth + 1) {
            val missing = pending.filterNot(metadata::containsKey)
            if (missing.isNotEmpty()) {
                val fetched = fetchPoms(missing)
                missing.forEach { coordinate ->
                    metadata[coordinate] =
                        fetched[coordinate]?.let { xml -> runCatching { PomLicenseParser.parse(xml) }.getOrNull() }
                }
            }
            pending =
                missing
                    .mapNotNull { metadata[it] }
                    .filter { it.licenses.isEmpty() }
                    .mapNotNull { it.parent }
                    .toSet()
        }
        return coordinates.associateWith { effectiveLicenses(it, metadata, depth = 0) }
    }

    private fun effectiveLicenses(
        coordinate: String,
        metadata: Map<String, PomMetadata?>,
        depth: Int,
    ): List<DeclaredLicense> {
        val pom = metadata[coordinate] ?: return emptyList()
        if (pom.licenses.isNotEmpty() || depth >= maxParentDepth) return pom.licenses
        return pom.parent?.let { effectiveLicenses(it, metadata, depth + 1) }.orEmpty()
    }

    companion object {
        const val DEFAULT_MAX_PARENT_DEPTH = 5
    }
}
