/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.i18n

import java.util.Properties

/** The package of the bundled resources, spelled out because R8 moves the classes of a release build elsewhere. */
private const val RESOURCE_PACKAGE = "/ir/taqvim/core/i18n/"

/** The UTF-8 `.properties` resource [name] of this package as a map. */
internal fun loadPropertiesResource(name: String): Map<String, String> {
    val path = RESOURCE_PACKAGE + name
    val stream = checkNotNull(LanguageTable::class.java.getResourceAsStream(path)) { "$name is missing" }
    val properties = Properties()
    stream.reader(Charsets.UTF_8).use { properties.load(it) }
    return properties.stringPropertyNames().associateWith { properties.getProperty(it) }
}
