/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.security

import java.io.File
import java.util.Properties
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/** An exported component of a merged manifest and the permission that guards it (`-` when none). */
data class ExportedComponent(
    val name: String,
    val permission: String,
)

/** One line of `security/exported-components.txt`. */
data class AllowedComponent(
    val component: ExportedComponent,
    val builds: String,
)

/** Reads the merged manifest that Robolectric uses for this module's unit tests, and the export allowlist. */
object MergedManifest {
    private const val ANDROID = "http://schemas.android.com/apk/res/android"
    private val COMPONENT_TAGS = listOf("activity", "activity-alias", "service", "receiver", "provider")
    private const val NO_PERMISSION = "-"
    private const val ALLOWLIST_COLUMNS = 4
    private const val TEST_CONFIG = "com/android/tools/test_config.properties"

    /** The AGP-generated merged manifest of the debug unit tests (`com/android/tools/test_config.properties`). */
    fun unitTestManifest(): File {
        val properties = Properties()
        val config = requireNotNull(MergedManifest::class.java.getResourceAsStream("/$TEST_CONFIG"))
        config.use(properties::load)
        return File(requireNotNull(properties.getProperty("android_merged_manifest")))
    }

    fun exportedComponents(manifest: File): Set<ExportedComponent> =
        components(manifest)
            .filter { it.getAttributeNS(ANDROID, "exported") == "true" }
            .map { ExportedComponent(it.getAttributeNS(ANDROID, "name"), permissionOf(it)) }
            .toSet()

    /** Components that declare no `android:exported` although they have an intent filter. */
    fun implicitlyExported(manifest: File): List<String> =
        components(manifest)
            .filter { !it.hasAttributeNS(ANDROID, "exported") && it.getElementsByTagName("intent-filter").length > 0 }
            .map { it.getAttributeNS(ANDROID, "name") }

    fun requestedPermissions(manifest: File): Set<String> =
        document(manifest)
            .getElementsByTagName("uses-permission")
            .let { nodes -> (0 until nodes.length).map { (nodes.item(it) as Element).getAttributeNS(ANDROID, "name") } }
            .toSet()

    fun allowlist(text: String): List<AllowedComponent> =
        text
            .lines()
            .map(String::trim)
            .filterNot { it.isEmpty() || it.startsWith("#") }
            .map { line ->
                val columns = line.split('|').map(String::trim)
                require(columns.size == ALLOWLIST_COLUMNS) { "allowlist line needs $ALLOWLIST_COLUMNS columns: $line" }
                AllowedComponent(ExportedComponent(columns[0], columns[1]), columns[2])
            }

    private fun permissionOf(element: Element): String =
        element.getAttributeNS(ANDROID, "permission").ifEmpty { NO_PERMISSION }

    private fun components(manifest: File): List<Element> {
        val document = document(manifest)
        return COMPONENT_TAGS.flatMap { tag ->
            val nodes = document.getElementsByTagName(tag)
            (0 until nodes.length).map { nodes.item(it) as Element }
        }
    }

    private fun document(manifest: File) =
        DocumentBuilderFactory
            .newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(manifest)
}
