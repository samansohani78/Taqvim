/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.longs.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import ir.taqvim.core.testing.GoldenFile
import kotlin.math.abs
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import org.junit.jupiter.api.Test

/** Columns of golden/panchang/tithi-1945se.csv. */
private const val PAKSHA = 1
private const val NUMBER = 2
private const val ENDS_UTC = 5

private const val TITHIS_PER_MONTH = 30
private const val SECONDS_PER_MINUTE = 60L

/**
 * How far a computed tithi boundary may sit from the Panchang's printed one. The Panchang prints whole minutes and
 * [Tithi.changes] resolves to 30 seconds, so a minute is rounding; the measured worst case over these 376 boundaries
 * is well inside this, and the test prints the distribution so a drift shows up as a number rather than a pass.
 */
private const val TOLERANCE_MINUTES = 3L

/**
 * A-15/T-406 (DT-014): the app's tithi boundaries against the Rashtriya Panchang, the panchang the Government of
 * India publishes through the Positional Astronomy Centre of the India Meteorological Department.
 *
 * The comparison is only fair because both sides are the same kind of quantity. The Panchang is computed "on modern
 * scientific principles" from the Indian Astronomical Ephemeris, and [Tithi] likewise takes true geocentric
 * longitudes from [Sky]; the Panchang states its tithi moments are geocentric and therefore "require no correction
 * for other places", so no observer enters either side. This is deliberately **not** a check of
 * `NepaliLunarDays`, which computes a Surya Siddhanta Moon on purpose because Nepal's panchang does (ADR-0030), and
 * whose boundaries differ from a modern ephemeris by hours.
 *
 * A printed entry is the *ending* moment of the tithi it names, which is the instant the next tithi begins — so
 * tithi 30 ends where tithi 1 starts.
 */
class RashtriyaPanchangTithiTest {
    private data class Reference(
        val endsAt: Instant,
        val ending: Int,
        val beginning: Int,
    )

    private val references: List<Reference> =
        GoldenFile
            .load("golden/panchang/tithi-1945se.csv")
            .lines
            .drop(1)
            .map { line ->
                val columns = line.split(",")
                val ending = columns[NUMBER].toInt()
                Reference(
                    endsAt = Instant.parse(columns[ENDS_UTC].replace("Z", ":00Z")),
                    ending = ending,
                    beginning = ending % TITHIS_PER_MONTH + 1,
                )
            }

    private val changes: List<Instant> by lazy {
        Tithi.changes(references.first().endsAt - 2.days, references.last().endsAt + 2.days)
    }

    @Test
    fun `the golden covers a full panchang year`() {
        references shouldHaveSize 376
        references.first().ending shouldBe 1
        // Every printed paksha agrees with the number it prints: Sukla is 1‥15, Krishna 16‥30.
        GoldenFile
            .load("golden/panchang/tithi-1945se.csv")
            .lines
            .drop(1)
            .forEach { line ->
                val columns = line.split(",")
                val number = columns[NUMBER].toInt()
                val sukla = columns[PAKSHA] == "Sukla"
                withClue(line) { (number <= TITHIS_PER_MONTH / 2) shouldBe sukla }
            }
    }

    @Test
    fun `every printed tithi ending matches a computed boundary`() {
        val deviations =
            references.map { reference ->
                val nearest =
                    changes.minByOrNull { abs((it - reference.endsAt).inWholeSeconds) }
                        ?: error("no computed boundary near ${reference.endsAt}")
                val seconds = (nearest - reference.endsAt).inWholeSeconds
                withClue("tithi ${reference.ending} ending ${reference.endsAt}, computed $nearest") {
                    abs(seconds) shouldBeLessThanOrEqual TOLERANCE_MINUTES * SECONDS_PER_MINUTE
                    // The boundary must be the one this tithi ends at, not a neighbouring one.
                    Tithi.at(nearest + 1.minutes).number shouldBe reference.beginning
                }
                seconds
            }
        val sorted = deviations.sorted()
        println(
            "Rashtriya Panchang 1945 SE: ${deviations.size} tithi boundaries, computed minus printed " +
                "min ${sorted.first()}s, median ${sorted[deviations.size / 2]}s, max ${sorted.last()}s",
        )
        // The offset is one-sided because the Panchang prints the minute the moment falls in, so a printed time is
        // up to 60 s early by construction; the computed boundary is never *earlier* than the printed minute.
        withClue("printed minutes should never be later than the computed boundary") {
            sorted.first() shouldBeGreaterThanOrEqual -SECONDS_PER_MINUTE
        }
        // The whole year must agree, not merely most of it.
        deviations shouldHaveSize 376
    }
}
