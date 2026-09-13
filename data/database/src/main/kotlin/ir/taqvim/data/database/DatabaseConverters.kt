/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.database

import androidx.room.TypeConverter
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.ics.WeekdayNum
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.workdays.LeaveRange

private const val SEPARATOR = ","
private const val ORDINAL = "@"
private const val RANGE = ".."

/** Terminates every shift label, so empty labels survive; labels must not contain it. */
private const val LABEL_TERMINATOR = '\u001F'

private fun String.entries(): List<String> = if (isEmpty()) emptyList() else split(SEPARATOR)

/** Column encodings of enum sets and plain lists. Single enums are stored by name by Room itself. */
class CollectionConverters {
    @TypeConverter
    fun weekdaysToColumn(days: Set<Weekday>): String = days.sortedBy { it.ordinal }.joinToString(SEPARATOR) { it.name }

    @TypeConverter
    fun columnToWeekdays(column: String): Set<Weekday> = column.entries().map(Weekday::valueOf).toSet()

    @TypeConverter
    fun sourcesToColumn(sources: Set<EventSource>): String =
        sources.sortedBy { it.ordinal }.joinToString(SEPARATOR) { it.name }

    @TypeConverter
    fun columnToSources(column: String): Set<EventSource> = column.entries().map(EventSource::valueOf).toSet()

    @TypeConverter
    fun intsToColumn(values: List<Int>): String = values.joinToString(SEPARATOR)

    @TypeConverter
    fun columnToInts(column: String): List<Int> = column.entries().map(String::toInt)

    @TypeConverter
    fun labelsToColumn(labels: List<String>): String {
        require(labels.none { LABEL_TERMINATOR in it }) { "shift labels must not contain U+001F" }
        return labels.joinToString("") { it + LABEL_TERMINATOR }
    }

    @TypeConverter
    fun columnToLabels(column: String): List<String> = column.split(LABEL_TERMINATOR).dropLast(1)
}

/**
 * Column encodings of calendar value types: BYDAY entries (`FRIDAY`, `-1@MONDAY`) and leave ranges (`first..last`).
 */
class CalendarConverters {
    @TypeConverter
    fun weekdayNumsToColumn(days: List<WeekdayNum>): String =
        days.joinToString(SEPARATOR) { day ->
            day.ordinal?.let { "$it$ORDINAL${day.weekday.name}" } ?: day.weekday.name
        }

    @TypeConverter
    fun columnToWeekdayNums(column: String): List<WeekdayNum> =
        column.entries().map { entry ->
            WeekdayNum(
                weekday = Weekday.valueOf(entry.substringAfter(ORDINAL)),
                ordinal = entry.substringBefore(ORDINAL, missingDelimiterValue = "").toIntOrNull(),
            )
        }

    @TypeConverter
    fun leaveToColumn(leave: List<LeaveRange>): String =
        leave.joinToString(SEPARATOR) { "${it.first.value}$RANGE${it.last.value}" }

    @TypeConverter
    fun columnToLeave(column: String): List<LeaveRange> =
        column.entries().map {
            LeaveRange(Jdn(it.substringBefore(RANGE).toLong()), Jdn(it.substringAfter(RANGE).toLong()))
        }
}
