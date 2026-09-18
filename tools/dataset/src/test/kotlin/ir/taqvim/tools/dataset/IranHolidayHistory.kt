/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.tools.dataset

import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.IranIslamicCalendar
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import java.io.File
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/** A calendar date as year, month and day, in whichever calendar the context says. */
internal data class Ymd(
    val year: Int,
    val month: Int,
    val day: Int,
) {
    val yearMonth: String get() = "%04d-%02d".format(year, month)

    override fun toString(): String = "%04d-%02d-%02d".format(year, month, day)

    companion object {
        fun parse(iso: String): Ymd {
            val (year, month, day) = iso.split('-').map(String::toInt)
            return Ymd(year, month, day)
        }
    }
}

/** One printed day of an official calendar (`golden/persian/official/<year>.csv`). */
internal data class OfficialDay(
    val persian: Ymd,
    val printedHijri: Ymd,
    val jdn: Long,
    val holiday: Boolean,
) {
    companion object {
        private const val PERSIAN = 0
        private const val HIJRI = 2
        private const val GREGORIAN = 3
        private const val HOLIDAY = 4

        fun readYear(file: File): List<OfficialDay> =
            file
                .readLines()
                .filterNot { it.startsWith("#") || it.isBlank() }
                .drop(1)
                .map { line ->
                    val columns = line.split(',')
                    val gregorian = Ymd.parse(columns[GREGORIAN])
                    val gregorianDate =
                        CalendarDate(CalendarSystem.GREGORIAN, gregorian.year, gregorian.month, gregorian.day)
                    OfficialDay(
                        persian = Ymd.parse(columns[PERSIAN]),
                        printedHijri = Ymd.parse(columns[HIJRI]),
                        jdn = GregorianCalendarSystem.toJdn(gregorianDate).value,
                        holiday = columns[HOLIDAY] == "true",
                    )
                }
    }
}

/**
 * The Iran holiday records of `dataset/iran/iran-official-holidays.json`, evaluated the way the app evaluates them:
 * a Persian or lunar Hijri rule, kept only inside the record's `validity`, whose year is read in the validity's
 * calendar. [hijri] supplies the lunar date of a day, so the same records can be read against the printed calendar
 * or against the computed one.
 */
