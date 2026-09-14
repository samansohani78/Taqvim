/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Instant

/** A prayer time of today, already localized (e.g. "Maghrib" and "19:12"); [isNext] marks the next one. */
data class SummaryPrayer(
    val name: String,
    val time: String,
    val isNext: Boolean = false,
)

/**
 * What the persistent notification (T-1213) and the Quick Settings tile (T-1215) show for today, every text already
 * localized in the app language and digits.
 *
 * @property dayOfMonth today's day of the month in the primary calendar (1…31).
 * @property dayNumber that day in the app's digits, drawn into the day icon.
 * @property title today's date in the primary calendar, e.g. "22 Shahrivar 1405".
 * @property otherDates today in the user's other calendars.
 * @property holidays today's holidays.
 * @property prayers today's prayer times in order, empty without a chosen place.
 * @property nextDayAt when today ends, in the zone "today" is taken in.
 * @property nextPrayerAt when the next prayer begins, or `null` without a chosen place.
 */
data class TodaySummary(
    val dayOfMonth: Int,
    val dayNumber: String,
    val title: String,
    val weekday: String,
    val otherDates: List<String> = emptyList(),
    val holidays: List<String> = emptyList(),
    val prayers: List<SummaryPrayer> = emptyList(),
    val nextDayAt: Instant,
    val nextPrayerAt: Instant? = null,
) {
    /** The next prayer of today, or `null` after the last one or without a place. */
    val nextPrayer: SummaryPrayer? get() = prayers.firstOrNull { it.isNext }

    /** When this summary goes stale: the next prayer when it comes before the day ends, else the end of the day. */
    val staleAt: Instant get() = nextPrayerAt?.takeIf { it < nextDayAt } ?: nextDayAt
}

/** Today's summary at an instant; bound in `:app` over the preferences, events and prayer times. */
fun interface TodaySummarySource {
    suspend fun load(now: Instant): TodaySummary
}

/** Whether the persistent notification is on (T-1500 `persistentNotification`) and shows a large day number. */
data class PersistentNotificationOptions(
    val enabled: Boolean,
    val largeNumber: Boolean = false,
)

/** The stored [PersistentNotificationOptions]; bound in `:app`. */
fun interface PersistentNotificationOptionsSource {
    suspend fun options(): PersistentNotificationOptions
}

/** Links and helpers shared by the surfaces outside the app (T-1213, T-1215). */
object TodaySurfaces {
    /** The link every surface opens: today in the calendar (T-1103). */
    const val TODAY_LINK: String = "taqvim://calendar"

    /** An intent that opens today in Taqvim and nothing else. */
    fun openToday(context: Context): Intent =
        Intent(Intent.ACTION_VIEW, TODAY_LINK.toUri())
            .setPackage(context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** An immutable pending intent of [openToday], told apart from other pending intents by [requestCode]. */
    fun openTodayPendingIntent(
        context: Context,
        requestCode: Int,
    ): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            openToday(context),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    /** The result of [block], or `null` when it fails; cancellation is never swallowed. */
    suspend fun <T> attempt(block: suspend () -> T): T? {
        val result = runCatching { block() }
        val failure = result.exceptionOrNull()
        if (failure is CancellationException) throw failure
        return result.getOrNull()
    }
}
