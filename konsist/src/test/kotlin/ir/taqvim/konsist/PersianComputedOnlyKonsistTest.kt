/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.konsist

import com.lemonappdev.konsist.api.Konsist
import io.kotest.matchers.collections.shouldBeEmpty
import org.junit.jupiter.api.Test

/**
 * Persian year starts come only from the equinox rule (ADR-0026 addendum, owner 2026-09-17): the Calendar Center's
 * official leap years, daily calendars and Nowruz instants are golden oracles in test sources, never runtime input.
 */
class PersianComputedOnlyKonsistTest {
    private val officialPersianData =
        listOf(
            "golden/persian",
            "official-leap-years",
            "official-nowruz-instants",
            "iran-official-14",
            "PersianLeapTable",
        )

    @Test
    fun `no production file reads official Persian calendar data`() {
        Konsist
            .scopeFromProduction()
            .files
            .filter { file -> officialPersianData.any { file.text.contains(it) } }
            .map { it.path }
            .shouldBeEmpty()
    }

    @Test
    fun `the Persian calendar has no override input`() {
        Konsist
            .scopeFromProduction()
            .files
            .filter { it.name == "PersianCalendarSystem" || it.name == "PersianYearStarts" }
            .filter { file -> Regex("""(?i)\boverride[A-Z]\w*|official""").containsMatchIn(file.text.stripKDoc()) }
            .map { it.path }
            .shouldBeEmpty()
    }

    private fun String.stripKDoc(): String = replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), "")
}