internal class IranHolidayRules(
    private val events: List<JsonObject>,
) {
    /** Holiday record ids per Persian date, and the records a `validity` range left out (law changes). */
    fun evaluate(
        days: List<OfficialDay>,
        hijri: (Long) -> Ymd,
    ): Evaluation {
        val holidays = sortedMapOf<String, MutableList<String>>()
        val excluded = sortedMapOf<String, MutableList<String>>()
        days.forEach { day ->
            val lunar = hijri(day.jdn)
            val nextMonth = hijri(day.jdn + 1).month
            events
                .filter { event -> matches(event, day.persian, lunar, nextMonth) }
                .forEach { event ->
                    val target = if (valid(event, day.persian, lunar)) holidays else excluded
                    target.getOrPut(day.persian.toString(), ::mutableListOf) += event.id
                }
        }
        return Evaluation(holidays, excluded)
    }

    private fun matches(
        event: JsonObject,
        persian: Ymd,
        hijri: Ymd,
        nextMonth: Int,
    ): Boolean {
        val rule = event["rule"] as? JsonObject ?: return false
        val date = if (event.text("calendar") == "PERSIAN") persian else hijri
        return when (rule.text("type")) {
            "Fixed" -> date.month == rule.number("month") && date.day == rule.number("day")
            "LastDayOfMonth" -> date.month == rule.number("month") && nextMonth != date.month
            else -> error("rule type ${rule.text("type")} is not used by the Iran holiday records")
        }
    }

    private fun valid(
        event: JsonObject,
        persian: Ymd,
        hijri: Ymd,
    ): Boolean {
        val validity = event["validity"] as? JsonObject ?: return true
        val year = if (validity.text("calendar") == "PERSIAN") persian.year else hijri.year
        return (validity.number("fromYear") ?: Int.MIN_VALUE) <= year &&
            year <= (validity.number("toYear") ?: Int.MAX_VALUE)
    }

    data class Evaluation(
        val holidays: Map<String, List<String>>,
        val excluded: Map<String, List<String>>,
    )

    companion object {
        val JsonObject.id: String get() = text("id").orEmpty()

        fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

        fun JsonObject.number(key: String): Int? = (this[key] as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull
    }
}

/** How a difference between the computed calendar's holidays and an official calendar's holidays comes about. */
internal enum class HolidayDifference(
    val label: String,
) {
    /** The official calendar was printed before an announcement moved the month (see its notice page). */
    ANNOUNCED_SHIFT("announced shift"),

    /** The computed Iranian lunar month starts on another day than the official one. */
    LUNAR_CALENDAR("lunar-calendar difference"),

    /** Neither: the rule itself is wrong. The history test allows none. */
    RULE_ERROR("rule error"),
}

/** One date on which the computed calendar and an official calendar disagree about a holiday. */
internal data class ComputedDifference(
    val persian: String,
    val record: String,
    val official: Boolean,
    val printedHijri: Ymd,
    val computedHijri: Ymd,
    val kind: HolidayDifference,
)

/** Compares the holidays of the computed Iranian lunar calendar with the official ones and explains each difference. */
internal class ComputedCalendarComparison(
    private val rules: IranHolidayRules,
    private val announcedMonths: Set<String>,
) {
    private val calendar = IranIslamicCalendar()

    fun hijri(jdn: Long): Ymd = calendar.fromJdn(Jdn(jdn)).let { Ymd(it.year, it.month, it.day) }

    /** The Persian date on which the computed calendar starts lunar month [month] of [year]. */
    fun persianStart(
        year: Int,
        month: Int,
    ): Ymd {
        val jdn = calendar.toJdn(CalendarDate(CalendarSystem.ISLAMIC, year, month, 1))
        return PersianCalendarSystem.fromJdn(jdn).let { Ymd(it.year, it.month, it.day) }
    }

    /** Differences on [days]; [printed] names the records behind each official holiday (the printed reading). */
    fun differences(
        days: List<OfficialDay>,
        printed: Map<String, List<String>>,
    ): List<ComputedDifference> {
        val computed = rules.evaluate(days, ::hijri).holidays
        val official = days.filter { it.holiday }.map { it.persian.toString() }.toSet()
        val byDate = days.associateBy { it.persian.toString() }
        val missing = (official - computed.keys).map { it to printed[it].orEmpty().ifEmpty { listOf(OFFICIAL_ONLY) } }
        val extra = (computed.keys - official).map { it to computed.getValue(it) }
        return (missing + extra).sortedBy { it.first }.flatMap { (date, records) ->
            val day = byDate.getValue(date)
            records.map { record -> difference(day, record, official = date in official, days) }
        }
    }

    private fun difference(
        day: OfficialDay,
        record: String,
        official: Boolean,
        days: List<OfficialDay>,
    ): ComputedDifference {
        val computed = hijri(day.jdn)
        val next = days.firstOrNull { it.jdn == day.jdn + 1 }
        val lunarDiffers =
            computed != day.printedHijri || (next != null && hijri(next.jdn).month != next.printedHijri.month)
        val announced = computed.yearMonth in announcedMonths || day.printedHijri.yearMonth in announcedMonths
        val kind =
            when {
                announced -> HolidayDifference.ANNOUNCED_SHIFT
                lunarDiffers -> HolidayDifference.LUNAR_CALENDAR
                else -> HolidayDifference.RULE_ERROR
            }
        return ComputedDifference(
            persian = day.persian.toString(),
            record = record,
            official = official,
            printedHijri = day.printedHijri,
            computedHijri = computed,
            kind = kind,
        )
    }

    companion object {
        /** Marks an official holiday that the computed calendar does not produce on that day. */
        const val OFFICIAL_ONLY = "(official holiday)"
    }
}
