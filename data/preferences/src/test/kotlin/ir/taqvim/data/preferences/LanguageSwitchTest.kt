/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.preferences

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.checkAll
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.i18n.NumeralSystem
import ir.taqvim.core.model.AsrJuristic
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.PrayerMethod
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.testing.PropertyTesting
import ir.taqvim.data.preferences.proto.UserPrefs
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** T-1501: switching the language applies its defaults to every value the user did not change (ADR-0023). */
class LanguageSwitchTest {
    private val codes = LanguageTable.languages.map { it.code }
    private val language = Arb.element(codes)

    @Test
    fun `switching from one language's defaults gives exactly the other language's defaults`() {
        codes.forEach { from ->
            codes.forEach { to ->
                withClue("$from -> $to") {
                    UserPreferences.defaultsFor(from).withLanguage(to) shouldBe UserPreferences.defaultsFor(to)
                }
            }
        }
    }

    @Test
    fun `values the user changed are kept and the rest follow the new language`(): Unit =
        runBlocking {
            checkAll(
                PropertyTesting.iterations,
                language,
                language,
                Arb.element(NumeralSystem.entries),
            ) { from, to, n ->
                val defaults = UserPreferences.defaultsFor(from)
                val chosen =
                    defaults.copy(
                        numerals = n,
                        prayerMethod = PrayerMethod.MAKKAH,
                        themeMode = ThemeMode.DARK,
                        place =
                            ChosenPlace(
                                PlaceSource.COORDINATES,
                                null,
                                "Tehran",
                                TEHRAN,
                                "Asia/Tehran",
                            ),
                        onboardingCompleted = true,
                    )
                val switched = chosen.withLanguage(to)
                val target = UserPreferences.defaultsFor(to)

                switched.languageCode shouldBe to
                switched.numerals shouldBe (if (n == defaults.numerals) target.numerals else n)
                switched.prayerMethod shouldBe
                    (if (defaults.prayerMethod == PrayerMethod.MAKKAH) target.prayerMethod else PrayerMethod.MAKKAH)
                switched.calendars shouldBe target.calendars
                switched.weekStart shouldBe target.weekStart
                switched.weekend shouldBe target.weekend
                switched.asrJuristic shouldBe target.asrJuristic
                switched.islamicVariant shouldBe target.islamicVariant
                switched.app.enabledEventSources shouldBe target.app.enabledEventSources
                switched.themeMode shouldBe ThemeMode.DARK
                switched.place shouldBe chosen.place
                switched.onboardingCompleted shouldBe true
            }
        }

    @Test
    fun `chosen event sources, calendars and weekend survive a language change`() {
        val persian = UserPreferences.defaultsFor("fa")
        val chosen =
            persian.copy(
                calendars = listOf(CalendarSystem.GREGORIAN, CalendarSystem.PERSIAN),
                // Differs from both the fa (Friday) and en (Saturday, Sunday) defaults: a value equal to the other
                // language's default would count as not chosen after the round trip (ADR-0023).
                weekend = setOf(Weekday.THURSDAY, Weekday.FRIDAY),
                weekStart = Weekday.MONDAY,
                asrJuristic = AsrJuristic.HANAFI,
                islamicVariant = IslamicVariant.TABULAR_16,
                app =
                    persian.app.copy(
                        enabledEventSources = setOf(EventSource.ANCIENT_IRAN),
                        eventSourcesChosen = true,
                    ),
            )

        val english = chosen.withLanguage("en")

        english.calendars shouldBe chosen.calendars
        english.weekend shouldBe chosen.weekend
        english.weekStart shouldBe chosen.weekStart
        english.asrJuristic shouldBe AsrJuristic.HANAFI
        english.islamicVariant shouldBe IslamicVariant.TABULAR_16
        english.app.enabledEventSources shouldBe setOf(EventSource.ANCIENT_IRAN)
        english.withLanguage("fa") shouldBe chosen
        persian.withLanguage("xx") shouldBe UserPreferences.defaultsFor(UserPreferences.FALLBACK_LANGUAGE)
    }

    @Test
    fun `first run gives each device language its defaults and an unfinished onboarding`(): Unit =
        runBlocking {
            val expectedSources =
                mapOf(
                    "fa" to setOf(EventSource.IRAN_OFFICIAL, EventSource.INTERNATIONAL),
                    "en" to setOf(EventSource.INTERNATIONAL),
                    "ne" to setOf(EventSource.NEPAL_OFFICIAL, EventSource.INTERNATIONAL),
                    "ckb" to setOf(EventSource.INTERNATIONAL),
                    "ar" to setOf(EventSource.INTERNATIONAL),
                )
            expectedSources.forEach { (code, sources) ->
                val stored = UserPrefsMigration { code }.migrate(UserPrefs.getDefaultInstance()).toDomain()
                val spec = requireNotNull(LanguageTable.forCode(code))
                withClue(code) {
                    stored.languageCode shouldBe code
                    stored.calendars shouldBe spec.calendars
                    stored.numerals shouldBe spec.numerals
                    stored.weekStart shouldBe spec.weekStart
                    stored.weekend shouldBe spec.weekend
                    stored.prayerMethod shouldBe spec.prayerMethod
                    stored.asrJuristic shouldBe spec.asrJuristic
                    stored.islamicVariant shouldBe
                        (if (code == "fa") IslamicVariant.IRAN_OFFICIAL else IslamicVariant.UMM_AL_QURA)
                    stored.app.enabledEventSources shouldBe sources
                    stored.onboardingCompleted shouldBe false
                }
            }
        }

    @Test
    fun `the onboarding flag survives the store and older stores read it as unfinished`() {
        val done = UserPreferences.defaultsFor("fa").copy(onboardingCompleted = true)

        done.toProto().toDomain() shouldBe done
        UserPreferences
            .defaultsFor("fa")
            .toProto()
            .toDomain()
            .onboardingCompleted shouldBe false
        UserPrefs.getDefaultInstance().toDomain().onboardingCompleted shouldBe false
    }

    private companion object {
        /** Rounded sample coordinates, not official data. */
        val TEHRAN = Coordinates(35.7, 51.4)
    }
}
