/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import ir.taqvim.core.testing.SnapshotVerifier
import java.io.File
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

/**
 * The Islamic calibration refit (docs/adr/0040-islamic-calibration-refit.md): every region's shipped calendar and
 * every candidate the refit tries are scored against the official months available today, and the result is written
 * to `docs/data-todo/islamic-calibration-report.md`.
 *
 * The report is a snapshot: adding official calendars to `docs/sources` and re-running
 * `tools/sources/iran/official_calendar_import.py` changes the anchors, this test then fails with the line that changed, and
 * `./gradlew :core:astronomy:test -Ptaqvim.updateSnapshots=true` rewrites it for review.
 */
class IslamicCalibrationReportTest {
    private val regions = regions()
    private val scores = regions.associateWith { region -> score(region, region.shipped) }

    @TestFactory
    fun `every region reaches its documented agreement`(): List<DynamicTest> =
        regions.map { region ->
            DynamicTest.dynamicTest(region.name) {
                val agreement = scores.getValue(region)

                withClue({ "${agreement.agreed}/${agreement.total}; misses: ${agreement.misses}" }) {
                    agreement.share shouldBeGreaterThanOrEqual region.threshold
                }
            }
        }

    @Test
    fun `every region has anchors and a shipped calendar that beats the tabular calendars`() {
        regions.filter { it.anchors.isEmpty() }.shouldBeEmpty()
        regions
            .filterNot { region ->
                val shipped = scores.getValue(region).share
                region.refit.filter { "tabular" in it.name || "type" in it.name }.all {
                    score(region, it).share <= shipped
                }
            }.map { it.name }
            .shouldBeEmpty()
    }

    @Test
    fun `the report is up to date`() {
        val report = File(System.getProperty(REPORT_PROPERTY) ?: error("$REPORT_PROPERTY is not set"))

        SnapshotVerifier().verify(report, render(), GENERATOR).getOrThrow() shouldBe Unit
    }

    private fun render(): String =
        buildString {
            appendLine("# Islamic calendar calibration — agreement with the official months")
            appendLine()
            appendLine(
                "Produced by `IslamicCalibrationReportTest` in `:core:astronomy`. It scores the shipped calendar of " +
                    "each region, and every calendar the refit tries, against the official months stored in the " +
                    "repository today. Import new official calendars with " +
                    "`tools/sources/iran/official_calendar_import.py`, then rerun the test with " +
                    "`-Ptaqvim.updateSnapshots=true` to refresh this page.",
            )
            regions.forEach { region -> appendRegion(region) }
        }

    private fun StringBuilder.appendRegion(region: Region) {
        val shipped = scores.getValue(region)
        val ranked = (listOf(shipped) + region.refit.map { score(region, it) }).sortedWith(BEST_FIRST)

        appendLine()
        appendLine("## ${region.name}")
        appendLine()
        appendLine("${region.anchors.size} official facts, AH ${region.firstYear}–${region.lastYear}. ${region.note}")
        appendLine()
        appendLine("**Shipped: ${shipped.candidate.name} — ${shipped.percent} (${shipped.agreed}/${shipped.total}).**")
        if (shipped.misses.isNotEmpty()) {
            appendLine()
            appendLine("Missed: ${shipped.misses.joinToString(", ")}.")
        }
        appendLine()
        appendLine("| Calendar tried | Agreement | Missed |")
        appendLine("|---|---|---|")
        ranked.forEach { agreement ->
            val missed =
                agreement.misses
                    .take(MISSES_LISTED)
                    .joinToString(", ")
                    .ifEmpty { "—" }
            val more = if (agreement.misses.size > MISSES_LISTED) ", …" else ""
            appendLine("| ${agreement.candidate.name} | ${agreement.percent} | $missed$more |")
        }
        appendLine()
        appendLine(verdict(shipped, ranked))
    }

    private fun verdict(
        shipped: Agreement,
        ranked: List<Agreement>,
    ): String {
        val best = ranked.first()
        return when {
            best.shipped -> {
                "No candidate does better than the shipped calendar, so it stays as it is."
            }

            best.share > shipped.share -> {
                "**${best.candidate.name} fits better (${best.percent}).** Too few official months to change the " +
                    "shipped calendar on: revisit once more official calendars are imported (ADR-0040)."
            }

            else -> {
                "Candidates tie with the shipped calendar; it stays as it is."
            }
        }
    }

    private companion object {
        const val REPORT_PROPERTY = "taqvim.calibration.report"
        const val GENERATOR = "core/astronomy IslamicCalibrationReportTest"
        const val MISSES_LISTED = 6
        val BEST_FIRST =
            compareByDescending<Agreement> { it.share }
                .thenBy { !it.shipped }
                .thenBy { it.candidate.name }
    }
}
