/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.about

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test

/**
 * T-1504: a stored crash is redacted like every other diagnostic before it is shown or reported, and the report
 * carries the newest one.
 */
class CrashReportTest {
    private fun report(crash: CrashReport?) =
        ProblemReportComposer.compose(
            AboutFixtures.info,
            AboutFixtures.device,
            entries = emptyList(),
            texts = AboutFixtures.texts,
            crash = crash,
        )

    @Test
    fun `the shown crash keeps the facts of the run but no personal data`() {
        val content = crashContent(listOf(AboutFixtures.crash))

        content.rows shouldHaveSize 1
        val row = content.rows.single()
        row.time shouldBe "2026-09-10T00:28:20Z"
        row.text shouldContain "device=OnePlus PJZ110"
        row.text shouldContain "android=16 (API 37)"
        row.text shouldContain "route=Calendar"
        row.text shouldContain "IllegalStateException"
        AboutFixtures.personalData.forEach { row.text shouldNotContain it }
    }

    @Test
    fun `the report carries the newest crash, redacted`() {
        val body = report(AboutFixtures.crash).body

        body shouldContain "Crash (2026-09-10T00:28:20Z):"
        body shouldContain "IllegalStateException"
        AboutFixtures.personalData.forEach { body shouldNotContain it }
    }

    @Test
    fun `a report without a stored crash has no crash section`() {
        report(crash = null).body shouldNotContain "Crash ("
    }

    @Test
    fun `a very long crash is cut`() {
        // A letter the rest of the report never uses, so counting it measures the crash section alone.
        val long = AboutFixtures.crash.copy(text = "q".repeat(ProblemReportComposer.MAX_CRASH_CHARS * 2))

        val body = report(long).body

        body.count { it == 'q' } shouldBe ProblemReportComposer.MAX_CRASH_CHARS
    }

    @Test
    fun `no stored crash means no rows`() {
        crashContent(emptyList()).rows shouldBe emptyList()
    }
}
