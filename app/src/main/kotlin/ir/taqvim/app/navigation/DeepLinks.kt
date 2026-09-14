/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.navigation

import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.IranIslamicCalendar
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.nlp.ParseContext
import ir.taqvim.core.nlp.TextDateDetector
import ir.taqvim.feature.settings.SettingsItemId
import java.net.URLDecoder

/**
 * The `taqvim://` links of T-1103 (F-14; the contract is ADR-0016 and docs/AUTOMATION.md) as destinations. Links only
 * open screens and never change data; a link that cannot be read opens the calendar.
 */
internal object DeepLinks {
    const val SCHEME: String = "taqvim"

    /** Longest text a link or selected text hands to a screen. */
    const val MAX_TEXT_LENGTH: Int = 500

    /** Julian day numbers accepted from links and text: the Gregorian years 1…9999. */
    val JDN_RANGE: LongRange = 1_721_426L..5_373_484L

    private const val DEFAULT_CALENDAR = "persian"
    private const val BACKUP = "BACKUP"
    private const val PRIVACY = "PRIVACY"
    private val NUMERIC_DATE = Regex("""(\d{1,4})-(\d{1,2})-(\d{1,2})""")

    private val CALENDARS: Map<String, CalendarArithmetic> =
        mapOf(
            DEFAULT_CALENDAR to PersianCalendarSystem,
            "islamic" to IranIslamicCalendar(),
            "gregorian" to GregorianCalendarSystem,
        )

    private val HANDLERS: Map<String, (Link) -> AppDestination?> =
        mapOf(
            "calendar" to { _ -> AppDestination.Calendar },
            "day" to ::day,
            "event" to ::event,
            "occasion" to ::occasion,
            "convert" to ::converter,
            "times" to { _ -> AppDestination.Times },
            "astronomy" to { _ -> AppDestination.Astronomy },
            "search" to ::search,
            "settings" to ::settings,
        )

    /** The screen [link] opens; the calendar for anything that is not a readable `taqvim://` link. */
    fun parse(link: String): AppDestination =
        runCatching { Link.of(link)?.let { HANDLERS[it.host]?.invoke(it) } }.getOrNull() ?: AppDestination.Calendar

    /** `day/<y-m-d>[?calendar=persian|islamic|gregorian]`. */
    private fun day(link: Link): AppDestination? {
        val calendar = CALENDARS[link.query["calendar"]?.lowercase() ?: DEFAULT_CALENDAR] ?: return null
        val (year, month, day) = link.path.singleOrNull()?.let(::numbers) ?: return null
        return calendar
            .takeIf { it.isValid(year, month, day) }
            ?.toJdn(calendar.date(year, month, day))
            ?.value
            ?.takeIf { it in JDN_RANGE }
            ?.let(AppDestination::Day)
    }

    /** `event/<id>`: a personal event in the editor. */
    private fun event(link: Link): AppDestination? =
        link.path
            .singleOrNull()
            ?.toLongOrNull()
            ?.takeIf { it > 0 }
            ?.let { AppDestination.EventEditor(it) }

    /** `occasion/<event id>?day=<jdn>`: the day of an official event (T-1001 reminders). */
    private fun occasion(link: Link): AppDestination? =
        link.query["day"]
            ?.toLongOrNull()
            ?.takeIf { it in JDN_RANGE }
            ?.let(AppDestination::Day)

    /** `convert?date=<text>[&from=<calendar>]`: a numeric date with its calendar is written with month names. */
    private fun converter(link: Link): AppDestination {
        val date =
            link.query["date"]
                ?.trim()
                ?.take(MAX_TEXT_LENGTH)
                .orEmpty()
        if (date.isEmpty()) return AppDestination.Tools
        val calendar = link.query["from"]?.lowercase()?.let(CALENDARS::get)
        return AppDestination.Converter(calendar?.let { named(date, it) } ?: date)
    }

    /** `search?q=<text>`. */
    private fun search(link: Link): AppDestination =
        link.query["q"]
            ?.trim()
            ?.take(MAX_TEXT_LENGTH)
            ?.takeIf { it.isNotEmpty() }
            ?.let(AppDestination::SearchFor) ?: AppDestination.Search

    /** `settings[/<item>]`: backup, privacy, or the settings home at an item such as `main-calendar`. */
    private fun settings(link: Link): AppDestination {
        val name =
            link.path
                .singleOrNull()
                ?.uppercase()
                ?.replace('-', '_') ?: return AppDestination.Settings()
        return when (name) {
            BACKUP -> AppDestination.Backup
            PRIVACY -> AppDestination.Privacy
            else -> AppDestination.Settings(SettingsItemId.entries.firstOrNull { it.name == name }?.name)
        }
    }

    /** [text] `y-m-d` written as "d Month y" with the English month names of [calendar]; `null` for other text. */
    private fun named(
        text: String,
        calendar: CalendarArithmetic,
    ): String? {
        val (year, month, day) = numbers(text) ?: return null
        val names = englishMonths(calendar.system)
        return if (names != null && calendar.isValid(year, month, day)) "$day ${names[month - 1]} $year" else null
    }

    private fun numbers(text: String): List<Int>? =
        NUMERIC_DATE
            .matchEntire(text)
            ?.groupValues
            ?.drop(1)
            ?.map(String::toInt)

    private fun englishMonths(system: CalendarSystem): List<String>? {
        val names = LanguageTable.forCode("en")?.monthNames ?: return null
        return when (system) {
            CalendarSystem.PERSIAN -> names.persian
            CalendarSystem.ISLAMIC -> names.islamic
            CalendarSystem.GREGORIAN -> names.gregorian
            CalendarSystem.NEPALI -> names.nepali
        }
    }

    /** A `taqvim://host/segment…?key=value…` link with its parts percent-decoded. */
    private class Link(
        val host: String,
        val path: List<String>,
        val query: Map<String, String>,
    ) {
        companion object {
            private const val PREFIX = "$SCHEME://"

            fun of(text: String): Link? {
                if (!text.startsWith(PREFIX, ignoreCase = true)) return null
                val rest = text.substring(PREFIX.length).substringBefore('#')
                val segments =
                    rest
                        .substringBefore('?')
                        .split('/')
                        .filter(String::isNotEmpty)
                        .map(::decode)
                val host = segments.firstOrNull()?.lowercase() ?: return null
                val query =
                    rest
                        .substringAfter('?', "")
                        .split('&')
                        .filter { '=' in it }
                        .associate { decode(it.substringBefore('=')) to decode(it.substringAfter('=')) }
                return Link(host, segments.drop(1), query)
            }

            private fun decode(part: String): String = URLDecoder.decode(part, "UTF-8")
        }
    }
}

/** Text selected in another app (`ACTION_PROCESS_TEXT`, T-1103): its first date opens that day, else the converter. */
internal object ProcessText {
    fun destination(
        text: String,
        context: ParseContext,
    ): AppDestination {
        val trimmed = text.trim().take(DeepLinks.MAX_TEXT_LENGTH)
        if (trimmed.isEmpty()) return AppDestination.Tools
        val day =
            TextDateDetector
                .detect(trimmed, context)
                .firstOrNull()
                ?.best
                ?.jdn
                ?.value
                ?.takeIf { it in DeepLinks.JDN_RANGE }
        return day?.let(AppDestination::Day) ?: AppDestination.Converter(trimmed)
    }
}
