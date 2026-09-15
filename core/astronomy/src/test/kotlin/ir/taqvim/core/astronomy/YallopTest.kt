/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.testing.GoldenFile
import java.time.LocalDate
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import org.junit.jupiter.api.Test

/** Columns of `golden/yallop/yallop-1997-table4.csv`. */
private const val NUMBER = 0
private const val DATE = 1
private const val PHASE = 2
private const val LATITUDE = 3
private const val LONGITUDE = 4
private const val OBSERVATION = 5

/** One Table 4 observation with Schaefer's coded outcome split into the unaided eye and optical aid. */
private data class YallopRecord(
    val record: CrescentRecord,
    val nakedEye: Sighting,
    val opticalAid: Sighting,
)

private fun codedOutcome(code: String): Pair<Sighting, Sighting> {
    val eye = if (code.first() == 'V') Sighting.SEEN else Sighting.NOT_SEEN
    val aid =
        when (code.getOrNull(2)) {
            null -> Sighting.NOT_TRIED
            'I' -> Sighting.NOT_SEEN
            else -> Sighting.SEEN // (F) found with aid, (B) binoculars, (T) telescope, (V) either
        }
    return eye to aid
}

/**
 * A-06 against the 295 observations of Yallop's Table 4 (golden/yallop, observation facts only) and real evenings and
 * mornings around a new moon. The app computes each observation's q; none of the note's computed columns is used.
 * The limits come from the rates measured on 2026-09-15 (in the comments) with a margin of about 7 percentage points,
 * or one or two records where none or a few were measured.
 */
class YallopTest {
    @Test
    fun `Table 4 holds the facts of its 295 observations`() {
        records shouldHaveSize 295
        records.map { it.record.number }.toSet() shouldHaveSize 295
        records.count { it.record.morning } shouldBe 24
    }

    @Test
    fun `equation 3·6 and the class limits`() {
        Yallop.q(11.8371, 0.0) shouldBe (0.0 plusOrMinus 1e-12)
        Yallop.q(12.0, 1.0) shouldBe ((12.0 - (11.8371 - 6.3226 + 0.7319 - 0.1018)) / 10 plusOrMinus 1e-12)
        Yallop.classify(0.217) shouldBe CrescentVisibilityClass.A
        Yallop.classify(0.216) shouldBe CrescentVisibilityClass.B
        Yallop.classify(-0.014) shouldBe CrescentVisibilityClass.C
        Yallop.classify(-0.160) shouldBe CrescentVisibilityClass.D
        Yallop.classify(-0.232) shouldBe CrescentVisibilityClass.E
        Yallop.classify(-0.293) shouldBe CrescentVisibilityClass.F
        Yallop.classify(-1.0) shouldBe CrescentVisibilityClass.F
    }

    @Test
    fun `the crescent width follows the parallax, altitude and arc of light`() {
        Yallop.topocentricWidthArcMinutes(60.0, 0.0, 90.0) shouldBe (0.27245 * 60.0 plusOrMinus 1e-9)
        Yallop.topocentricWidthArcMinutes(60.0, 0.0, 0.0) shouldBe (0.0 plusOrMinus 1e-12)
        Yallop.topocentricWidthArcMinutes(60.0, 10.0, 12.0) shouldBeGreaterThan
            Yallop.topocentricWidthArcMinutes(60.0, 0.0, 12.0)
    }

    @Test
    fun `naked-eye outcomes of Table 4 follow the computed classes`() {
        val tally = SightingTally(computed.map { (record, observation) -> observation.visibility to record.nakedEye })
        val invisible = listOf(CrescentVisibilityClass.D, CrescentVisibilityClass.E, CrescentVisibilityClass.F)

        withClue(tally) {
            tally.successShare(CrescentVisibilityClass.A) shouldBeGreaterThanOrEqual 0.85 // measured 154 of 166
            tally.successShare(CrescentVisibilityClass.B) shouldBeGreaterThanOrEqual 0.63 // measured 48 of 68
            tally.successShare(CrescentVisibilityClass.C) shouldBeLessThanOrEqual 0.19 // measured 3 of 26
            invisible.sumOf { tally.count(it, Sighting.SEEN) } shouldBeLessThanOrEqual 4 // measured 2 of 35
            tally.count(CrescentVisibilityClass.F, Sighting.SEEN) shouldBeLessThanOrEqual 1 // measured 0 of 17
        }
    }

    @Test
    fun `optical aid never missed a crescent of classes A and B`() {
        val tally = SightingTally(computed.map { (record, observation) -> observation.visibility to record.opticalAid })

        withClue(tally) {
            // measured: 45 aided attempts in A and B, all seen
            val easy = listOf(CrescentVisibilityClass.A, CrescentVisibilityClass.B)
            easy.sumOf { tally.count(it, Sighting.NOT_SEEN) } shouldBeLessThanOrEqual 1
        }
    }

    @Test
    fun `the crescent is invisible on the evening of a new moon and easy two evenings later`() {
        val sameEvening = Yallop.evening(TEHRAN, newMoon - 12.hours)
        (sameEvening == null || sameEvening.visibility == CrescentVisibilityClass.F) shouldBe true
        val later = Yallop.evening(TEHRAN, newMoon + 2.days).shouldNotBeNull()
        later.visibility shouldBeLessThanOrEqualTo CrescentVisibilityClass.B
        later.lagMinutes shouldBeGreaterThan 60.0
        later.arcLightDegrees shouldBeGreaterThan 15.0
    }

    @Test
    fun `the old crescent is invisible on the morning of a new moon and easy two mornings earlier`() {
        val sameMorning = Yallop.morning(TEHRAN, newMoon - 12.hours)
        (sameMorning == null || sameMorning.visibility == CrescentVisibilityClass.F) shouldBe true
        val earlier = Yallop.morning(TEHRAN, newMoon - 3.days).shouldNotBeNull()
        earlier.bestTime shouldBeLessThanOrEqualTo newMoon - 1.days
        earlier.visibility shouldBeLessThanOrEqualTo CrescentVisibilityClass.B
        earlier.lagMinutes shouldBeGreaterThan 60.0
    }

    @Test
    fun `no evening without a sunset and no morning without a sunrise`() {
        Yallop.evening(Coordinates(80.0, 15.0), Instant.parse("2026-06-21T00:00:00Z")).shouldBeNull()
        Yallop.morning(Coordinates(80.0, 15.0), Instant.parse("2026-06-21T00:00:00Z")).shouldBeNull()
    }

    private companion object {
        val TEHRAN = Coordinates(35.70, 51.42)

        val newMoon: Instant =
            Sky
                .moonQuarters(Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-03-01T00:00:00Z"))
                .first { it.quarter == MoonQuarter.NEW_MOON }
                .instant

        val records: List<YallopRecord> =
            GoldenFile
                .load("golden/yallop/yallop-1997-table4.csv")
                .lines
                .drop(1)
                .map { it.split(',') }
                .map { row ->
                    val place = Coordinates(row[LATITUDE].toDouble(), row[LONGITUDE].toDouble())
                    val (eye, aid) = codedOutcome(row[OBSERVATION])
                    YallopRecord(
                        CrescentRecord(row[NUMBER].toInt(), place, LocalDate.parse(row[DATE]), row[PHASE] == "M"),
                        eye,
                        aid,
                    )
                }

        /** Every observation with the crescent the app computes for it; an observation without one fails here. */
        val computed: List<Pair<YallopRecord, CrescentObservation>> by lazy {
            records.map { record ->
                val observation = record.record.yallop()
                record to requireNotNull(observation) { "observation ${record.record.number} has no crescent" }
            }
        }
    }
}
