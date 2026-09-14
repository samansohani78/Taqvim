/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Test

/** T-1200 click targets and the framework's broadcasts. */
class WidgetLinksTest {
    @Test
    fun `click targets are deep links`() {
        WidgetLinks.uri(WidgetClickTarget.Today) shouldBe "taqvim://calendar"
        WidgetLinks.uri(WidgetClickTarget.Day(LocalDate(2026, 3, 21))) shouldBe
            "taqvim://day/2026-03-21?calendar=gregorian"
        WidgetLinks.uri(WidgetClickTarget.Event(42)) shouldBe "taqvim://event/42"
        WidgetLinks.uri(WidgetClickTarget.PrayerTimes) shouldBe "taqvim://times"
        WidgetLinks.uri(WidgetClickTarget.NewEvent(LocalDate(2026, 9, 23))) shouldBe
            "taqvim://event/new/2026-09-23?calendar=gregorian"
        WidgetLinks.uri(WidgetClickTarget.Astronomy) shouldBe "taqvim://astronomy"
        WidgetLinks.uri(WidgetClickTarget.WorldMap) shouldBe "taqvim://map"
    }

    @Test
    fun `broadcasts map to triggers`() {
        val names =
            WidgetBroadcasts.namesOf(
                setOf(WidgetUpdateTrigger.MinuteTick, WidgetUpdateTrigger.DayChanged, WidgetUpdateTrigger.Everything),
            )
        names.toList() shouldBe listOf("DayChanged", "MinuteTick")
        WidgetBroadcasts.triggersFor(WidgetBroadcasts.ACTION_WAKE_UP, names) shouldBe
            setOf(WidgetUpdateTrigger.DayChanged, WidgetUpdateTrigger.MinuteTick)
        WidgetBroadcasts.triggersFor(WidgetBroadcasts.ACTION_WAKE_UP, arrayOf("Unknown", "PrayerTimeReached")) shouldBe
            setOf(WidgetUpdateTrigger.PrayerTimeReached)
        WidgetBroadcasts.triggersFor(WidgetBroadcasts.ACTION_WAKE_UP, null) shouldBe emptySet()
        WidgetBroadcasts.triggersFor(WidgetBroadcasts.ACTION_RESCHEDULE, null) shouldBe emptySet()
        listOf(
            "android.intent.action.TIME_SET",
            "android.intent.action.TIMEZONE_CHANGED",
            "android.intent.action.LOCALE_CHANGED",
            "android.intent.action.MY_PACKAGE_REPLACED",
        ).forEach { WidgetBroadcasts.triggersFor(it, null) shouldBe setOf(WidgetUpdateTrigger.Everything) }
        WidgetBroadcasts.triggersFor("android.intent.action.BOOT_COMPLETED", null).shouldBeNull()
        WidgetBroadcasts.triggersFor(null, null).shouldBeNull()
    }
}
