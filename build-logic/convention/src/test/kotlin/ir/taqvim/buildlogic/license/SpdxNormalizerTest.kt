/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.buildlogic.license

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class SpdxNormalizerTest {
    private val names =
        listOf(
            "The Apache Software License, Version 2.0" to "Apache-2.0",
            "Apache License, Version 2.0" to "Apache-2.0",
            "Apache-2.0" to "Apache-2.0",
            "ASL 2.0" to "Apache-2.0",
            "The MIT License" to "MIT",
            "MIT License" to "MIT",
            "Bouncy Castle Licence" to "MIT",
            "MIT-0" to "MIT-0",
            "BSD-3-Clause" to "BSD-3-Clause",
            "New BSD License" to "BSD-3-Clause",
            "BSD License 3" to "BSD-3-Clause",
            "Go License" to "BSD-3-Clause",
            "Eclipse Distribution License - v 1.0" to "BSD-3-Clause",
            "The BSD 2-Clause License" to "BSD-2-Clause",
            "ISC License" to "ISC",
            "Unicode/ICU License" to "Unicode-3.0",
            "Unicode-DFS-2016" to "Unicode-DFS-2016",
            "CC0" to "CC0-1.0",
            "Public Domain" to "LicenseRef-Public-Domain",
            "Eclipse Public License - v 2.0" to "EPL-2.0",
            "EPL 2.0" to "EPL-2.0",
            "Eclipse Public License 1.0" to "EPL-1.0",
            "GNU Lesser General Public License" to "LGPL-2.1-or-later",
            "LGPL-2.1-or-later" to "LGPL-2.1-or-later",
            "GNU Affero General Public License v3" to "AGPL-3.0-or-later",
            "GNU General Public License, version 2 with the GNU Classpath Exception" to
                "GPL-2.0-with-classpath-exception",
            "GNU General Public License v3.0" to "GPL-2.0-or-later",
            "Mozilla Public License 2.0" to "MPL-2.0",
            "CDDL + GPLv2 with classpath exception" to "GPL-2.0-with-classpath-exception",
            "COMMON DEVELOPMENT AND DISTRIBUTION LICENSE (CDDL) Version 1.1" to "CDDL-1.1",
        )

    @TestFactory
    fun `maps license names`() =
        names.map { (name, expected) ->
            DynamicTest.dynamicTest(name) { SpdxNormalizer.normalize(name, null) shouldBe expected }
        }

    @Test
    fun `copyleft is recognised even when a permissive word appears`() {
        SpdxNormalizer.normalize("GNU Lesser General Public License (MIT-style header)", null) shouldBe
            "LGPL-2.1-or-later"
    }

    @Test
    fun `falls back to the url when the name is unknown`() {
        SpdxNormalizer.normalize("Custom", "https://www.apache.org/licenses/LICENSE-2.0.txt") shouldBe "Apache-2.0"
        SpdxNormalizer.normalize(null, "https://opensource.org/licenses/MIT") shouldBe "MIT"
        SpdxNormalizer.normalize(null, "https://www.eclipse.org/legal/epl-v20.html") shouldBe "EPL-2.0"
        SpdxNormalizer.normalize(null, "http://www.eclipse.org/legal/epl-v10.html") shouldBe "EPL-1.0"
        SpdxNormalizer.normalize(null, "https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html") shouldBe
            "LGPL-2.1-or-later"
        SpdxNormalizer.normalize(null, "https://www.gnu.org/licenses/gpl-3.0.html") shouldBe "GPL-2.0-or-later"
    }

    @Test
    fun `ambiguous or missing licenses stay unknown`() {
        SpdxNormalizer.normalize("BSD", null) shouldBe null
        SpdxNormalizer.normalize("Android Software Development Kit License", null) shouldBe null
        SpdxNormalizer.normalize(null, null) shouldBe null
        SpdxNormalizer.normalize("  ", "https://example.com/license") shouldBe null
    }

    @Test
    fun `splits spdx disjunctions`() {
        SpdxNormalizer.alternatives("(EPL-2.0 OR GPL-2.0-with-classpath-exception)") shouldContainExactly
            listOf("EPL-2.0", "GPL-2.0-with-classpath-exception")
        SpdxNormalizer.alternatives("Apache-2.0") shouldContainExactly listOf("Apache-2.0")
        SpdxNormalizer.alternatives("  ") shouldContainExactly emptyList()
    }
}
