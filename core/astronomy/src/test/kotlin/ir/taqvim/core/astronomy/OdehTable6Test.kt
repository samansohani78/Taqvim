/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.testing.GoldenFile
import java.time.LocalDate
import kotlin.time.Duration.Companion.days
import org.junit.jupiter.api.Test

/** Columns of golden/odeh/odeh-2004-table6.csv. */
private const val NUMBER = 0
private const val SOURCE = 1
private const val PHASE = 2
private const val DATE = 3
private const val LATITUDE = 4
private const val LONGITUDE = 5
private const val ELEVATION = 6
private const val NAKED_EYE = 7
private const val BINOCULARS = 8
private const val TELESCOPE = 9

/** One Table VI record: the observation facts and how the crescent fared with each means of looking. */
private data class OdehRecord(
    val record: CrescentRecord,
    val source: String,
    val nakedEye: Sighting,
    val binoculars: Sighting,
    val telescope: Sighting,
) {
    val opticalAid: Sighting
        get() = Sighting.best(listOf(binoculars, telescope))

    val anyMeans: Sighting
        get() = Sighting.best(listOf(nakedEye, binoculars, telescope))
}

/**
 * T-1301 Odeh's criterion against the 578 printed observations of Odeh (2004) Table VI (golden/odeh, observation facts
 * only). The app computes each record's best time, topocentric arc of vision and crescent width and applies equation 2;
 * none of the paper's computed columns is used. The limits come from the rates measured on 2026-09-15 (see the
 * companion) with a margin of about 7 percentage points, or 1–2 records where none or one was measured.
 */
class OdehTable6Test {
    @Test
    fun `the golden holds the facts of the 578 printed records`() {
        records shouldHaveSize PRINTED_RECORDS
        records.map { it.record.number }.toSet() shouldHaveSize PRINTED_RECORDS
        records.all { it.record.number in 1..PAPER_RECORDS } shouldBe true
        records.groupingBy { it.source }.eachCount() shouldBe
            mapOf("I" to 284, "A" to 199, "C" to 41, "F" to 33, "D" to 15, "B" to 6)
        records.count { it.record.morning } shouldBe MORNINGS
        records.single { it.record.number == 737 }.record.localDate shouldBe LocalDate.of(2005, 11, 2)
    }

    @Test
    fun `naked-eye outcomes follow the computed zones`() {
        val tally = SightingTally(computed.map { (record, observation) -> observation.zone to record.nakedEye })

        withClue(tally) {
            tally.successShare(OdehZone.A) shouldBeGreaterThanOrEqual 0.80 // measured 136 of 155
            tally.successShare(OdehZone.B) shouldBeGreaterThanOrEqual 0.44 // measured 116 of 226
            tally.successShare(OdehZone.C) shouldBeLessThanOrEqual 0.12 // measured 4 of 89
            tally.count(OdehZone.D, Sighting.SEEN) shouldBeLessThanOrEqual 1 // measured 0 of 41
            tally.successShare(OdehZone.A) shouldBeGreaterThan tally.successShare(OdehZone.B)
            tally.successShare(OdehZone.B) shouldBeGreaterThan tally.successShare(OdehZone.C)
        }
    }

    @Test
    fun `optical-aid outcomes follow the computed zones`() {
        val aided = SightingTally(computed.map { (record, observation) -> observation.zone to record.opticalAid })
        val anyMeans = SightingTally(computed.map { (record, observation) -> observation.zone to record.anyMeans })

        withClue("optical aid: $aided / any means: $anyMeans") {
            aided.successShare(OdehZone.A, OdehZone.B) shouldBeGreaterThanOrEqual 0.82 // measured 184 of 207
            aided.successShare(OdehZone.C) shouldBeGreaterThanOrEqual 0.32 // measured 34 of 86
            aided.successShare(OdehZone.D) shouldBeLessThanOrEqual 0.11 // measured 1 of 27
            anyMeans.count(OdehZone.D, Sighting.SEEN) shouldBeLessThanOrEqual 2 // measured 1 of 47
        }
    }

    @Test
    fun `Yallop's classes computed for the same records rank them alike`() {
        val tally =
            SightingTally(
                records.map { record ->
                    requireNotNull(record.record.yallop()) { "record ${record.record.number} has no crescent" }
                        .visibility to record.nakedEye
                },
            )

        withClue(tally) {
            tally.successShare(CrescentVisibilityClass.A) shouldBeGreaterThanOrEqual 0.81 // measured 140 of 159
            tally.successShare(CrescentVisibilityClass.C) shouldBeLessThanOrEqual 0.28 // measured 13 of 64
            val invisible = listOf(CrescentVisibilityClass.E, CrescentVisibilityClass.F)
            invisible.sumOf { tally.count(it, Sighting.SEEN) } shouldBeLessThanOrEqual 4 // measured 2 of 99
            val (a, b, c) = listOf(CrescentVisibilityClass.A, CrescentVisibilityClass.B, CrescentVisibilityClass.C)
            tally.successShare(a) shouldBeGreaterThan tally.successShare(b)
            tally.successShare(b) shouldBeGreaterThan tally.successShare(c)
        }
    }

    @Test
    fun `morning records are computed before sunrise`() {
        val mornings = computed.filter { (record, _) -> record.record.morning }

        mornings shouldHaveSize MORNINGS
        mornings.forEach { (record, observation) ->
            observation.bestTime shouldBeLessThanOrEqualTo record.record.startOfLocalDay + 1.days
            observation.lagMinutes shouldBeGreaterThan 0.0
        }
    }

    private companion object {
        const val PRINTED_RECORDS = 578
        const val PAPER_RECORDS = 737
        const val MORNINGS = 56

        val records: List<OdehRecord> =
            GoldenFile
                .load("golden/odeh/odeh-2004-table6.csv")
                .lines
                .drop(1)
                .map { it.split(',') }
                .map { row ->
                    val place =
                        Coordinates(row[LATITUDE].toDouble(), row[LONGITUDE].toDouble(), row[ELEVATION].toDouble())
                    OdehRecord(
                        CrescentRecord(row[NUMBER].toInt(), place, LocalDate.parse(row[DATE]), row[PHASE] == "M"),
                        row[SOURCE],
                        Sighting.ofMark(row[NAKED_EYE]),
                        Sighting.ofMark(row[BINOCULARS]),
                        Sighting.ofMark(row[TELESCOPE]),
                    )
                }

        /** Every record with the crescent the app computes for it; a record without one fails here. */
        val computed: List<Pair<OdehRecord, OdehObservation>> by lazy {
            records.map { record ->
                record to requireNotNull(record.record.odeh()) { "record ${record.record.number} has no crescent" }
            }
        }
    }
}
