/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.konsist

import com.lemonappdev.konsist.api.Konsist
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * R8 moves the classes of a release build into another package, while the bundled `.properties`, `.tsv` and `.json`
 * files keep their own. `Class.getResourceAsStream` resolves a name without a leading slash against the package of the
 * (renamed) class, so a relative name reads nothing in a shrunk build: the app crashed at start with
 * "languages.properties is missing" on the macrobenchmark emulator (T-1801, run 35300935866) while every JVM test and
 * the debug build passed. Production lookups therefore spell the path out from the classpath root.
 */
class ClasspathResourceKonsistTest {
    @Test
    fun `production resource lookups use an absolute path`() {
        Konsist
            .scopeFromProduction()
            .files
            .filterNot { it.path.contains("/build-logic/") }
            .flatMap { file -> relativeLookups(file.text).map { "${file.path}: $it" } }
            .shouldBeEmpty()
    }

    @Test
    fun `the check flags a relative name and accepts absolute ones`() {
        relativeLookups("""val s = A::class.java.getResourceAsStream("cities.tsv")""") shouldBe listOf("cities.tsv")
        relativeLookups("""val s = A::class.java.getResourceAsStream("/ir/cities.tsv")""").shouldBeEmpty()
        val indirect =
            """
            private const val DIR = "/ir/taqvim/"
            val s = A::class.java.getResourceAsStream(DIR + name)
            """
        relativeLookups(indirect).shouldBeEmpty()
        relativeLookups("""val s = A::class.java.classLoader?.getResourceAsStream(name)""").shouldBeEmpty()
    }

    /** The resource paths of `Class.getResourceAsStream` calls in [source] that do not start at the classpath root. */
    private fun relativeLookups(source: String): List<String> =
        LOOKUP
            .findAll(source)
            .mapNotNull { literalFor(it.groupValues[1], source) }
            .filterNot { it.startsWith("/") }
            .toList()

    /** The string [expression] stands for, following one `val` and taking the first part of a concatenation. */
    private fun literalFor(
        expression: String,
        source: String,
        depth: Int = MAX_DEPTH,
    ): String? {
        val head = expression.split('+').first().trim()
        return when {
            head.startsWith("\"") -> head.removePrefix("\"").substringBefore("\"")
            depth == 0 -> null
            else -> assignedValue(head, source)?.let { literalFor(it, source, depth - 1) }
        }
    }

    /** The right-hand side of `val [name] = …` in [source]. */
    private fun assignedValue(
        name: String,
        source: String,
    ): String? = Regex("""val\s+${Regex.escape(name)}\s*(?::[^=]+)?=\s*(.+)""").find(source)?.groupValues?.get(1)

    private companion object {
        const val MAX_DEPTH = 2
        val LOOKUP = Regex("""\.java\s*\.getResourceAsStream\(\s*([^)]+)\)""", RegexOption.DOT_MATCHES_ALL)
    }
}
