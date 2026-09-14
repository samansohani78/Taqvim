/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
@file:Suppress("MatchingDeclarationName")

package ir.taqvim.app.di

import ir.taqvim.core.astronomy.ZodiacSystem
import ir.taqvim.feature.astronomy.AstronomySettings
import ir.taqvim.feature.astronomy.AstronomySettingsSource
import ir.taqvim.feature.times.TimesSettings
import ir.taqvim.feature.times.TimesSettingsSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * [AstronomySettingsSource] (T-1300) from the Times tab's settings (T-1100): the same chosen city, language and first
 * available calendar. "The Moon in Scorpio" uses IAU constellation boundaries until a preference exists (DT-013).
 */
internal class TimesAstronomySettingsSource(
    private val times: TimesSettingsSource,
) : AstronomySettingsSource {
    override fun settings(): Flow<AstronomySettings?> =
        times
            .settings()
            .map { it?.toAstronomySettings() }
            .distinctUntilChanged()
}

/** These Times settings as the Astronomy screen's settings. */
internal fun TimesSettings.toAstronomySettings(): AstronomySettings =
    AstronomySettings(
        placeName = placeName,
        place = place,
        timeZone = timeZone,
        language = language,
        calendar = calendar,
        scorpioSystem = ZodiacSystem.IAU_CONSTELLATION,
    )
