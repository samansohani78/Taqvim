/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import android.content.Context
import io.kotest.matchers.shouldBe
import ir.taqvim.data.preferences.AppSettings
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify

/** T-1103 bindings: the automation module is complete, and board edits are stored within the board's limit. */
class AutomationModuleTest {
    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `automation bindings are complete`() {
        automationModule.verify(extraTypes = listOf(Context::class, UserPreferencesRepository::class))
    }

    @Test
    fun `the board keeps distinct zones up to its limit`(): Unit =
        runTest {
            val preferences = repositoryOf(UserPreferences.defaultsFor("fa"))
            val store = PreferencesToolsBoardStore(preferences)

            store.setBoardZones(listOf("Asia/Kabul", "Europe/Paris", "Asia/Kabul"))
            preferences.preferences
                .first()
                .app.timeZoneBoard shouldBe listOf("Asia/Kabul", "Europe/Paris")

            val many = (0 until AppSettings.MAX_BOARD_ZONES + 5).map { "Etc/GMT+${it % 12}-$it" }
            store.setBoardZones(many)
            preferences.preferences
                .first()
                .app.timeZoneBoard.size shouldBe AppSettings.MAX_BOARD_ZONES
        }
}
