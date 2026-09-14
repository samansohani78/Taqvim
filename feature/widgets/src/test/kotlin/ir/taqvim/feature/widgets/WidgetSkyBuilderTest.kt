/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.astronomy.MoonQuarter
import ir.taqvim.core.astronomy.Sky
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.ui.painter.NormalizedPoint
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import org.junit.jupiter.api.Test

/** T-1210 Moon and T-1211 map content. */
class WidgetSkyBuilderTest {
    private val now = Instant.parse("2026-09-14T12:00:00Z")
    private val tehranPlace = Coordinates(35.69, 51.42)
    private val en = requireNotNull(LanguageTable.forCode("en"))
    private val fa = requireNotNull(LanguageTable.forCode("fa"))

    @Test
    fun `the Moon is dated in the primary calendar and mirrored south of the equator`() {
        val north = WidgetSkyBuilder.moon(now, tehranPlace, PersianCalendarSystem, WidgetSamples.tehran, en)
        val appearance = Sky.moonAppearance(now, tehranPlace)
        val quarters = Sky.moonQuarters(now, now + 32.days)

        fun dated(quarter: MoonQuarter) =
            WidgetContentBuilder.dayTitle(
                PersianCalendarSystem,
                quarters.first { it.quarter == quarter }.instant.toJdn(WidgetSamples.tehran),
                en,
            )
        north.illuminatedFraction shouldBe appearance.illuminatedFraction.toFloat()
        north.waxing shouldBe appearance.waxing
        north.illumination shouldBe (appearance.illuminatedFraction * 100).roundToInt().toString()
        north.nextFullMoon shouldBe dated(MoonQuarter.FULL_MOON)
        north.nextNewMoon shouldBe dated(MoonQuarter.NEW_MOON)
        north.rotationDegrees shouldBe 0f

        val sydney = Coordinates(-33.87, 151.21)
        WidgetSkyBuilder.moon(now, sydney, PersianCalendarSystem, WidgetSamples.tehran, en).rotationDegrees shouldBe
            180f
        val persian = WidgetSkyBuilder.moon(now, null, PersianCalendarSystem, WidgetSamples.tehran, fa)
        persian.rotationDegrees shouldBe 0f
        persian.illumination.all { it in '۰'..'۹' }.shouldBeTrue()
    }

    @Test
    fun `the map keeps rings of two or more points and reads darkness row by row`() {
        val map =
            WidgetSkyBuilder.map(
                land = listOf(floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f), floatArrayOf(0.5f, 0.5f)),
                columns = 3,
                rows = 2,
                darkness = { column, row -> (row * 3 + column) / 10f },
                marker = NormalizedPoint(0.5f, 0.25f),
            )

        map.land shouldBe listOf(listOf(NormalizedPoint(0.1f, 0.2f), NormalizedPoint(0.3f, 0.4f)))
        map.shade shouldBe
            WidgetMapShade(3, 2, kotlinx.collections.immutable.persistentListOf(0f, 0.1f, 0.2f, 0.3f, 0.4f, 0.5f))
        map.marker shouldBe NormalizedPoint(0.5f, 0.25f)
        WidgetSkyBuilder.map(emptyList(), 0, 0, { _, _ -> 1f }, null).shade.shouldBeNull()
    }
}
