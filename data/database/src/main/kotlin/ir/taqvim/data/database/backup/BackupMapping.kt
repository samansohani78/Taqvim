/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.database.backup

import ir.taqvim.core.ics.WeekdayNum
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.workdays.LeaveRange
import ir.taqvim.data.database.EventRecurrenceEntity
import ir.taqvim.data.database.IcsSubscriptionEntity
import ir.taqvim.data.database.PersonalData
import ir.taqvim.data.database.PersonalEventEntity
import ir.taqvim.data.database.ReminderEntity
import ir.taqvim.data.database.ShiftRotationEntity
import ir.taqvim.data.database.ShiftRotationRecordEntity
import ir.taqvim.data.database.WorkdayProfileEntity
import ir.taqvim.data.preferences.UserPreferences

internal fun UserPreferences.toRecord(): PreferencesRecord =
    PreferencesRecord(
        languageCode,
        calendars,
        numerals,
        weekStart,
        weekend,
        prayerMethod,
        asrJuristic,
        islamicVariant,
        themeMode,
        hijriOffsetDays,
        hijriOffsetSetAtEpochMillis,
    )

internal fun PreferencesRecord.toPreferences(): UserPreferences =
    UserPreferences(
        languageCode,
        calendars,
        numerals,
        weekStart,
        weekend,
        prayerMethod,
        asrJuristic,
        islamicVariant,
        themeMode,
        hijriOffsetDays,
        hijriOffsetSetAtEpochMillis,
    )

internal fun PersonalData.toRecord(): DataRecord =
    DataRecord(
        personalEvents = events.map { it.toRecord() },
        eventRecurrences = recurrences.map { it.toRecord() },
        reminders = reminders.map { ReminderRecord(it.id, it.eventId, it.minutesBefore, it.enabled) },
        shiftRotations =
            shiftRotations.map {
                ShiftRotationRecord(
                    it.id,
                    it.name,
                    it.anchorJdn,
                    it.pattern,
                    it.isActive,
                )
            },
        shiftRotationRecords = shiftRecords.map { ShiftDayRecord(it.rotationId, it.jdn, it.shift, it.note) },
        icsSubscriptions =
            icsSubscriptions.map {
                IcsSubscriptionRecord(
                    it.id,
                    it.url,
                    it.displayName,
                    it.colorArgb,
                    it.enabled,
                    it.refreshIntervalMinutes,
                )
            },
        workdayProfiles = workdayProfiles.map { it.toRecord() },
    )

/**
 * The rows of [this] document. Throws [IllegalArgumentException] when ids repeat, a row refers to a missing parent or a
 * value breaks a core type's invariant; the reader reports that as [BackupError.InvalidContent].
 */
internal fun DataRecord.toPersonalData(): PersonalData {
    requireConsistentKeys()
    return PersonalData(
        events = personalEvents.map { it.toEntity() },
        recurrences = eventRecurrences.map { it.toEntity() },
        reminders = reminders.map { ReminderEntity(it.id, it.eventId, it.minutesBefore, it.enabled) },
        shiftRotations =
            shiftRotations.map {
                ShiftRotationEntity(
                    it.id,
                    it.name,
                    it.anchorJdn,
                    it.pattern,
                    it.isActive,
                )
            },
        shiftRecords =
            shiftRotationRecords.map {
                ShiftRotationRecordEntity(
                    it.rotationId,
                    it.jdn,
                    it.shift,
                    it.note,
                )
            },
        icsSubscriptions =
            icsSubscriptions.map {
                IcsSubscriptionEntity(
                    id = it.id,
                    url = it.url,
                    displayName = it.displayName,
                    colorArgb = it.colorArgb,
                    enabled = it.enabled,
                    refreshIntervalMinutes = it.refreshIntervalMinutes,
                )
            },
        workdayProfiles = workdayProfiles.map { it.toEntity() },
    )
}

/** Unique primary keys per table and existing parents for every child row, as the database will enforce. */
private fun DataRecord.requireConsistentKeys() {
    val eventIds = uniqueIds("personal event", personalEvents.map { it.id })
    val rotationIds = uniqueIds("shift rotation", shiftRotations.map { it.id })
    uniqueIds("event recurrence", eventRecurrences.map { it.eventId })
    uniqueIds("reminder", reminders.map { it.id })
    uniqueIds("ICS subscription", icsSubscriptions.map { it.id })
    uniqueIds("workday profile", workdayProfiles.map { it.id })
    require(shiftRotationRecords.distinctBy { it.rotationId to it.jdn }.size == shiftRotationRecords.size) {
        "duplicate shift rotation record"
    }
    requireParents("event recurrence", eventRecurrences.map { it.eventId }, eventIds)
    requireParents("reminder", reminders.map { it.eventId }, eventIds)
    requireParents("shift rotation record", shiftRotationRecords.map { it.rotationId }, rotationIds)
}

private fun uniqueIds(
    table: String,
    ids: List<Long>,
): Set<Long> = ids.toSet().also { require(it.size == ids.size) { "duplicate $table id" } }

private fun requireParents(
    table: String,
    parentIds: List<Long>,
    existing: Set<Long>,
) {
    val missing = parentIds.firstOrNull { it !in existing }
    require(missing == null) { "$table refers to missing parent $missing" }
}

private fun PersonalEventEntity.toRecord() =
    EventRecord(
        id,
        title,
        notes,
        calendarSystem,
        startJdn,
        startMinute,
        endJdn,
        endMinute,
        timeZoneId,
        colorArgb,
        createdAtEpochMillis,
        updatedAtEpochMillis,
    )

private fun EventRecord.toEntity() =
    PersonalEventEntity(
        id,
        title,
        notes,
        calendarSystem,
        startJdn,
        startMinute,
        endJdn,
        endMinute,
        timeZoneId,
        colorArgb,
        createdAtEpochMillis,
        updatedAtEpochMillis,
    )

private fun EventRecurrenceEntity.toRecord() =
    RecurrenceRecord(
        eventId = eventId,
        frequency = frequency,
        interval = interval,
        count = count,
        untilJdn = untilJdn,
        byDay = byDay.map { WeekdayNumRecord(it.weekday, it.ordinal) },
        byMonthDay = byMonthDay,
        calendarSystem = calendarSystem,
        invalidDates = invalidDates,
        weekStart = weekStart,
    )

private fun RecurrenceRecord.toEntity() =
    EventRecurrenceEntity(
        eventId = eventId,
        frequency = frequency,
        interval = interval,
        count = count,
        untilJdn = untilJdn,
        byDay = byDay.map { WeekdayNum(it.weekday, it.ordinal) },
        byMonthDay = byMonthDay,
        calendarSystem = calendarSystem,
        invalidDates = invalidDates,
        weekStart = weekStart,
    )

private fun WorkdayProfileEntity.toRecord() =
    WorkdayProfileRecord(
        id = id,
        name = name,
        weekend = weekend,
        holidaySources = holidaySources,
        halfDays = halfDays,
        personalLeave = personalLeave.map { LeaveRangeRecord(it.first.value, it.last.value) },
        isDefault = isDefault,
    )

private fun WorkdayProfileRecord.toEntity() =
    WorkdayProfileEntity(
        id = id,
        name = name,
        weekend = weekend,
        holidaySources = holidaySources,
        halfDays = halfDays,
        personalLeave = personalLeave.map { LeaveRange(Jdn(it.firstJdn), Jdn(it.lastJdn)) },
        isDefault = isDefault,
    )
