/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.wear

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.io.File
import java.util.Properties
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.jupiter.api.Test
import org.w3c.dom.Element

/**
 * T-1600 / T-1804 (ADR-0017, ADR-0019): the watch's merged manifest exports exactly the allowlisted components, each
 * service behind the system permission that binds it, and requests no network, calendar or location permission.
 */
class WearManifestTest {
    private val manifest by lazy { mergedManifest() }

    @Test
    fun `exported components are exactly the allowlist`() {
        exportedComponents(manifest) shouldBe allowlist()
    }

    @Test
    fun `every component with an intent filter declares exported`() {
        components(manifest)
            .filter { !it.hasAttributeNS(ANDROID, "exported") && it.getElementsByTagName("intent-filter").length > 0 }
            .map { it.getAttributeNS(ANDROID, "name") }
            .shouldBeEmpty()
    }

    @Test
    fun `the watch requests no network, calendar or location permission`() {
        val requested = requestedPermissions(manifest)

        requested.intersect(REMOVED_PERMISSIONS).shouldBeEmpty()
    }

    private fun mergedManifest(): File {
        val properties = Properties()
        requireNotNull(javaClass.getResourceAsStream("/com/android/tools/test_config.properties")).use(properties::load)
        return File(requireNotNull(properties.getProperty("android_merged_manifest")))
    }

    private fun allowlist(): Set<String> =
        requireNotNull(javaClass.getResourceAsStream("/security/wear-exported-components.txt"))
            .bufferedReader()
            .readLines()
            .map { it.substringBefore('#').trim() }
            .filter { it.isNotEmpty() }
            .toSet()

    private fun exportedComponents(file: File): Set<String> =
        components(file)
            .filter { it.getAttributeNS(ANDROID, "exported") == "true" }
            .map { "${it.getAttributeNS(ANDROID, "name")} ${it.getAttributeNS(ANDROID, "permission").ifEmpty { "-" }}" }
            .toSet()

    private fun components(file: File): List<Element> {
        val document = parse(file)
        return COMPONENT_TAGS.flatMap { tag ->
            val nodes = document.getElementsByTagName(tag)
            (0 until nodes.length).mapNotNull { nodes.item(it) as? Element }
        }
    }

    private fun requestedPermissions(file: File): Set<String> {
        val nodes = parse(file).getElementsByTagName("uses-permission")
        return (0 until nodes.length)
            .mapNotNull {
                (
                    nodes.item(
                        it,
                    ) as? Element
                )?.getAttributeNS(ANDROID, "name")
            }.toSet()
    }

    private fun parse(file: File) =
        DocumentBuilderFactory
            .newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(file)

    private companion object {
        const val ANDROID = "http://schemas.android.com/apk/res/android"
        val COMPONENT_TAGS = listOf("activity", "activity-alias", "service", "receiver", "provider")
        val REMOVED_PERMISSIONS =
            setOf(
                "android.permission.INTERNET",
                "android.permission.READ_CALENDAR",
                "android.permission.ACCESS_COARSE_LOCATION",
                "android.permission.ACCESS_FINE_LOCATION",
            )
    }
}
