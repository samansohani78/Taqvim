/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.automation

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.app.MainActivity
import ir.taqvim.app.navigation.AppDestination
import ir.taqvim.app.navigation.DeepLinks
import ir.taqvim.feature.notification.TodaySummary
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * T-1214/T-1215 (R): the launcher entry follows the day and the setting with `DONT_KILL_APP`, only changed components
 * are written, and launcher shortcuts open screens through `MainActivity` while following the enabled entry. A plain
 * [Application] hosts the test so the app's own start-up refresh and shortcut publishing cannot race it.
 */
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class LauncherIconTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val packageManager = context.packageManager
    private val switcher = LauncherIconSwitcher(context)
    private val now = Instant.parse("2026-09-13T08:30:00Z")
    private val midnight = Instant.parse("2026-09-13T20:30:00Z")

    private fun component(name: String) = ComponentName(context.packageName, name)

    private fun state(name: String): Int = packageManager.getComponentEnabledSetting(component(name))

    private fun enabledEntries(): List<String> =
        LauncherIcons.all.filter {
            val state = state(it)
            state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && it == LauncherIcons.DEFAULT)
        }

    private fun summary(day: Int) =
        TodaySummary(dayOfMonth = day, dayNumber = "$day", title = "$day Shahrivar", weekday = "", nextDayAt = midnight)

    @Test
    fun withTheSettingOffNothingIsWritten() {
        switcher.apply(LauncherIconPlan.of(22, dynamic = false)) shouldBe 0

        switcher.enabledEntry() shouldBe LauncherIcons.DEFAULT
        LauncherIcons.all.forEach { state(it) shouldBe PackageManager.COMPONENT_ENABLED_STATE_DEFAULT }
    }

    @Test
    fun theDayChangeSwitchesToTheNextDayWithoutKillingTheApp(): Unit =
        runTest {
            var day = 22
            var dynamic = true
            val switched = mutableListOf<String>()
            val refresh = LauncherIconRefresh({ dynamic }, { summary(day) }, switcher) { switched += it }

            refresh.refresh(now) shouldBe midnight
            enabledEntries() shouldBe listOf(LauncherIcons.dayAlias(22))
            state(LauncherIcons.DEFAULT) shouldBe PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            shadowOf(packageManager).getComponentEnabledSettingFlags(component(LauncherIcons.dayAlias(22))) shouldBe
                PackageManager.DONT_KILL_APP
            refresh.refresh(now)
            switched shouldBe listOf(LauncherIcons.dayAlias(22))

            day = 23
            refresh.refresh(midnight) shouldBe midnight
            enabledEntries() shouldBe listOf(LauncherIcons.dayAlias(23))
            state(LauncherIcons.dayAlias(22)) shouldBe PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            switcher.enabledEntry() shouldBe LauncherIcons.dayAlias(23)

            dynamic = false
            refresh.refresh(midnight).shouldBeNull()
            enabledEntries() shouldBe listOf(LauncherIcons.DEFAULT)
            switched shouldBe listOf(LauncherIcons.dayAlias(22), LauncherIcons.dayAlias(23), LauncherIcons.DEFAULT)
        }

    @Test
    fun everyLauncherEntryIsAnAliasOfMainActivity() {
        LauncherIcons.all.forEach { name ->
            val info = packageManager.getActivityInfo(component(name), PackageManager.MATCH_DISABLED_COMPONENTS)
            info.targetActivity shouldBe MainActivity::class.java.name
        }
    }

    @Test
    fun shortcutsOpenScreensThroughMainActivityAndFollowTheEnabledEntry() {
        AppShortcuts.publish(context, LauncherIcons.DEFAULT) shouldBe true
        AppShortcuts.publish(context, LauncherIcons.DEFAULT) shouldBe false

        val published = ShortcutManagerCompat.getDynamicShortcuts(context).sortedBy { it.rank }
        published.map { it.id } shouldBe listOf("today", "new_event", "prayer_times", "converter")
        published.map { it.shortLabel.toString() } shouldBe
            listOf("Today", "New event", "Prayer times", "Date converter")
        published.map { DeepLinks.parse(it.intent.data.toString()) } shouldBe
            listOf(AppDestination.Calendar, AppDestination.EventEditor(), AppDestination.Times, AppDestination.Tools)
        published.forEach { shortcut ->
            shortcut.intent.`package` shouldBe context.packageName
            shortcut.activity shouldBe component(LauncherIcons.DEFAULT)
            packageManager.resolveActivity(shortcut.intent, 0)?.activityInfo?.name shouldBe
                MainActivity::class.java.name
        }

        AppShortcuts.publish(context, LauncherIcons.dayAlias(1)) shouldBe true
        ShortcutManagerCompat.getDynamicShortcuts(context).map { it.activity?.className }.distinct() shouldBe
            listOf(LauncherIcons.dayAlias(1))
    }
}
