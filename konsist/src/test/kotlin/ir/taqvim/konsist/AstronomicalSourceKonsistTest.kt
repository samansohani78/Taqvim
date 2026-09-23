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
 * `OccurrenceCalculator` and `EventLookup` default `astronomy` to `null`, and an `Astronomical` rule then fails at run
 * time with `checkNotNull`. Until main@e92cdff every production site relied on that default, so the first astronomical
 * record — Vesak — would have thrown in the app while every test passed, because the dataset had none to evaluate. The
 * default stays for the 44 test sites that never evaluate such a rule; production code must pass a real source — named or positional —
 * which this check enforces at build time instead of leaving it to be discovered on a device.
 */
class AstronomicalSourceKonsistTest {
    @Test
    fun `production event calculators are given an astronomical source`() {
        Konsist
            .scopeFromProduction()
            .files
            .filterNot { it.path.contains("/build-logic/") }
            .flatMap { file -> callsWithoutAstronomy(file.text).map { "${file.path}: $it" } }
            .shouldBeEmpty()
    }

    @Test
    fun `the check flags a call without the source and accepts one with it`() {
        callsWithoutAstronomy("val c = OccurrenceCalculator(rules)") shouldBe listOf("OccurrenceCalculator(rules)")
        callsWithoutAstronomy("val l = EventLookup(sources, calendars)") shouldBe
            listOf("EventLookup(sources, calendars)")
        callsWithoutAstronomy("val c = OccurrenceCalculator(rules, astronomy = sky)").shouldBeEmpty()
        // The Wear screen passes the source positionally rather than by name.
        callsWithoutAstronomy("val l = EventLookup(definitions, selection, SkyAstronomicalEventSource)").shouldBeEmpty()
        val multiline =
            """
            val lookup =
                EventLookup(
                    sources = sources,
                    astronomy = SkyAstronomicalEventSource(),
                )
            """
        callsWithoutAstronomy(multiline).shouldBeEmpty()
        callsWithoutAstronomy("class OccurrenceCalculator(private val astronomy: X? = null)").shouldBeEmpty()
    }

    /** The `OccurrenceCalculator(` / `EventLookup(` calls in [source] whose argument list never names `astronomy`. */
    private fun callsWithoutAstronomy(source: String): List<String> =
        CALL
            .findAll(source)
            .map { it.value }
            .filterNot { it.contains("astronomy") || it.contains("AstronomicalEventSource") }
            .filterNot { it.startsWith("class ") }
            .map { it.trim() }
            .toList()

    private companion object {
        /** A constructor call and its balanced-enough argument list: up to one level of nested parentheses. */
        val CALL =
            Regex("""\b(?:OccurrenceCalculator|EventLookup)\((?:[^()]|\([^()]*\))*\)""", RegexOption.DOT_MATCHES_ALL)
    }
}
