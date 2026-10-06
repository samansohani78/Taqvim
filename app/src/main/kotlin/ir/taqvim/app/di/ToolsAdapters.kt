/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
@file:Suppress("MatchingDeclarationName")

package ir.taqvim.app.di

import ir.taqvim.core.events.EventLookup
import ir.taqvim.core.nlp.AnchorLookup
import ir.taqvim.core.workdays.WorkdayCalculator
import ir.taqvim.core.workdays.WorkdayProfile
import ir.taqvim.data.database.WorkdayProfileDao
import ir.taqvim.data.database.WorkdayProfileEntity
import ir.taqvim.data.database.toProfile
import ir.taqvim.data.devicecalendar.DeviceTimeZone
import ir.taqvim.data.events.SkyAstronomicalEventSource
import ir.taqvim.data.events.generated.OfficialEvents
import ir.taqvim.data.preferences.UserPreferencesRepository
import ir.taqvim.feature.tools.NamedWorkdayProfile
import ir.taqvim.feature.tools.ToolsSettings
import ir.taqvim.feature.tools.ToolsSettingsSource
import ir.taqvim.feature.tools.WorkdayProfileStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone

/**
 * [ToolsSettingsSource] (T-1400) from the stored preferences and the default workday profile (T-504): the app language,
 * the device's time zone (re-emitted when it changes, review I06), the user's computable calendars (the tools' defaults when none is) and workday arithmetic
 * over the official events when a default profile exists. The time-zone board starts empty until T-1500 stores it.
 */
internal class PreferencesToolsSettingsSource(
    private val preferences: UserPreferencesRepository,
    private val workdayProfile: Flow<WorkdayProfile?>,
    private val zones: Flow<TimeZone> = DeviceTimeZone.current,
    loadLookup: () -> EventLookup = { EventLookup(OfficialEvents.ALL, astronomy = SkyAstronomicalEventSource) },
    private val anchors: AnchorLookup? = null,
) : ToolsSettingsSource {
    private val lookup by lazy(loadLookup)

    override fun settings(): Flow<ToolsSettings> =
        combine(
            preferences.preferences,
            workdayProfile.distinctUntilChanged(),
            zones.distinctUntilChanged(),
        ) { preferences, profile, zone ->
            ToolsSettings(
                language = preferences.languageSpec(),
                homeZone = zone,
                calendars = preferences.availableCalendars().ifEmpty { ToolsSettings.DEFAULT_CALENDARS },
                boardZones = preferences.app.timeZoneBoard,
                workdays = profile?.let { WorkdayCalculator(lookup, it) },
                anchors = anchors,
            )
        }
}

/** The stored profile marked as default, as a [WorkdayProfile]; `null` when none is. */
internal fun List<WorkdayProfileEntity>.defaultProfile(): WorkdayProfile? = firstOrNull { it.isDefault }?.toProfile()

/**
 * [WorkdayProfileStore] (F-07) over `workday_profiles`.
 *
 * The table, its DAO and its backup mapping have existed since T-504; nothing wrote to them, so `defaultProfile()`
 * was `null` for every user and the workday calculator fell back to the language's weekend. This is the writer.
 */
internal class RoomWorkdayProfileStore(
    private val dao: WorkdayProfileDao,
) : WorkdayProfileStore {
    override fun profiles(): Flow<List<NamedWorkdayProfile>> =
        dao.observeAll().map { stored ->
            stored.map { NamedWorkdayProfile(it.id, it.name, it.toProfile(), it.isDefault) }
        }

    override suspend fun save(
        id: Long?,
        name: String,
        profile: WorkdayProfile,
    ): Long {
        val entity = profile.toEntity(id ?: 0L, name, isDefault = false)
        if (id == null) {
            val newId = dao.insert(entity)
            // The first profile a user makes is the one the calculator should use; asking them to press "make
            // default" immediately afterwards would be a step with only one possible answer.
            if (dao.observeAll().first().size == 1) dao.makeDefault(newId)
            return newId
        }
        val wasDefault =
            dao
                .observeAll()
                .first()
                .firstOrNull { it.id == id }
                ?.isDefault ?: false
        dao.update(entity.copy(isDefault = wasDefault))
        return id
    }

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun makeDefault(id: Long) = dao.makeDefault(id)
}

/** The profile as a row of `workday_profiles`. */
private fun WorkdayProfile.toEntity(
    id: Long,
    name: String,
    isDefault: Boolean,
): WorkdayProfileEntity =
    WorkdayProfileEntity(
        id = id,
        name = name,
        weekend = weekend,
        holidaySources = holidaySources,
        halfDays = halfDays,
        personalLeave = personalLeave,
        isDefault = isDefault,
    )
