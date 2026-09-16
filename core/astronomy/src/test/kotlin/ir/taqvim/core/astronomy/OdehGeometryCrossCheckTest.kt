/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.doubles.shouldBeLessThan
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.testing.GoldenFile
import java.time.LocalDate
import kotlin.math.abs
import org.junit.jupiter.api.Test

/**
 * I08: the app's crescent geometry (cosinekitty ephemeris) against an independent textbook computation
 * ([MeeusCrescentGeometry]) at the same best time, for every Odeh (2004) Table VI observation. Unlike the outcome
 * rates in [OdehTable6Test], these bounds do not come from the app's own output: they are the stated accuracy of the
 * truncated Meeus series (about 10″ for the Moon, 0.01° for the Sun) plus the ΔT model difference, about 0.03° in
 * all. Measured on 2026-09-17: ARCV and ARCL within 0.015° (mean 0.004°), W within 0.001′, the same zone for 576 of
 * 578 records (the two others lie on a zone boundary).
 */
class OdehGeometryCrossCheckTest {
    @Test
    fun `arc of vision, arc of light and width agree with the independent computation`() {
        val differences =
            observations.map { (record, app) ->
                val independent = MeeusCrescentGeometry.at(record.place, app.bestTime)
                Triple(
                    abs(app.arcVisionDegrees - independent.arcVisionDegrees),
                    abs(app.arcLightDegrees - independent.arcLightDegrees),
                    abs(app.widthArcMinutes - independent.widthArcMinutes),
                )
            }
        differences.maxOf { it.first } shouldBeLessThan MAX_ARCV_DEGREES
        differences.maxOf { it.second } shouldBeLessThan MAX_ARCL_DEGREES
        differences.maxOf { it.third } shouldBeLessThan MAX_WIDTH_ARC_MINUTES
    }

    @Test
    fun `the independent geometry puts almost every record in the app's Odeh zone`() {
        val agreeing =
            observations.count { (record, app) ->
                val independent = MeeusCrescentGeometry.at(record.place, app.bestTime)
                Odeh.classify(Odeh.v(independent.arcVisionDegrees, independent.widthArcMinutes)) == app.zone
            }

        agreeing.toDouble() / observations.size shouldBeGreaterThanOrEqual MIN_ZONE_AGREEMENT
    }

    private companion object {
        const val MAX_ARCV_DEGREES = 0.03
        const val MAX_ARCL_DEGREES = 0.03

        /** 0.03° of arc of light changes the width of a thin crescent by well under 0.005′. */
        const val MAX_WIDTH_ARC_MINUTES = 0.005

        /** Records within the geometry tolerance of a zone boundary may fall on either side. */
        const val MIN_ZONE_AGREEMENT = 0.99

        /** Every Table VI record with the crescent the app computes for it. */
        val observations: List<Pair<CrescentRecord, OdehObservation>> by lazy {
            GoldenFile
                .load("golden/odeh/odeh-2004-table6.csv")
                .lines
                .drop(1)
                .map { it.split(',') }
                .mapNotNull { row ->
                    val place = Coordinates(row[4].toDouble(), row[5].toDouble(), row[6].toDouble())
                    val record = CrescentRecord(row[0].toInt(), place, LocalDate.parse(row[3]), row[2] == "M")
                    record.odeh()?.let { record to it }
                }
        }
    }
}
