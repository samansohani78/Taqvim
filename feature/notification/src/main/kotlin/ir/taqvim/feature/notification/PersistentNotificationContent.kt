/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import android.content.Context
import androidx.core.content.edit

/**
 * What the persistent notification (T-1213) shows. Collapsed: [title] (today in the primary calendar), [text] (the
 * next prayer, else the first other date) and [subText] (the weekday); expanded: every line of [lines] (other
 * calendars, holidays and the prayer strip). [iconText] is the day number of the status bar icon, also drawn as the
 * large icon when [largeNumber] is on.
 */
data class PersistentNotificationContent(
    val title: String,
    val subText: String,
    val text: String,
    val lines: List<String>,
    val iconText: String,
    val largeNumber: Boolean,
) {
    /** Equal for equal content; the notification is posted again only when it changes. */
    val key: String
        get() = (listOf(title, subText, text, iconText, largeNumber.toString()) + lines).joinToString(SEPARATOR)

    private companion object {
        const val SEPARATOR = ""
    }
}

/** Builds [PersistentNotificationContent] from a [TodaySummary]. */
object PersistentNotificationContents {
    private const val STRIP_SEPARATOR = " · "

    /** The content for [summary]; [nextPrayer] formats the next prayer line (e.g. "Maghrib 19:12"). */
    fun of(
        summary: TodaySummary,
        largeNumber: Boolean,
        nextPrayer: (SummaryPrayer) -> String,
    ): PersistentNotificationContent {
        val strip = summary.prayers.joinToString(STRIP_SEPARATOR) { "${it.name} ${it.time}" }
        return PersistentNotificationContent(
            title = summary.title,
            subText = summary.weekday,
            text = summary.nextPrayer?.let(nextPrayer) ?: summary.otherDates.firstOrNull() ?: summary.weekday,
            lines = summary.otherDates + summary.holidays + listOfNotNull(strip.ifEmpty { null }),
            iconText = summary.dayNumber,
            largeNumber = largeNumber,
        )
    }
}

/** The key of the last posted persistent notification, kept across process restarts. */
interface PostedNotificationStore {
    fun postedKey(): String?

    fun remember(key: String)

    fun forget()
}

/** [PostedNotificationStore] in the app's private shared preferences. */
class SharedPreferencesPostedNotificationStore(
    context: Context,
) : PostedNotificationStore {
    private val preferences = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    override fun postedKey(): String? = preferences.getString(KEY, null)

    override fun remember(key: String) {
        preferences.edit { putString(KEY, key) }
    }

    override fun forget() {
        preferences.edit { remove(KEY) }
    }

    private companion object {
        const val FILE = "persistent_notification"
        const val KEY = "posted_key"
    }
}
