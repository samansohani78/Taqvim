/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.security

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.io.File
import org.junit.jupiter.api.Test

/**
 * REVIEW R18: `docs/SECURITY.md` had drifted from the app — it named two exported components where the release
 * manifest has 54, and said Taqvim code never uses `android.util.Log`. It is now checked against the export allowlist
 * (itself checked against the merged manifest by [ExportedComponentsTest]) and against the sources, so a new exported
 * component, a new guard permission or a new log call fails until the document says so.
 */
class SecurityDocumentTest {
    private val document = File("../docs/SECURITY.md").readText()
    private val released =
        MergedManifest
            .allowlist(File("src/test/resources/security/exported-components.txt").readText())
            .filter { it.builds == RELEASE }
            .map { it.component }

    @Test
    fun `the document states how many components a release exports`() {
        EXPORT_COUNT
            .find(document)
            ?.groupValues
            ?.get(1)
            ?.toInt() shouldBe released.size
    }

    @Test
    fun `every exported component and its guard permission is named in the document`() {
        val launcherDays = released.map { it.name }.filter { LAUNCHER_DAY.matches(it.substringAfterLast('.')) }
        val unnamed =
            released
                .filterNot { it.name in launcherDays }
                .flatMap { component ->
                    listOfNotNull(
                        component.name.substringAfterLast('.').takeIf { it !in document },
                        component.permission.substringAfterLast('.').takeIf { it != NO_PERMISSION && it !in document },
                    )
                }
        val lastDay = launcherDays.map { it.substringAfterLast('.') }.maxOrNull()
        unnamed.shouldBeEmpty()
        (lastDay == null || (FIRST_DAY in document && lastDay in document)) shouldBe true
    }

    @Test
    fun `every source file that logs through android util Log is documented`() {
        // canonicalFile: the walk must not start at "..", whose name the hidden-directory filter would skip.
        val root = File("..").canonicalFile
        val sources =
            root
                .walkTopDown()
                .onEnter { it.name !in SKIPPED && !it.name.startsWith(".") }
                .filter { it.isFile && it.extension == "kt" && MAIN_SOURCE in it.invariantSeparatorsPath }
                .filterNot { it.relativeTo(root).invariantSeparatorsPath.substringBefore('/') in BUILD_ONLY_ROOTS }
                .toList()
        val undocumented = sources.filter { ANDROID_LOG in it.readText() }.map { it.name }.filterNot { it in document }
        (sources.size > MIN_MAIN_SOURCES) shouldBe true
        undocumented.shouldBeEmpty()
    }

    private companion object {
        const val RELEASE = "all"
        const val NO_PERMISSION = "-"
        const val FIRST_DAY = "LauncherDay01"
        const val MAIN_SOURCE = "/src/main/"
        const val ANDROID_LOG = "import android.util.Log"

        /** The walk must see the app's sources; far fewer means it scanned the wrong directory. */
        const val MIN_MAIN_SOURCES = 400
        val EXPORT_COUNT = Regex("""release manifest exports \*\*(\d+)\*\* components""")
        val LAUNCHER_DAY = Regex("""LauncherDay\d{2}""")
        val SKIPPED = setOf("build", "node_modules", "gradle", "usno-data")

        /** Modules that never ship: build logic, lint rules, architecture tests (with planted violations), tools. */
        val BUILD_ONLY_ROOTS = setOf("tools", "build-logic", "lint", "konsist", "benchmark")
    }
}
