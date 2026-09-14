/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import kotlinx.datetime.LocalDate

/** Where tapping a widget (or a part of it) leads. */
sealed interface WidgetClickTarget {
    data object Today : WidgetClickTarget

    data class Day(
        val date: LocalDate,
    ) : WidgetClickTarget

    data class Event(
        val id: Long,
    ) : WidgetClickTarget

    data object PrayerTimes : WidgetClickTarget

    /** The editor of a new event on [date] (T-1205). */
    data class NewEvent(
        val date: LocalDate,
    ) : WidgetClickTarget
}

/**
 * Widget click targets as the documented `taqvim://` links of ADR-0016 (T-1103), which `MainActivity` handles. Links
 * only open screens, so a widget tap never changes data.
 */
object WidgetLinks {
    const val SCHEME: String = "taqvim"

    /**
     * The link of [target]: `calendar` (today), `day/<y-m-d>?calendar=gregorian` (widget days are Gregorian dates),
     * `event/<id>` (a personal event in the editor), `event/new/<y-m-d>?calendar=gregorian` (a new event on that day)
     * or `times`.
     */
    fun uri(target: WidgetClickTarget): String =
        when (target) {
            WidgetClickTarget.Today -> "$SCHEME://calendar"
            is WidgetClickTarget.NewEvent -> "$SCHEME://event/new/${target.date}?calendar=gregorian"
            is WidgetClickTarget.Day -> "$SCHEME://day/${target.date}?calendar=gregorian"
            is WidgetClickTarget.Event -> "$SCHEME://event/${target.id}"
            WidgetClickTarget.PrayerTimes -> "$SCHEME://times"
        }

    /** An `ACTION_VIEW` intent for [target] limited to this app's package, starting it in a new task. */
    fun intent(
        context: Context,
        target: WidgetClickTarget,
    ): Intent =
        Intent(Intent.ACTION_VIEW, uri(target).toUri())
            .setPackage(context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

/** Broadcast actions and extras of the widget framework, and the triggers each broadcast stands for. */
object WidgetBroadcasts {
    /** The framework's own wake-up alarm; [EXTRA_TRIGGERS] lists the due trigger names. */
    const val ACTION_WAKE_UP: String = "ir.taqvim.feature.widgets.action.WAKE_UP"

    /** Sent by widget receivers after a widget was placed or removed, to re-plan the wake-up. */
    const val ACTION_RESCHEDULE: String = "ir.taqvim.feature.widgets.action.RESCHEDULE"

    /** Sent by widget receivers after widgets were removed; [EXTRA_APP_WIDGET_IDS] lists them. */
    const val ACTION_DELETED: String = "ir.taqvim.feature.widgets.action.DELETED"
    const val EXTRA_TRIGGERS: String = "ir.taqvim.feature.widgets.extra.TRIGGERS"
    const val EXTRA_APP_WIDGET_IDS: String = "ir.taqvim.feature.widgets.extra.APP_WIDGET_IDS"

    private const val TIME_SET = "android.intent.action.TIME_SET"
    private const val TIMEZONE_CHANGED = "android.intent.action.TIMEZONE_CHANGED"
    private const val LOCALE_CHANGED = "android.intent.action.LOCALE_CHANGED"
    private const val MY_PACKAGE_REPLACED = "android.intent.action.MY_PACKAGE_REPLACED"

    private val WAKE_UP_TRIGGERS =
        listOf(
            WidgetUpdateTrigger.DayChanged,
            WidgetUpdateTrigger.PrayerTimeReached,
            WidgetUpdateTrigger.MinuteTick,
        ).associateBy { it.toString() }

    /** Names of wake-up triggers, as stored in [EXTRA_TRIGGERS]. */
    fun namesOf(triggers: Set<WidgetUpdateTrigger>): Array<String> =
        triggers
            .map { it.toString() }
            .filter { it in WAKE_UP_TRIGGERS }
            .sorted()
            .toTypedArray()

    /**
     * The triggers of a broadcast with [action] and wake-up [triggerNames]: an empty set means "only reschedule", and
     * `null` means the broadcast is not for the widget framework.
     */
    fun triggersFor(
        action: String?,
        triggerNames: Array<String>?,
    ): Set<WidgetUpdateTrigger>? =
        when (action) {
            ACTION_WAKE_UP -> triggerNames.orEmpty().mapNotNull { WAKE_UP_TRIGGERS[it] }.toSet()
            ACTION_RESCHEDULE -> emptySet()
            TIME_SET, TIMEZONE_CHANGED, LOCALE_CHANGED, MY_PACKAGE_REPLACED -> setOf(WidgetUpdateTrigger.Everything)
            else -> null
        }
}
