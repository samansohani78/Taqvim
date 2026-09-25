/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.testing.GoldenFile
import java.time.LocalDate
import org.junit.jupiter.api.Test

/** Columns of `golden/hmnao/moonsighting-uk-visibility-maps.csv`. */
private const val ID = 0
private const val DATE = 1
private const val PLACE = 2
private const val LATITUDE = 3
private const val LONGITUDE = 4
private const val EXPECTED_CLASS = 5

/**
 * DT-034: the app's Yallop classifier (A-06) against 44 (date, place) readings taken, by pixel colour, off five of
 * HMNAO's own published New Crescent Moon Visibility maps (`golden/hmnao`, republished by Moon Sighting UK/ICOUK).
 * Every reading was kept only where one legend class held at least 95% of a 21x21 px block around the place, well
 * inside a single-coloured band (the golden's header records the method and its limits in full) — this is weaker
 * evidence than a published table of numeric values (`YallopTest`, `OdehTable6Test`), since a map read by pixel
 * carries its own colour- and position-uncertainty that a printed number does not, but every kept point sits far
 * enough from a class boundary that this uncertainty cannot plausibly change its class.
 */
class HmnaoVisibilityMapTest {
    @Test
    fun `Yallop's classifier agrees with HMNAO's published visibility maps`() {
        val rows =
            GoldenFile
                .load("golden/hmnao/moonsighting-uk-visibility-maps.csv")
                .lines
                .drop(1)
                .map { it.split(',') }
        rows.shouldNotBeEmpty()
        rows.forEach { columns ->
            val place = Coordinates(columns[LATITUDE].toDouble(), columns[LONGITUDE].toDouble())
            val record = CrescentRecord(0, place, LocalDate.parse(columns[DATE]), morning = false)
            val observed = record.yallop()
            withClue("${columns[ID]}: ${columns[PLACE]} on ${columns[DATE]}") {
                if (columns[EXPECTED_CLASS] == "NULL") {
                    observed.shouldBeNull()
                } else {
                    observed.shouldNotBeNull()
                    observed.visibility shouldBe CrescentVisibilityClass.valueOf(columns[EXPECTED_CLASS])
                }
            }
        }
    }
}
