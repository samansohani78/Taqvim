/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.about

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test

/** F-16: a problem report carries the app, device and language facts and redacted diagnostics only. */
class ProblemReportTest {
    private fun compose(
        entries: List<DiagnosticEntry>,
        info: AboutInfo = AboutFixtures.info,
    ): ProblemReport = ProblemReportComposer.compose(info, AboutFixtures.device, entries, AboutFixtures.texts)

    @Test
    fun `report lists facts and redacted diagnostics for the support address`() {
        val report = compose(AboutFixtures.entries)

        report.recipient shouldBe AboutFixtures.SUPPORT
        report.subject shouldBe "Report"
        report.body shouldContain "App: 1.0.0 (42, debug)"
        report.body shouldContain "Device: Google Pixel 8"
        report.body shouldContain "Android: 16 (API 36)"
        report.body shouldContain "Language: en"
        report.body shouldContain "Diagnostics (4):"
        report.body shouldContain
            "2026-09-10T00:26:40Z ERROR Subscriptions: " + "Fetch https://cal.example.org/[redacted] failed"
        report.body shouldContain "35.68,51.38"
        AboutFixtures.personalData.forEach { report.body shouldNotContain it }
    }

    @Test
    fun `report keeps the newest entries up to its cap`() {
        val many = (0 until 250).map { DiagnosticEntry(it.toLong(), DiagnosticLevel.INFO, "Tag", "entry $it") }
        val lines = compose(many).body.lines()

        lines.size shouldBe 4 + 1 + 1 + ProblemReportComposer.MAX_ENTRIES
        lines.last() shouldContain "entry 199"
    }

    @Test
    fun `report without diagnostics or support address`() {
        val report = compose(emptyList(), AboutFixtures.info.copy(links = AboutLinks()))

        report.recipient.shouldBeNull()
        report.body.lines().last() shouldBe "No diagnostics"
    }
}
