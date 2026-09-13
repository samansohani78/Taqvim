/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.HijriOffset
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.data.preferences.UserPreferences
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

/** T-305 (U): stored preferences become event settings. */
class EventsSettingsTest {
    private val persian = UserPreferences.defaultsFor("fa")
    private val setAt = Instant.parse("2026-09-01T00:00:00Z")

    @Test
    fun `defaults show every source except ancient Iranian festivals`() {
        val settings = persian.toEventsSettings(homeTimeZone = TimeZone.of("Asia/Tehran"))

        settings.preferences.enabledSources shouldBe EventSource.entries.toSet() - EventSource.ANCIENT_IRAN
        settings.preferences.islamicVariant shouldBe IslamicVariant.IRAN_OFFICIAL
        settings.preferences.homeTimeZone.id shouldBe "Asia/Tehran"
        settings.weekend shouldBe persian.weekend
        settings.hijriOffset.shouldBeNull()
    }

    @Test
    fun `only a set, non-zero, in-range Hijri offset is used`() {
        fun offset(
            days: Int,
            setAtMillis: Long?,
        ) = persian
            .copy(hijriOffsetDays = days, hijriOffsetSetAtEpochMillis = setAtMillis)
            .toEventsSettings()
            .hijriOffset

        offset(-2, setAt.toEpochMilliseconds()) shouldBe HijriOffset(-2, setAt)
        offset(1, null).shouldBeNull()
        offset(0, setAt.toEpochMilliseconds()).shouldBeNull()
        offset(3, setAt.toEpochMilliseconds()).shouldBeNull()
    }
}
