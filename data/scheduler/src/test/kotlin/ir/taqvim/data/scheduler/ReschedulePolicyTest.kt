/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.scheduler

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.AsrJuristic
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.PrayerMethod
import ir.taqvim.data.database.AlarmKind
import ir.taqvim.data.preferences.AthanAlert
import ir.taqvim.data.preferences.AthanPrayer
import ir.taqvim.data.preferences.AthanSound
import ir.taqvim.data.preferences.ChosenPlace
import ir.taqvim.data.preferences.IslamicOverrideOrigin
import ir.taqvim.data.preferences.IslamicOverrideSetting
import ir.taqvim.data.preferences.PlaceSource
import ir.taqvim.data.preferences.ThemeMode
import ir.taqvim.data.preferences.UserPreferences
import org.junit.jupiter.api.Test

/** T-604 (U): the reschedule matrix, preference impact and system broadcast actions. */
class ReschedulePolicyTest {
    private val policy = ReschedulePolicy()
    private val all = AlarmKind.entries.toSet()
    private val none = emptySet<AlarmKind>()
    private val base = UserPreferences.defaultsFor("fa")
    private val tehran = ChosenPlace(PlaceSource.CITY, 112_931, "Tehran", Coordinates(35.69, 51.39), "Asia/Tehran")
    private val fajrAthan = AthanAlert(enabled = true, gapMinutes = -5)

    private fun <T> otherThan(
        values: List<T>,
        current: T,
    ): T = values.first { it != current }

    @Test
    fun `reschedule matrix for system events`() {
        val expected =
            mapOf(
                RescheduleEvent.BootCompleted to ReschedulePlan(restore = all, recompute = all),
                RescheduleEvent.PackageReplaced to ReschedulePlan(restore = all, recompute = all),
                RescheduleEvent.TimeChanged to ReschedulePlan(restore = none, recompute = all),
                RescheduleEvent.TimeZoneChanged to ReschedulePlan(restore = none, recompute = all),
                RescheduleEvent.ExactAlarmPermissionChanged to ReschedulePlan(restore = all, recompute = none),
            )

        expected.forEach { (event, plan) -> withClue(event) { policy.planFor(event) shouldBe plan } }
    }

    @Test
    fun `changed alarm inputs recompute exactly their kinds`() {
        policy.planFor(RescheduleEvent.AlarmInputsChanged(setOf(AlarmKind.REMINDER))) shouldBe
            ReschedulePlan(restore = none, recompute = setOf(AlarmKind.REMINDER))
        policy.planFor(RescheduleEvent.AlarmInputsChanged(none)) shouldBe ReschedulePlan(none, none)
    }

    @Test
    fun `preference changes recompute only the kinds whose times they move`() {
        val prayer = setOf(AlarmKind.PRAYER)
        val reminder = setOf(AlarmKind.REMINDER)
        val cases =
            listOf(
                base.copy(prayerMethod = otherThan(PrayerMethod.entries, base.prayerMethod)) to prayer,
                base.copy(asrJuristic = otherThan(AsrJuristic.entries, base.asrJuristic)) to prayer,
                base.copy(place = tehran) to prayer,
                base.copy(athan = base.athan.copy(alerts = base.athan.alerts + (AthanPrayer.FAJR to fajrAthan))) to
                    prayer,
                base.copy(athan = base.athan.copy(vibrate = !base.athan.vibrate)) to prayer,
                base.copy(athan = base.athan.copy(useIranTime = true)) to prayer,
                base.copy(athan = base.athan.copy(sound = AthanSound("content://athan", null))) to prayer,
                base.copy(islamicVariant = otherThan(IslamicVariant.entries, base.islamicVariant)) to reminder,
                base.copy(hijriOffsetDays = 1) to reminder,
                base.copy(islamicOverride = IslamicOverrideSetting(IslamicOverrideOrigin.IMPORTED, "{}")) to reminder,
                base.copy(hijriOffsetSetAtEpochMillis = 1L) to reminder,
                base.copy(themeMode = ThemeMode.DARK) to none,
                base.copy(languageCode = "en") to none,
                base to none,
                base.copy(asrJuristic = otherThan(AsrJuristic.entries, base.asrJuristic), hijriOffsetDays = -1) to
                    prayer + reminder,
            )

        cases.forEach { (changed, kinds) ->
            withClue(changed) {
                policy.planFor(RescheduleEvent.PreferencesChanged(base, changed)) shouldBe
                    ReschedulePlan(restore = none, recompute = kinds)
            }
        }
    }

    @Test
    fun `system broadcast actions map to events`() {
        val actions =
            mapOf(
                "android.intent.action.BOOT_COMPLETED" to RescheduleEvent.BootCompleted,
                "android.intent.action.MY_PACKAGE_REPLACED" to RescheduleEvent.PackageReplaced,
                "android.intent.action.TIME_SET" to RescheduleEvent.TimeChanged,
                "android.intent.action.TIMEZONE_CHANGED" to RescheduleEvent.TimeZoneChanged,
                "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED" to
                    RescheduleEvent.ExactAlarmPermissionChanged,
            )

        actions.forEach { (action, event) -> RescheduleEvent.forAction(action) shouldBe event }
        RescheduleEvent.forAction(null) shouldBe null
        RescheduleEvent.forAction("android.intent.action.SCREEN_ON") shouldBe null
    }
}
