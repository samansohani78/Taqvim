/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.konsist

import com.lemonappdev.konsist.api.Konsist
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.io.File
import org.junit.jupiter.api.Test

/**
 * T-1800: the committed baseline profile must cover the generated dataset classes.
 *
 * The app builds the whole official catalogue while it starts, so a freshly generated profile names every
 * `OfficialEventsPart` class. Growing the dataset generates more of them and the profile keeps covering only those
 * that existed when it was last generated: after main@0855d52 took the UN records from 102 to 236, the committed
 * profile still stopped at `Part17` while the sources reached `Part38`, so 21 classes had no AOT rules.
 *
 * What that costs is **not** established. Measured on a software-rendered x86 emulator, three interleaved pairs on
 * one commit with only the profile differing, the regenerated profile was within noise of the stale one
 * (`startupCold` 270 ms against 275 ms). A profile's benefit belongs to a real arm64 device, which this project has
 * never measured on (see the baseline note in docs/STATUS_REPORT.md), so this check exists to keep the profile
 * honest rather than to defend a number: shipping AOT rules that describe a smaller app than the one in the APK is
 * a defect whatever it measures here.
 *
 * Nothing else notices. `check<Variant>BaselineProfile` only counts how many rules name the app, which a stale
 * profile still passes, and the nightly benchmark compares against a baseline recorded on an earlier runner.
 */
class BaselineProfileFreshnessKonsistTest {
    private val root = File(Konsist.projectRootPath)

    @Test
    fun `the baseline profile covers every generated dataset class`() {
        val profile = root.resolve(PROFILE).readText()
        withClue(
            "These generated classes have no rules in $PROFILE, so the profile describes a smaller app than the " +
                "APK ships: regenerate it with `./gradlew :app:generateBaselineProfile` and commit the result " +
                "(T-1800, ADR-0018 addendum).",
        ) {
            missingFrom(profile, generatedClasses()).shouldBeEmpty()
        }
    }

    @Test
    fun `the profile and the generated sources are both found`() {
        // A check that silently reads nothing would pass for ever; both inputs must exist and be non-trivial.
        root.resolve(PROFILE).isFile shouldBe true
        (generatedClasses().size >= MINIMUM_GENERATED_CLASSES) shouldBe true
    }

    @Test
    fun `the check flags a class the profile never names`() {
        val profile =
            """
            HSPLir/taqvim/data/events/generated/OfficialEventsPart1Kt;-><clinit>()V
            HSPLir/taqvim/data/events/generated/OfficialEventsPart2;->events()Ljava/util/List;
            """.trimIndent()
        // The first is present as a `…Kt` facade, the second as a plain class; the third is absent, and
        // `OfficialEventsPart1` must not be satisfied by a line naming `OfficialEventsPart10Kt`.
        missingFrom(profile, listOf("OfficialEventsPart1", "OfficialEventsPart2")).shouldBeEmpty()
        missingFrom(profile, listOf("OfficialEventsPart2", "OfficialEventsPart3")) shouldBe
            listOf("OfficialEventsPart3")
        missingFrom("HSPLir/taqvim/OfficialEventsPart10Kt;-><clinit>()V", listOf("OfficialEventsPart1")) shouldBe
            listOf("OfficialEventsPart1")
    }

    /** The generated catalogue classes the dataset produces, by simple name. */
    private fun generatedClasses(): List<String> =
        root
            .resolve(GENERATED_SOURCES)
            .listFiles()
            .orEmpty()
            .filter { it.isFile && it.name.startsWith(GENERATED_PREFIX) && it.extension == "kt" }
            .map { it.nameWithoutExtension }
            .sorted()

    /**
     * The [classes] that [profile] never names.
     *
     * A file of top-level declarations compiles to a `…Kt` facade, which is the form these generated catalogues take;
     * the plain name is accepted too, so the check still holds if one ever becomes a real class. The trailing `;` of
     * the JVM descriptor is required, or `OfficialEventsPart1` would match `OfficialEventsPart10`.
     */
    private fun missingFrom(
        profile: String,
        classes: List<String>,
    ): List<String> = classes.filterNot { profile.contains("$it;") || profile.contains("${it}Kt;") }

    private companion object {
        const val PROFILE = "app/src/main/generated/baselineProfiles/baseline-prof.txt"
        const val GENERATED_SOURCES = "data/events/src/main/kotlin/ir/taqvim/data/events/generated"
        const val GENERATED_PREFIX = "OfficialEventsPart"

        /** The catalogue has been split into dozens of classes since D-05; a handful would mean a wrong path. */
        const val MINIMUM_GENERATED_CLASSES = 10
    }
}
