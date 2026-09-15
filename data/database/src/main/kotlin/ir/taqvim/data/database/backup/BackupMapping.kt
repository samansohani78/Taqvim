/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.database.backup

import ir.taqvim.core.ics.WeekdayNum
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.workdays.LeaveRange
import ir.taqvim.data.database.EventRecurrenceEntity
import ir.taqvim.data.database.IcsSubscriptionEntity
import ir.taqvim.data.database.OfficialReminderEntity
import ir.taqvim.data.database.PersonalData
import ir.taqvim.data.database.PersonalEventEntity
import ir.taqvim.data.database.ReminderEntity
import ir.taqvim.data.database.ShiftRotationEntity
import ir.taqvim.data.database.ShiftRotationRecordEntity
import ir.taqvim.data.database.WorkdayProfileEntity
import ir.taqvim.data.preferences.AppSettings
import ir.taqvim.data.preferences.AthanAlert
import ir.taqvim.data.preferences.AthanPrayer
import ir.taqvim.data.preferences.AthanPreferences
import ir.taqvim.data.preferences.ChosenPlace
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.data.preferences.withEventSourcesFor

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
        place?.toRecord(),
        app.toRecord(),
        athan.toRecord(),
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
        place?.toPlace(),
        app = (app?.toAppSettings() ?: AppSettings.DEFAULT).withEventSourcesFor(languageCode),
        athan = athan?.toAthan() ?: AthanPreferences.DEFAULT,
    )

/**
 * These restored preferences with the device-only values of [current] kept: recent searches, level calibration, the
 * picked athan sound and whether the onboarding was finished (T-1501, ADR-0023) are never written to a backup, so a
 * restore must not erase them.
 */
internal fun UserPreferences.keepingDeviceOnlyValuesOf(current: UserPreferences): UserPreferences =
    copy(
        app = app.copy(recentSearches = current.app.recentSearches, levelOffsets = current.app.levelOffsets),
        athan = athan.copy(sound = current.athan.sound),
        onboardingCompleted = current.onboardingCompleted,
    )

private fun AthanPreferences.toRecord() =
    AthanRecord(
        alerts =
            AthanPrayer.entries.map { prayer ->
                val alert = alerts.getValue(prayer)
                AthanAlertRecord(prayer, alert.enabled, alert.gapMinutes)
            },
        vibrate = vibrate,
        bypassDndForFajr = bypassDndForFajr,
        volumePercent = volumePercent,
        useIranTime = useIranTime,
    )

/** The backed-up athan settings: missing prayers are off, gaps and volume are clamped, the sound is the default. */
private fun AthanRecord.toAthan(): AthanPreferences {
    val stored = alerts.associateBy { it.prayer }
    return AthanPreferences(
        alerts =
            AthanPrayer.entries.associateWith { prayer ->
                stored[prayer]?.let { AthanAlert(it.enabled, it.gapMinutes.coerceIn(AthanAlert.GAP_RANGE)) }
                    ?: AthanAlert.OFF
            },
        sound = null,
        vibrate = vibrate,
        bypassDndForFajr = bypassDndForFajr,
        volumePercent = volumePercent.coerceIn(AthanPreferences.VOLUME_RANGE),
        useIranTime = useIranTime,
    )
}

private fun AppSettings.toRecord() =
    AppSettingsRecord(
        dynamicColor = dynamicColor,
        highContrast = highContrast,
        boldText = boldText,
        gradient = gradient,
        showWeekNumbers = showWeekNumbers,
        enabledEventSources = enabledEventSources,
        highLatitudeRule = highLatitudeRule,
        subscriptionsNetworkAllowed = subscriptionsNetworkAllowed,
        persistentNotification = persistentNotification,
        rememberRecentSearches = rememberRecentSearches,
        timeZoneBoard = timeZoneBoard,
        allDayReminderMinute = allDayReminderMinute,
        eventSourcesChosen = eventSourcesChosen,
        persistentNotificationLargeNumber = persistentNotificationLargeNumber,
        dynamicLauncherIcon = dynamicLauncherIcon,
    )

/** The backed-up settings; values this version cannot use (e.g. personal events as a source) fall back to defaults. */
private fun AppSettingsRecord.toAppSettings(): AppSettings =
    runCatching {
        AppSettings.DEFAULT.copy(
            dynamicColor = dynamicColor,
            highContrast = highContrast,
            boldText = boldText,
            gradient = gradient,
            showWeekNumbers = showWeekNumbers,
            enabledEventSources = enabledEventSources,
            highLatitudeRule = highLatitudeRule,
            subscriptionsNetworkAllowed = subscriptionsNetworkAllowed,
            persistentNotification = persistentNotification,
            rememberRecentSearches = rememberRecentSearches,
            timeZoneBoard = timeZoneBoard,
            allDayReminderMinute =
                allDayReminderMinute.takeIf { it in AppSettings.ALL_DAY_REMINDER_MINUTES }
                    ?: AppSettings.DEFAULT_ALL_DAY_REMINDER_MINUTE,
            eventSourcesChosen = eventSourcesChosen,
            persistentNotificationLargeNumber = persistentNotificationLargeNumber,
            dynamicLauncherIcon = dynamicLauncherIcon,
        )
    }.getOrDefault(AppSettings.DEFAULT)

private fun ChosenPlace.toRecord() =
    PlaceRecord(source, cityId, name, coordinates.latitude, coordinates.longitude, zoneId)

/** The backed-up place, or `null` when it cannot be used on this device (e.g. its time zone is unknown here). */
private fun PlaceRecord.toPlace(): ChosenPlace? =
    runCatching { ChosenPlace(source, cityId, name, Coordinates(latitude, longitude), zoneId) }.getOrNull()

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
        officialReminders =
            officialReminders.map { OfficialReminderRecord(it.id, it.eventId, it.daysBefore, it.enabled) },
        eventExceptions = eventExceptions.map { it.toRecord() },
        eventOverrides = eventOverrides.map { it.toRecord() },
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
        officialReminders =
            officialReminders.map { OfficialReminderEntity(it.id, it.eventId, it.daysBefore, it.enabled) },
        eventExceptions = eventExceptions.map { it.toEntity() },
        eventOverrides = eventOverrides.map { it.toEntity() },
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
    requireValidOfficialReminders()
    requireValidEventInstances(eventIds)
}

/** Official reminders have unique ids, one row per event and lead time, an event id and a lead time of 0‥30 days. */
private fun DataRecord.requireValidOfficialReminders() {
    uniqueIds("official reminder", officialReminders.map { it.id })
    require(officialReminders.distinctBy { it.eventId to it.daysBefore }.size == officialReminders.size) {
        "duplicate official reminder"
    }
    require(
        officialReminders.all {
            it.eventId.isNotBlank() && it.daysBefore in 0..OfficialReminderEntity.MAX_DAYS_BEFORE
        },
    ) { "official reminder without an event or with a lead time outside 0..${OfficialReminderEntity.MAX_DAYS_BEFORE}" }
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
        icsUid,
        sourceLink,
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
        icsUid,
        sourceLink,
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
