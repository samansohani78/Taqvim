/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.database.backup

import ir.taqvim.data.database.EventExceptionEntity
import ir.taqvim.data.database.EventOverrideEntity

private const val MINUTES_PER_DAY = 1_440

internal fun EventExceptionEntity.toRecord(): EventExceptionRecord = EventExceptionRecord(eventId, dayJdn)

internal fun EventExceptionRecord.toEntity(): EventExceptionEntity = EventExceptionEntity(eventId, dayJdn)

internal fun EventOverrideEntity.toRecord(): EventOverrideRecord =
    EventOverrideRecord(
        eventId = eventId,
        originalJdn = originalJdn,
        title = title,
        notes = notes,
        startJdn = startJdn,
        startMinute = startMinute,
        endJdn = endJdn,
        endMinute = endMinute,
        colorArgb = colorArgb,
        cancelled = cancelled,
    )

internal fun EventOverrideRecord.toEntity(): EventOverrideEntity =
    EventOverrideEntity(
        eventId = eventId,
        originalJdn = originalJdn,
        title = title,
        notes = notes,
        startJdn = startJdn,
        startMinute = startMinute,
        endJdn = endJdn,
        endMinute = endMinute,
        colorArgb = colorArgb,
        cancelled = cancelled,
    )

/**
 * Exception days and overridden occurrences (T-1003) belong to an event in [eventIds], appear once per event and day,
 * and overrides end on or after their start with times inside the day (both times absent for all-day instances).
 */
internal fun DataRecord.requireValidEventInstances(eventIds: Set<Long>) {
    require(eventExceptions.distinct().size == eventExceptions.size) { "duplicate event exception" }
    require(eventOverrides.distinctBy { it.eventId to it.originalJdn }.size == eventOverrides.size) {
        "duplicate event override"
    }
    val parents = eventExceptions.map { it.eventId } + eventOverrides.map { it.eventId }
    val orphan = parents.firstOrNull { it !in eventIds }
    require(orphan == null) { "event exception or override refers to missing parent $orphan" }
    require(eventOverrides.all { it.hasValidSpan() }) { "event override with an invalid start or end" }
}

private fun EventOverrideRecord.hasValidSpan(): Boolean {
    val timesInDay = listOfNotNull(startMinute, endMinute).all { it in 0 until MINUTES_PER_DAY }
    val allDayOrTimed = startMinute != null || endMinute == null
    val ordered =
        endJdn > startJdn || (endJdn == startJdn && (endMinute ?: startMinute ?: 0) >= (startMinute ?: 0))
    return timesInDay && allDayOrTimed && ordered
}
