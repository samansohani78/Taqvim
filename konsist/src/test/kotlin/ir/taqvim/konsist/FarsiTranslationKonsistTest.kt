/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.konsist

import com.lemonappdev.konsist.api.Konsist
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import java.io.File
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** T-204: Persian (`fa`) translation completeness is 100% across every module (docs/i18n/STRINGS.md). */
class FarsiTranslationKonsistTest {
    @Test
    fun `Persian translations are complete in every module`() {
        val root = File(Konsist.projectRootPath)

        StringResources.defaultCount(root) shouldBeGreaterThan 0
        StringResources.missingFarsi(root).shouldBeEmpty()
    }

    @Test
    fun `missing Persian resources are detected per module`(
        @TempDir root: File,
    ) {
        write(
            File(root, "feature/month/src/main/res/values/strings.xml"),
            """<string name="a">A</string><string name="b">B</string>""" +
                """<string name="c" translatable="false">C</string>""" +
                """<plurals name="d"><item quantity="other">D</item></plurals>""",
        )
        write(File(root, "feature/month/src/main/res/values-fa/strings.xml"), """<string name="a">A</string>""")
        write(File(root, "feature/month/build/intermediates/res/values/strings.xml"), """<string name="e">E</string>""")

        StringResources.missingFarsi(root) shouldBe
            listOf("feature/month/src/main/res: b", "feature/month/src/main/res: d")
        StringResources.defaultCount(root) shouldBe 3
    }

    private fun write(
        file: File,
        body: String,
    ) {
        file.parentFile.mkdirs()
        file.writeText("<resources>$body</resources>")
    }
}
