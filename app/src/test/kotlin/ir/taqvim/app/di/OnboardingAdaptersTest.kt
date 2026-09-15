/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.data.preferences.UserPreferencesRepository
import ir.taqvim.feature.settings.ThemeChoice
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/** T-1501 wiring: completing the onboarding, language changes from the settings and the app language sync. */
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingAdaptersTest {
    private suspend fun UserPreferencesRepository.current(): UserPreferences = preferences.first()

    @Test
    fun `completing the onboarding is stored`(): Unit =
        runTest {
            val repository = repositoryOf(UserPreferences.defaultsFor("fa"))

            PreferencesOnboardingStore(repository).complete()

            repository.current() shouldBe UserPreferences.defaultsFor("fa").copy(onboardingCompleted = true)
        }

    @Test
    fun `a language chosen in the settings applies its defaults while other changes are stored as they are`(): Unit =
        runTest {
            val repository = repositoryOf(UserPreferences.defaultsFor("fa"))
            val store = PreferencesGeneralSettingsStore(repository)

            store.update { it.copy(languageCode = "ne") }
            repository.current() shouldBe UserPreferences.defaultsFor("ne")

            store.update { it.copy(theme = ThemeChoice.DARK) }
            repository.current().themeMode.name shouldBe ThemeChoice.DARK.name
            repository.current().languageCode shouldBe "ne"
        }

    @Test
    fun `the language sync applies only what the platform does not already show`() {
        LanguageSync.toApply("fa", null, "fa-IR") shouldBe null
        LanguageSync.toApply("prs", null, "fa-AF") shouldBe null
        LanguageSync.toApply("en", null, "fa-IR") shouldBe "en-US"
        LanguageSync.toApply("fa", "en-US", "fa-IR") shouldBe "fa-IR"
        LanguageSync.toApply("kmr", null, "de-DE") shouldBe "ku-TR"
        LanguageSync.toApply("en", null, "pt-BR") shouldBe null
        LanguageTable.languages.forEach { spec ->
            withClue(spec.code) { LanguageSync.toApply(spec.code, spec.localeTag, "en-US") shouldBe null }
        }

        LanguageSync.adopted("fa", null) shouldBe null
        LanguageSync.adopted("fa", "fa-IR") shouldBe null
        LanguageSync.adopted("fa", "de-DE") shouldBe "de"
        LanguageSync.adopted("fa", "fa-AF") shouldBe "prs"
        LanguageSync.adopted("fa", "pt-BR") shouldBe null
    }

    @Test
    fun `the sync adopts a language set outside the app and then applies stored changes`(): Unit =
        runTest {
            val repository = repositoryOf(UserPreferences.defaultsFor("fa"))
            val locales = FakeAppLocales(current = "de-DE")
            LanguageSync.adopted("fa", locales.current()) shouldBe "de"
            // A foreground job, so a failure of the sync fails the test here rather than after the assertions.
            val sync = launch { AppLanguageSync(repository, locales) { "fa-IR" }.run() }
            advanceUntilIdle()

            repository.current() shouldBe UserPreferences.defaultsFor("de")
            locales.applied shouldBe emptyList()

            repository.update { it.withLanguage("fa") }
            advanceUntilIdle()
            locales.applied shouldBe listOf("fa-IR")

            repository.update { it.copy(onboardingCompleted = true) }
            advanceUntilIdle()
            locales.applied shouldBe listOf("fa-IR")
            sync.cancel()
        }

    @Test
    fun `a fresh install that already shows the device language changes nothing`(): Unit =
        runTest {
            val repository = repositoryOf(UserPreferences.defaultsFor("prs"))
            val locales = FakeAppLocales(current = null)
            backgroundScope.launch { AppLanguageSync(repository, locales) { "fa-AF" }.run() }
            advanceUntilIdle()

            repository.current() shouldBe UserPreferences.defaultsFor("prs")
            locales.applied shouldBe emptyList()
        }
}

/** Records applied tags; [current] follows the last one. */
internal class FakeAppLocales(
    var current: String?,
) : AppLocales {
    val applied = mutableListOf<String>()

    override fun current(): String? = current

    override fun apply(tag: String) {
        applied += tag
        current = tag
    }
}
