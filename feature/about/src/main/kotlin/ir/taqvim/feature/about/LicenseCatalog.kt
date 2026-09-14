/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.about

import android.content.res.AssetManager
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** A license of bundled components; [textAsset] is the verbatim text in the app assets, or `null` when not bundled. */
data class LicenseInfo(
    val id: String,
    val name: String,
    val url: String?,
    val textAsset: String?,
)

/** A third-party runtime component (`group:artifact:version`) and the SPDX ids of its licenses. */
data class ThirdPartyComponent(
    val coordinate: String,
    val licenseIds: List<String>,
) {
    /** `group:artifact` without the version. */
    val module: String get() = coordinate.substringBeforeLast(':')

    val version: String get() = coordinate.substringAfterLast(':')
}

/** The license catalog generated from the license gate report (`tools/licenses/about_licenses.py`). */
data class LicenseCatalog(
    val licenses: List<LicenseInfo>,
    val components: List<ThirdPartyComponent>,
)

/** A license with the modules it covers, each with its bundled versions. */
data class LicenseGroup(
    val license: LicenseInfo,
    val modules: ImmutableList<ModuleRow>,
)

/** One module of a [LicenseGroup]: `group:artifact` and its versions, comma separated. */
data class ModuleRow(
    val module: String,
    val versions: String,
)

/** The catalog grouped by license, in catalog order; licenses covering no component are left out. */
internal fun LicenseCatalog.groups(): ImmutableList<LicenseGroup> =
    licenses
        .map { license ->
            val modules =
                components
                    .filter { license.id in it.licenseIds }
                    .groupBy { it.module }
                    .map { (module, items) -> ModuleRow(module, items.joinToString(", ") { it.version }) }
            LicenseGroup(license, modules.toImmutableList())
        }.filter { it.modules.isNotEmpty() }
        .toImmutableList()

/** Parses the catalog asset and checks that every component names known licenses. */
object LicenseCatalogParser {
    const val CATALOG_ASSET: String = "licenses/third-party.json"

    fun parse(json: String): LicenseCatalog {
        val root = Json.parseToJsonElement(json).jsonObject
        val licenses =
            root.array("licenses").map { element ->
                val item = element.jsonObject
                LicenseInfo(item.string("id"), item.string("name"), item.optional("url"), item.optional("text"))
            }
        val components =
            root.array("components").map { element ->
                val item = element.jsonObject
                ThirdPartyComponent(item.string("coordinate"), item.array("licenses").map { it.jsonPrimitive.content })
            }
        val known = licenses.map { it.id }.toSet()
        components.forEach { component ->
            require(component.coordinate.count { it == ':' } == 2) { "bad coordinate ${component.coordinate}" }
            require(component.licenseIds.isNotEmpty() && known.containsAll(component.licenseIds)) {
                "unknown license of ${component.coordinate}"
            }
        }
        return LicenseCatalog(licenses, components)
    }

    private fun JsonObject.array(key: String): JsonArray = requireNotNull(this[key]) { "missing $key" }.jsonArray

    private fun JsonObject.string(key: String): String =
        requireNotNull(this[key]) { "missing $key" }.jsonPrimitive.content

    private fun JsonObject.optional(key: String): String? =
        this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content
}

/** Reads the catalog and license texts from the app assets on [io]. */
class AssetLicenseCatalogSource(
    private val assets: AssetManager,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : LicenseCatalogSource {
    override suspend fun catalog(): LicenseCatalog =
        LicenseCatalogParser.parse(read(LicenseCatalogParser.CATALOG_ASSET))

    override suspend fun text(asset: String): String = read(asset)

    private suspend fun read(asset: String): String =
        withContext(io) { assets.open(asset).bufferedReader().use { it.readText() } }
}
