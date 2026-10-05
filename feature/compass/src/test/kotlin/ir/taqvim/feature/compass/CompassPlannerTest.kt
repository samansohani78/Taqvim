/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.compass

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.toJdn
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

/** T-1302 planner: the moment the Sun and Moon are drawn for, named in the user's own calendar. */
class CompassPlannerTest {
    private val today = CompassFixtures.NOON.toJdn(TimeZone.UTC)

    @Test
    fun `live mode offers a picker but names no moment`() {
        val state = CompassStateMapper.planner(CompassFixtures.settings(), today, null, 720, false)

        state.planned.shouldBeNull()
        state.pickerOpen shouldBe false
        // The picker is still built, so opening it starts on today rather than on nothing.
        state.picker.shouldNotBeNull()
    }

    @Test
    fun `the picker and the planned date speak the user's calendar, not Gregorian`() {
        // A Persian user picking a date must get their own months. Building the picker from the Gregorian calendar
        // would silently offer January to someone whose calendar starts at Farvardin.
        val persian = CompassFixtures.settings(languageCode = "fa", calendar = PersianCalendarSystem)
        val nowruz = PersianCalendarSystem.toJdn(PersianCalendarSystem.date(1405, 1, 1))

        val state = CompassStateMapper.planner(persian, today, nowruz, 18 * 60 + 30, false)

        val planned = state.planned.shouldNotBeNull()
        planned.dateText shouldContain "۱۴۰۵"
        planned.timeText shouldBe "۱۸:۳۰"
        planned.minuteOfDay shouldBe 18 * 60 + 30
        state.picker
            .shouldNotBeNull()
            .initial.year shouldBe 1405
    }

    @Test
    fun `without settings there is nothing to plan with`() {
        val state = CompassStateMapper.planner(null, today, today, 720, true)

        state.planned.shouldBeNull()
        state.picker.shouldBeNull()
        state.pickerOpen shouldBe false
    }
}
