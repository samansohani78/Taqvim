/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.i18n

import java.util.Properties

/** The UTF-8 `.properties` resource [name] of this package as a map. */
internal fun loadPropertiesResource(name: String): Map<String, String> {
    val stream = checkNotNull(LanguageTable::class.java.getResourceAsStream(name)) { "$name is missing" }
    val properties = Properties()
    stream.reader(Charsets.UTF_8).use { properties.load(it) }
    return properties.stringPropertyNames().associateWith { properties.getProperty(it) }
}
