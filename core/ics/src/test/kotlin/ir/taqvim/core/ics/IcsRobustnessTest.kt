/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ics

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import ir.taqvim.core.testing.GoldenFile
import kotlin.random.Random
import org.junit.jupiter.api.Test

/** Malformed input yields errors with line numbers, never exceptions; fuzzed input never crashes the reader. */
class IcsRobustnessTest {
    private fun failure(text: String): IcsParseResult.Failure {
        val result = IcsReader.read(text)
        result.shouldBeInstanceOf<IcsParseResult.Failure>()
        return result
    }

    @Test
    fun `structural errors are reported with their lines`() {
        failure("") shouldBe IcsParseResult.Failure(listOf(IcsProblem(1, "no VCALENDAR component")))
        failure("BEGIN:VCALENDAR\nthis is not a content line\nEND:VCALENDAR").errors.single().line shouldBe 2
        failure("BEGIN:VCALENDAR\nBEGIN:VEVENT\nEND:VCALENDAR").errors.map { it.line } shouldBe listOf(3, 1)
        failure("VERSION:2.0\nBEGIN:VCALENDAR\nEND:VCALENDAR").errors.single().message shouldContain "outside"
        failure("END:VCALENDAR").errors.single().message shouldContain "any component"
        failure("BEGIN:VTODO\nEND:VTODO").errors.single().message shouldContain "not a VCALENDAR"
    }

    @Test
    fun `several calendar objects in one stream are merged`() {
        val text =
            "BEGIN:VCALENDAR\nPRODID:first\nBEGIN:VEVENT\nUID:a\n" +
                "DTSTART;VALUE=DATE:20260101\nEND:VEVENT\nEND:VCALENDAR\n" +
                "BEGIN:VCALENDAR\nBEGIN:VEVENT\nUID:b\nDTSTART;VALUE=DATE:20260102\nEND:VEVENT\nEND:VCALENDAR\n"
        val result = IcsReader.read(text)

        result.shouldBeInstanceOf<IcsParseResult.Success>()
        result.calendar.productId shouldBe "first"
        result.calendar.events.map { it.uid } shouldBe listOf("a", "b")
        result.warnings.shouldBeEmpty()
    }

    @Test
    fun `alarms without action or trigger are skipped with warnings`() {
        val text =
            "BEGIN:VCALENDAR\nBEGIN:VEVENT\nUID:a\nDTSTART:20260101T000000Z\n" +
                "BEGIN:VALARM\nTRIGGER:-PT5M\nEND:VALARM\n" +
                "BEGIN:VALARM\nACTION:DISPLAY\nTRIGGER:soon\nEND:VALARM\n" +
                "BEGIN:VALARM\nACTION:DISPLAY\nTRIGGER;VALUE=DATE-TIME:20260101T000000\nEND:VALARM\n" +
                "END:VEVENT\nEND:VCALENDAR\n"
        val result = IcsReader.read(text)

        result.shouldBeInstanceOf<IcsParseResult.Success>()
        result.calendar.events
            .single()
            .alarms
            .shouldBeEmpty()
        result.warnings.map { it.message } shouldBe
            listOf(
                "VALARM with ACTION missing ignored",
                "VALARM without a valid TRIGGER ignored",
                "VALARM without a valid TRIGGER ignored",
            )
    }

    @Test
    fun `ten thousand fuzzed documents never crash the reader`() {
        val random = Random(SEED)
        val seeds = FIXTURES.map { GoldenFile.load("golden/ics/$it.ics").body }
        val alphabet = "BEGIN:VEVENTDSTART;=,\"\\\r\n\t 0123456789TZ-+PWDHMS"

        repeat(DOCUMENTS) { index ->
            val text =
                if (index % 2 == 0) {
                    mutate(seeds.random(random), random, alphabet)
                } else {
                    String(CharArray(random.nextInt(0, 400)) { alphabet[random.nextInt(alphabet.length)] })
                }
            IcsReader.read(text)
        }
    }

    private fun mutate(
        text: String,
        random: Random,
        alphabet: String,
    ): String =
        (1..random.nextInt(1, MAX_MUTATIONS)).fold(text) { current, _ ->
            val at = random.nextInt(0, current.length + 1)
            val insert = alphabet[random.nextInt(alphabet.length)].toString()
            when (random.nextInt(3)) {
                0 -> current.take(at) + insert + current.drop(at + 1)
                1 -> current.take(at) + insert + current.drop(at)
                else -> current.take(at) + current.drop(at + 1)
            }
        }

    private companion object {
        const val SEED = 5545
        const val DOCUMENTS = 10_000
        const val MAX_MUTATIONS = 12
        val FIXTURES =
            listOf(
                "01-google-timed-event",
                "05-google-display-alarm",
                "09-folded-description",
                "30-unsupported-and-invalid-values",
            )
    }
}
