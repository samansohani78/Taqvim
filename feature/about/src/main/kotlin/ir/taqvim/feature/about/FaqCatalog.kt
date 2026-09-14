/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.about

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import ir.taqvim.core.i18n.PersianText
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet

/** Topics of the in-app FAQ (T-1901), in display order. */
enum class FaqTopic(
    @param:StringRes val title: Int,
) {
    CALENDARS(R.string.about_faq_topic_calendars),
    OCCASIONS(R.string.about_faq_topic_occasions),
    PRAYER(R.string.about_faq_topic_prayer),
    NOTIFICATIONS(R.string.about_faq_topic_notifications),
    BACKUP_PRIVACY(R.string.about_faq_topic_backup_privacy),
}

/**
 * One question of the FAQ with its answer and, where a screen of the app helps, a `taqvim://` [link] from
 * docs/AUTOMATION.md that opens it. Answers describe what the app does today; pending features say so.
 */
enum class FaqEntry(
    val topic: FaqTopic,
    @param:StringRes val question: Int,
    @param:StringRes val answer: Int,
    val link: String?,
) {
    ISLAMIC_DATE(
        FaqTopic.CALENDARS,
        R.string.about_faq_islamic_date_q,
        R.string.about_faq_islamic_date_a,
        "taqvim://settings/islamic-variant",
    ),
    MAIN_CALENDAR(
        FaqTopic.CALENDARS,
        R.string.about_faq_main_calendar_q,
        R.string.about_faq_main_calendar_a,
        "taqvim://settings/main-calendar",
    ),
    NEPALI(FaqTopic.CALENDARS, R.string.about_faq_nepali_q, R.string.about_faq_nepali_a, null),
    WIDGETS(FaqTopic.CALENDARS, R.string.about_faq_widgets_q, R.string.about_faq_widgets_a, null),
    HOLIDAY_SOURCES(
        FaqTopic.OCCASIONS,
        R.string.about_faq_holiday_sources_q,
        R.string.about_faq_holiday_sources_a,
        null,
    ),
    MISSING_DAYS(FaqTopic.OCCASIONS, R.string.about_faq_missing_days_q, R.string.about_faq_missing_days_a, null),
    EVENT_SOURCES(
        FaqTopic.OCCASIONS,
        R.string.about_faq_event_sources_q,
        R.string.about_faq_event_sources_a,
        "taqvim://settings/event-sources",
    ),
    WRONG_DATE(FaqTopic.OCCASIONS, R.string.about_faq_wrong_date_q, R.string.about_faq_wrong_date_a, null),
    PRAYER_METHOD(
        FaqTopic.PRAYER,
        R.string.about_faq_prayer_method_q,
        R.string.about_faq_prayer_method_a,
        "taqvim://settings/prayer-method",
    ),
    NO_LOCATION(
        FaqTopic.PRAYER,
        R.string.about_faq_no_location_q,
        R.string.about_faq_no_location_a,
        "taqvim://settings/location",
    ),
    HIGH_LATITUDE(
        FaqTopic.PRAYER,
        R.string.about_faq_high_latitude_q,
        R.string.about_faq_high_latitude_a,
        "taqvim://settings/high-latitude",
    ),
    ATHAN(FaqTopic.NOTIFICATIONS, R.string.about_faq_athan_q, R.string.about_faq_athan_a, "taqvim://settings/athan"),
    REMINDERS(
        FaqTopic.NOTIFICATIONS,
        R.string.about_faq_reminders_q,
        R.string.about_faq_reminders_a,
        "taqvim://settings/privacy",
    ),
    PASSPHRASE(
        FaqTopic.BACKUP_PRIVACY,
        R.string.about_faq_passphrase_q,
        R.string.about_faq_passphrase_a,
        "taqvim://settings/backup",
    ),
    PRIVACY(
        FaqTopic.BACKUP_PRIVACY,
        R.string.about_faq_privacy_q,
        R.string.about_faq_privacy_a,
        "taqvim://settings/privacy",
    ),
}

/** The texts of one [FaqEntry] in the current language, used for searching. */
data class FaqText(
    val topic: String,
    val question: String,
    val answer: String,
)

/** Entries of one topic. */
@Immutable
data class FaqGroup(
    val topic: FaqTopic,
    val entries: ImmutableList<FaqEntry>,
)

/** The FAQ page: the [query], the matching [groups] and the [expanded] entries. */
@Immutable
data class FaqContent(
    val query: String = "",
    val groups: ImmutableList<FaqGroup> = allGroups(),
    val expanded: ImmutableSet<FaqEntry> = persistentSetOf(),
) {
    /** Nothing matched a non-blank [query]. */
    val noResults: Boolean get() = groups.isEmpty()
}

/** Searching the FAQ. */
internal object FaqSearch {
    /**
     * The [FaqEntry]s whose topic, question or answer in [texts] contains every word of [query], compared by
     * [PersianText.searchKey] with Persian and Arabic-Indic digits folded to Latin; all entries for a blank query.
     */
    fun matches(
        texts: Map<FaqEntry, FaqText>,
        query: String,
    ): List<FaqEntry> {
        val words = query.split(' ', '‌').map(::key).filter(String::isNotEmpty)
        if (words.isEmpty()) return FaqEntry.entries
        return FaqEntry.entries.filter { entry ->
            val text = texts[entry] ?: return@filter false
            val haystack = key("${text.topic} ${text.question} ${text.answer}")
            words.all(haystack::contains)
        }
    }

    private fun key(text: String): String = PersianText.searchKey(text).map(::latinDigit).joinToString("")

    private fun latinDigit(char: Char): Char = Character.digit(char, RADIX).takeIf { it >= 0 }?.digitToChar() ?: char

    private const val RADIX = 10
}

/** The FAQ page content for [query] over [texts] with [expanded] entries. */
internal fun faqContent(
    texts: Map<FaqEntry, FaqText>,
    query: String,
    expanded: Set<FaqEntry>,
): FaqContent {
    val matching = FaqSearch.matches(texts, query).toSet()
    return FaqContent(
        query = query,
        groups = groups(FaqEntry.entries.filter { it in matching }),
        expanded = expanded.filter { it in matching }.toImmutableSet(),
    )
}

private fun allGroups(): ImmutableList<FaqGroup> = groups(FaqEntry.entries)

private fun groups(entries: List<FaqEntry>): ImmutableList<FaqGroup> =
    FaqTopic.entries
        .mapNotNull { topic ->
            val ofTopic = entries.filter { it.topic == topic }
            if (ofTopic.isEmpty()) null else FaqGroup(topic, ofTopic.toImmutableList())
        }.toImmutableList()
