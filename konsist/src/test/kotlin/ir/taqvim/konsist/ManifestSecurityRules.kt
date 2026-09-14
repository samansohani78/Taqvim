/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.konsist

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/** Source-tree security checks of T-1804 (ADR-0017), run without the Android toolchain. */
object ManifestSecurityRules {
    private const val ANDROID = "http://schemas.android.com/apk/res/android"
    private const val TOOLS = "http://schemas.android.com/tools"
    private val COMPONENT_TAGS = listOf("activity", "activity-alias", "service", "receiver", "provider")
    private val SKIPPED_DIRS = setOf("build", ".gradle", ".git", ".kotlin", ".idea")
    private val PENDING_INTENT_CALL = Regex("""PendingIntent\.get(Activity|Broadcast|Service|ForegroundService)\(""")
    private val BROAD_PATH_TAGS = setOf("root-path", "external-path", "external-files-path", "external-media-path")
    private val BROAD_PATHS = setOf("", ".", "/")
    private const val INTERNET = "android.permission.INTERNET"

    /** `manifest: component` for every component in a source manifest that does not declare `android:exported`. */
    fun componentsWithoutExported(root: File): List<String> =
        sourceManifests(root).flatMap { manifest ->
            components(manifest)
                .filterNot { it.hasAttributeNS(ANDROID, "exported") }
                .map { "${manifest.relativeTo(root).invariantSeparatorsPath}: ${it.getAttributeNS(ANDROID, "name")}" }
        }

    /** Source manifests (relative paths) that request network access. */
    fun manifestsRequestingInternet(root: File): List<String> =
        sourceManifests(root)
            .filter { manifest -> permissions(manifest).contains(INTERNET) }
            .map { it.relativeTo(root).invariantSeparatorsPath }

    /** Main Kotlin sources that create a `PendingIntent` without `FLAG_IMMUTABLE`, or with `FLAG_MUTABLE`. */
    fun mutablePendingIntents(root: File): List<String> =
        mainKotlinFiles(root)
            .filter { file ->
                val text = file.readText()
                PENDING_INTENT_CALL.containsMatchIn(text) &&
                    (!text.contains("FLAG_IMMUTABLE") || text.contains("FLAG_MUTABLE"))
            }.map { it.relativeTo(root).invariantSeparatorsPath }

    /** `file: tag path` for every `FileProvider` path that shares more than a dedicated sub-directory. */
    fun broadFileProviderPaths(root: File): List<String> =
        walk(root)
            .filter {
                it.isFile && it.extension == "xml" && it.parentFile.name == "xml" &&
                    it.path.contains(
                        "src/main",
                    )
            }.flatMap { file ->
                val document = parse(file)
                if (document.documentElement.tagName != "paths") return@flatMap emptyList()
                children(document.documentElement)
                    .filter { it.tagName in BROAD_PATH_TAGS || it.getAttribute("path").trim() in BROAD_PATHS }
                    .map {
                        "${file.relativeTo(
                            root,
                        ).invariantSeparatorsPath}: ${it.tagName} '${it.getAttribute("path")}'"
                    }
            }

    private fun sourceManifests(root: File): List<File> =
        walk(root).filter { it.isFile && it.name == "AndroidManifest.xml" && it.parentFile.path.endsWith("src/main") }

    private fun mainKotlinFiles(root: File): List<File> =
        walk(root).filter { it.isFile && it.extension == "kt" && it.invariantSeparatorsPath.contains("/src/main/") }

    private fun walk(root: File): List<File> =
        root
            .walkTopDown()
            .onEnter { it == root || it.name !in SKIPPED_DIRS }
            .toList()

    private fun components(manifest: File): List<Element> {
        val document = parse(manifest)
        return COMPONENT_TAGS.flatMap { tag ->
            val nodes = document.getElementsByTagName(tag)
            (0 until nodes.length).map { nodes.item(it) as Element }
        }
    }

    /** Requested permissions; `tools:node="remove"` entries take a library's permission out and are not requests. */
    private fun permissions(manifest: File): Set<String> {
        val nodes = parse(manifest).getElementsByTagName("uses-permission")
        return (0 until nodes.length)
            .map { nodes.item(it) as Element }
            .filterNot { it.getAttributeNS(TOOLS, "node") == "remove" }
            .map { it.getAttributeNS(ANDROID, "name") }
            .toSet()
    }

    private fun children(element: Element): List<Element> {
        val nodes = element.childNodes
        return (0 until nodes.length).mapNotNull { nodes.item(it) as? Element }
    }

    private fun parse(file: File) =
        DocumentBuilderFactory
            .newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(file)
}
