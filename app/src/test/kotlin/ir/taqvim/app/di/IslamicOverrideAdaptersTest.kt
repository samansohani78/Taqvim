/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.IslamicMonthOverrides
import ir.taqvim.core.calendar.TodayProvider
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.data.preferences.IslamicOverrideOrigin
import ir.taqvim.data.preferences.IslamicOverrideSetting
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.data.preferences.UserPreferencesRepository
import ir.taqvim.feature.settings.IslamicOverrideKind
import ir.taqvim.feature.settings.OverrideImportResult
import java.io.ByteArrayInputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/** ADR-0037 wiring: the optional official Islamic dates are switched, imported, removed and described. */
class IslamicOverrideAdaptersTest {
    private val official = requireNotNull(IslamicMonthOverrides.bundledIranOfficialText())
    private val before = Jdn(2_460_700)
    private val after = Jdn(2_470_000)

    private fun store(
        files: Map<String, OverrideFileText> = emptyMap(),
        today: Jdn = before,
        preferences: UserPreferences = UserPreferences.defaultsFor("en"),
    ): Pair<PreferencesIslamicOverrideStore, UserPreferencesRepository> {
        val repository = repositoryOf(preferences)
        val reader = OverrideFileReader { files[it] ?: OverrideFileText.Unreadable }
        return PreferencesIslamicOverrideStore(repository, reader, TodayProvider { today }) to repository
    }

    @Test
    fun `computed dates are the default and the bundled official dates can be switched on and off`(): Unit =
        runTest {
            val (store, repository) = store()
            store.status().first().kind shouldBe IslamicOverrideKind.NONE
            repository.preferences.first().islamicOverride shouldBe IslamicOverrideSetting.NONE

            store.setOfficial(true)
            val status = store.status().first()
            status.kind shouldBe IslamicOverrideKind.OFFICIAL
            status.firstMonth shouldBe CalendarDate(CalendarSystem.ISLAMIC, 1446, 9, 1)
            status.lastMonth shouldBe CalendarDate(CalendarSystem.ISLAMIC, 1448, 9, 1)
            status.broken shouldBe false
            status.ended shouldBe false
            repository.preferences
                .first()
                .islamicOverride.origin shouldBe IslamicOverrideOrigin.OFFICIAL_BUNDLED

            store.setOfficial(false)
            store.status().first().kind shouldBe IslamicOverrideKind.NONE
        }

    @Test
    fun `switching the official dates off keeps an imported file`(): Unit =
        runTest {
            val imported = IslamicOverrideSetting(IslamicOverrideOrigin.IMPORTED, official)
            val (store, repository) =
                store(
                    preferences = UserPreferences.defaultsFor("en").copy(islamicOverride = imported),
                )
            store.setOfficial(false)
            repository.preferences.first().islamicOverride shouldBe imported
        }

    @Test
    fun `a missing bundled file leaves the dates computed`(): Unit =
        runTest {
            val repository = repositoryOf(UserPreferences.defaultsFor("en"))
            val store =
                PreferencesIslamicOverrideStore(repository, { OverrideFileText.Unreadable }, { before }) { null }
            store.setOfficial(true)
            store.status().first().kind shouldBe IslamicOverrideKind.NONE
        }

    @Test
    fun `imports store valid files and refuse the rest with their reason`(): Unit =
        runTest {
            val files =
                mapOf(
                    "good" to OverrideFileText.Read(official),
                    "big" to OverrideFileText.TooLarge,
                    "text" to OverrideFileText.Read("not json"),
                    "old" to OverrideFileText.Read("""{"schemaVersion":2,"months":[]}"""),
                    "uncited" to OverrideFileText.Read(official.replace("\"url\"", "\"link\"")),
                )
            val (store, repository) = store(files)
            store.import("missing") shouldBe OverrideImportResult.UNREADABLE
            store.import("big") shouldBe OverrideImportResult.TOO_LARGE
            store.import("text") shouldBe OverrideImportResult.NOT_JSON
            store.import("old") shouldBe OverrideImportResult.UNSUPPORTED_VERSION
            store.import("uncited") shouldBe OverrideImportResult.MISSING_CITATION
            store.status().first().kind shouldBe IslamicOverrideKind.NONE

            store.import("good") shouldBe OverrideImportResult.IMPORTED
            store.status().first().kind shouldBe IslamicOverrideKind.IMPORTED
            repository.preferences
                .first()
                .islamicOverride.json shouldBe official

            store.remove()
            store.status().first().kind shouldBe IslamicOverrideKind.NONE
        }

    @Test
    fun `broken and ended overrides are reported`(): Unit =
        runTest {
            val broken = IslamicOverrideSetting(IslamicOverrideOrigin.IMPORTED, "{")
            val (brokenStore, _) = store(preferences = UserPreferences.defaultsFor("fa").copy(islamicOverride = broken))
            val status = brokenStore.status().first()
            status.broken shouldBe true
            status.firstMonth shouldBe null
            status.language.code shouldBe "fa"

            val officialSetting = IslamicOverrideSetting(IslamicOverrideOrigin.OFFICIAL_BUNDLED, official)
            val (ended, _) =
                store(
                    today = after,
                    preferences = UserPreferences.defaultsFor("en").copy(islamicOverride = officialSetting),
                )
            ended.status().first().ended shouldBe true
        }

    @Test
    fun `at most the limit is read from a stream`() {
        val bytes = ByteArray(OVERRIDE_MAX_BYTES + 100) { 1 }
        ByteArrayInputStream(bytes).readAtMost(OVERRIDE_MAX_BYTES + 1).size shouldBe OVERRIDE_MAX_BYTES + 1
        ByteArrayInputStream(byteArrayOf(1, 2, 3)).readAtMost(10).toList() shouldBe listOf<Byte>(1, 2, 3)
        ByteArrayInputStream(ByteArray(0)).readAtMost(10).size shouldBe 0
    }
}
