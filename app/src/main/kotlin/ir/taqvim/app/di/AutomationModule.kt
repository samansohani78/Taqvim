/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
@file:Suppress("MatchingDeclarationName")

package ir.taqvim.app.di

import ir.taqvim.app.automation.BroadcastAthanEventHook
import ir.taqvim.data.preferences.AppSettings
import ir.taqvim.data.preferences.UserPreferencesRepository
import ir.taqvim.feature.notification.AthanEventHook
import ir.taqvim.feature.tools.ToolsBoardStore
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Automation (T-1103, ADR-0016): athans announced to automation apps, and the time-zone board kept by the tools
 * (T-1400 over T-1500). Without these bindings the athan hook and the board store fall back to doing nothing.
 */
val automationModule: Module =
    module {
        single<AthanEventHook> { BroadcastAthanEventHook(androidContext()) }
        single<ToolsBoardStore> { PreferencesToolsBoardStore(get()) }
    }

/** [ToolsBoardStore] over the stored time-zone board: distinct ids, at most [AppSettings.MAX_BOARD_ZONES]. */
internal class PreferencesToolsBoardStore(
    private val preferences: UserPreferencesRepository,
) : ToolsBoardStore {
    override suspend fun setBoardZones(zones: List<String>) {
        val board = zones.distinct().take(AppSettings.MAX_BOARD_ZONES)
        preferences.update { current -> current.copy(app = current.app.copy(timeZoneBoard = board)) }
    }
}
