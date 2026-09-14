/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
@file:Suppress("MatchingDeclarationName")

package ir.taqvim.app.di

import ir.taqvim.core.events.EventLookup
import ir.taqvim.core.workdays.WorkdayCalculator
import ir.taqvim.core.workdays.WorkdayProfile
import ir.taqvim.data.database.WorkdayProfileEntity
import ir.taqvim.data.database.toProfile
import ir.taqvim.data.events.generated.OfficialEvents
import ir.taqvim.data.preferences.UserPreferencesRepository
import ir.taqvim.feature.tools.ToolsSettings
import ir.taqvim.feature.tools.ToolsSettingsSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.datetime.TimeZone

/**
 * [ToolsSettingsSource] (T-1400) from the stored preferences and the default workday profile (T-504): the app language,
 * the device's time zone, the user's computable calendars (the tools' defaults when none is) and workday arithmetic
 * over the official events when a default profile exists. The time-zone board starts empty until T-1500 stores it.
 */
internal class PreferencesToolsSettingsSource(
    private val preferences: UserPreferencesRepository,
    private val workdayProfile: Flow<WorkdayProfile?>,
    private val zone: () -> TimeZone = { TimeZone.currentSystemDefault() },
    loadLookup: () -> EventLookup = { EventLookup(OfficialEvents.ALL) },
) : ToolsSettingsSource {
    private val lookup by lazy(loadLookup)

    override fun settings(): Flow<ToolsSettings> =
        combine(preferences.preferences, workdayProfile.distinctUntilChanged()) { preferences, profile ->
            ToolsSettings(
                language = preferences.languageSpec(),
                homeZone = zone(),
                calendars = preferences.availableCalendars().ifEmpty { ToolsSettings.DEFAULT_CALENDARS },
                boardZones = preferences.app.timeZoneBoard,
                workdays = profile?.let { WorkdayCalculator(lookup, it) },
            )
        }
}

/** The stored profile marked as default, as a [WorkdayProfile]; `null` when none is. */
internal fun List<WorkdayProfileEntity>.defaultProfile(): WorkdayProfile? = firstOrNull { it.isDefault }?.toProfile()
