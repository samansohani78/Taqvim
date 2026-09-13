/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.times

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.floats.shouldBeBetween
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import ir.taqvim.core.i18n.DurationFormatter
import ir.taqvim.core.praytimes.PrayerTimesResult
import kotlin.time.Duration.Companion.minutes
import org.junit.jupiter.api.Test

class TimesStateMapperTest {
    private val tehran = TimesFixtures.tehran()
    private val noon = TimesFixtures.at("2026-06-21T12:00", tehran)

    private fun day(
        settings: TimesSettings = tehran,
        offset: Long = 0,
        expanded: Boolean = false,
    ): TimesContent.Day {
        val state = TimesStateMapper.map(settings, offset, noon, expanded)
        state.expanded shouldBe expanded
        return state.content.shouldBeInstanceOf<TimesContent.Day>()
    }

    @Test
    fun `without a place there is nothing to calculate`() {
        TimesStateMapper.map(null, 0, noon, expanded = true) shouldBe TimesUiState(TimesContent.NoLocation, true)
    }

    @Test
    fun `today shows the primary times, the countdown and the sun's progress`() {
        val content = day()
        content.isToday shouldBe true
        content.placeName shouldBe "Tehran"
        content.dayTitle shouldContain "1405"
        content.rows.map { it.kind } shouldContainExactly PrayerKind.entries.filter { it.primary }
        content.rows.filter { it.isNext }.map { it.kind } shouldContainExactly listOf(PrayerKind.DHUHR)
        content.rows.all { it.time?.matches(Regex("""\d\d:\d\d""")) == true } shouldBe true
        val next = content.next.shouldNotBeNull()
        next.kind shouldBe PrayerKind.DHUHR
        next.remaining shouldContain "minute"
        content.sunPath
            .shouldNotBeNull()
            .progress
            .shouldNotBeNull()
            .shouldBeBetween(0f, 1f, 0f)
        content.unavailable.shouldBeNull()
    }

    @Test
    fun `expanded lists every time`() {
        day(expanded = true).rows.map { it.kind } shouldContainExactly PrayerKind.entries
    }

    @Test
    fun `other days have no countdown and no sun position`() {
        val tomorrow = day(offset = 1)
        tomorrow.isToday shouldBe false
        tomorrow.next.shouldBeNull()
        tomorrow.rows.none { it.isNext } shouldBe true
        tomorrow.sunPath
            .shouldNotBeNull()
            .progress
            .shouldBeNull()
        tomorrow.dayTitle shouldBe day(offset = 1).dayTitle
        (tomorrow.dayTitle == day().dayTitle) shouldBe false
    }

    @Test
    fun `after the last time of today there is no sun position and no countdown on today's list`() {
        val evening = TimesFixtures.at("2026-06-21T23:30", tehran)
        val content = TimesStateMapper.map(tehran, 0, evening, false).content
        content
            .shouldBeInstanceOf<TimesContent.Day>()
            .sunPath
            .shouldNotBeNull()
            .progress
            .shouldBeNull()
        content.next.shouldBeNull()
        content.rows.none { it.isNext } shouldBe true
    }

    @Test
    fun `a polar day explains why there are no times`() {
        val content = TimesStateMapper.map(TimesFixtures.tromso(), 0, noon, false).content
        content.shouldBeInstanceOf<TimesContent.Day>().unavailable shouldBe PrayerTimesResult.Reason.POLAR_DAY
        content.rows.shouldBeEmpty()
        content.next.shouldBeNull()
        content.sunPath.shouldBeNull()
    }

    @Test
    fun `languages without duration patterns get a clock-style countdown in their digits`() {
        val persian = day(TimesFixtures.tehran("fa")).next.shouldNotBeNull()
        persian.remaining.any { it in '۰'..'۹' } shouldBe true
        val tajik = TimesFixtures.tehran("tg")
        if (DurationFormatter.format(5.minutes, tajik.language) == null) {
            day(tajik).next.shouldNotBeNull().remaining shouldContain ":"
        } else {
            day(tajik)
                .next
                .shouldNotBeNull()
                .remaining
                .isNotBlank() shouldBe true
        }
    }
}
