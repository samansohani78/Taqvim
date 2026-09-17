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
 * Android compiles regular expressions with ICU, which rejects a bare `}` after an escaped `\{` that the JVM accepts.
 * `LanguageSpec`'s placeholder pattern crashed the app on its first start on a device while every JVM test passed
 * (T-1800 baseline profile run, 2026-09-17), so app code must escape both braces of a literal brace pair.
 */
class AndroidRegexKonsistTest {
    @Test
    fun `app regex literals escape both braces of a literal brace pair`() {
        Konsist
            .scopeFromProduction()
            .files
            .filterNot { it.path.contains("/build-logic/") }
            .flatMap { file -> unbalancedLiterals(file.text).map { "${file.path}: $it" } }
            .shouldBeEmpty()
    }

    @Test
    fun `the check flags a bare closing brace and accepts escaped pairs and quantifiers`() {
        unbalancedLiterals("val a = Regex(\"\"\"\\{[01]}\"\"\")") shouldBe listOf("\\{[01]}")
        unbalancedLiterals("val b = Regex(\"\"\"\\{[01]\\}\"\"\")").shouldBeEmpty()
        unbalancedLiterals("val c = Regex(\"\"\"\\d{1,4}\"\"\")").shouldBeEmpty()
    }

    private fun unbalancedLiterals(source: String): List<String> =
        LITERAL
            .findAll(source)
            .map { it.groupValues[1] }
            .filter { pattern -> ESCAPED_OPEN.findAll(pattern).count() != ESCAPED_CLOSE.findAll(pattern).count() }
            .toList()

    private companion object {
        val LITERAL = Regex("\"\"\"(.*?)\"\"\"\\)", RegexOption.DOT_MATCHES_ALL)
        val ESCAPED_OPEN = Regex("""\\\{""")
        val ESCAPED_CLOSE = Regex("""\\\}""")
    }
}
