/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.automation

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import ir.taqvim.feature.notification.DailyRefresh
import ir.taqvim.feature.notification.TodaySummarySource
import kotlin.time.Instant

/** The launcher entries of the manifest (T-1214, ADR-0022): the default icon and one alias per day of the month. */
object LauncherIcons {
    const val DEFAULT: String = "ir.taqvim.app.LauncherDefault"
    const val DAYS: Int = 31
    private const val DAY_PREFIX = "ir.taqvim.app.LauncherDay"

    /** The alias showing [day] (1…[DAYS]). */
    fun dayAlias(day: Int): String {
        require(day in 1..DAYS) { "No launcher icon for day $day" }
        return DAY_PREFIX + day.toString().padStart(2, '0')
    }

    /** Every launcher entry, the default first. */
    val all: List<String> = listOf(DEFAULT) + (1..DAYS).map(::dayAlias)
}

/** The launcher entry to enable; every other entry is disabled. */
data class LauncherIconPlan(
    val enabled: String,
) {
    val disabled: List<String> get() = LauncherIcons.all - enabled

    companion object {
        /**
         * The default icon, or the day alias of [dayOfMonth] when the [dynamic] icon is on. Days outside 1…31 (e.g. the
         * 32nd day of a Nepali month) fall back to the default icon.
         */
        fun of(
            dayOfMonth: Int?,
            dynamic: Boolean,
        ): LauncherIconPlan {
            val day = dayOfMonth?.takeIf { dynamic && it in 1..LauncherIcons.DAYS }
            return LauncherIconPlan(day?.let(LauncherIcons::dayAlias) ?: LauncherIcons.DEFAULT)
        }
    }
}

/**
 * Switches the launcher entry with `PackageManager.setComponentEnabledSetting` and `DONT_KILL_APP`, so a running app is
 * not stopped. The new entry is enabled before the old one is disabled, so the launcher never sees the app without an
 * entry; components already in the wanted state are not touched, so a user who never turns the icon on causes no
 * package changes at all.
 */
class LauncherIconSwitcher(
    context: Context,
) {
    private val app = context.applicationContext
    private val packageManager = app.packageManager

    /** Applies [plan]; returns how many components changed. */
    fun apply(plan: LauncherIconPlan): Int {
        val wanted = listOf(plan.enabled to true) + plan.disabled.map { it to false }
        return wanted.count { (name, enabled) ->
            val component = ComponentName(app.packageName, name)
            val changes = isEnabled(component) != enabled
            if (changes) {
                val state =
                    if (enabled) {
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    } else {
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    }
                packageManager.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP)
            }
            changes
        }
    }

    /** The first enabled launcher entry, or the default when none is (which [apply] never leaves). */
    fun enabledEntry(): String =
        LauncherIcons.all.firstOrNull { isEnabled(ComponentName(app.packageName, it)) } ?: LauncherIcons.DEFAULT

    private fun isEnabled(component: ComponentName): Boolean =
        when (packageManager.getComponentEnabledSetting(component)) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> component.className == LauncherIcons.DEFAULT
            else -> false
        }
}

/**
 * Keeps the launcher icon (T-1214) on today's day while the dynamic icon is on, switching at the day change; with it
 * off the default icon is restored and no wake-up is needed. [onSwitched] runs with the new entry after a switch, so
 * the app shortcuts (attached to the enabled entry) are published again.
 */
class LauncherIconRefresh(
    private val dynamic: suspend () -> Boolean,
    private val summaries: TodaySummarySource,
    private val switcher: LauncherIconSwitcher,
    private val onSwitched: (String) -> Unit = {},
) : DailyRefresh {
    override suspend fun refresh(now: Instant): Instant? {
        if (!dynamic()) {
            switch(LauncherIconPlan.of(null, dynamic = false))
            return null
        }
        val summary = summaries.load(now)
        switch(LauncherIconPlan.of(summary.dayOfMonth, dynamic = true))
        return summary.nextDayAt
    }

    private fun switch(plan: LauncherIconPlan) {
        if (switcher.apply(plan) > 0) onSwitched(plan.enabled)
    }
}
