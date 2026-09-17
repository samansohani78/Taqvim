/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.preferences

import androidx.datastore.core.CorruptionException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.IslamicMonthOverrides
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.i18n.NumeralSystem
import ir.taqvim.core.model.AsrJuristic
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.PrayerMethod
import ir.taqvim.core.model.Weekday
import ir.taqvim.data.preferences.proto.UserPrefs
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

/** T-600: first-run defaults per language, table-driven schema migrations and proto mapping. */
class UserPreferencesMappingTest {
    @TestFactory
    fun `defaults follow every language of the language table`(): List<DynamicTest> =
        LanguageTable.languages.map { spec ->
            DynamicTest.dynamicTest(spec.code) {
                val defaults = UserPreferences.defaultsFor(spec.code)
                defaults.languageCode shouldBe spec.code
                defaults.calendars shouldBe spec.calendars
                defaults.numerals shouldBe spec.numerals
                defaults.weekStart shouldBe spec.weekStart
                defaults.weekend shouldBe spec.weekend
                defaults.prayerMethod shouldBe spec.prayerMethod
                defaults.asrJuristic shouldBe spec.asrJuristic
                defaults.islamicVariant shouldBe
                    if (spec.code == "fa") IslamicVariant.IRAN_OFFICIAL else IslamicVariant.UMM_AL_QURA
                defaults.themeMode shouldBe ThemeMode.SYSTEM
                defaults.toProto().toDomain() shouldBe defaults
            }
        }

    @Test
    fun `the Islamic override round-trips and older stores read as computed`() {
        val bundled = IslamicMonthOverrides.bundledIranOfficialText().orEmpty()
        val official =
            UserPreferences.defaultsFor("fa").copy(
                islamicOverride = IslamicOverrideSetting(IslamicOverrideOrigin.OFFICIAL_BUNDLED, bundled),
            )

        UserPrefs.getDefaultInstance().toDomain().islamicOverride shouldBe IslamicOverrideSetting.NONE
        UserPreferences.defaultsFor("fa").islamicOverride.table shouldBe null
        official.toProto().toDomain() shouldBe official
        official.islamicOverride.table?.monthCount shouldBe 25
        official.withLanguage("en").islamicOverride shouldBe official.islamicOverride
        IslamicOverrideSetting(IslamicOverrideOrigin.IMPORTED, "{").let {
            it.table shouldBe null
            it.isBroken shouldBe true
        }
        IslamicOverrideSetting.NONE.isBroken shouldBe false
    }

    @TestFactory
    fun `schema migrations`(): List<DynamicTest> {
        val stored = UserPreferences.defaultsFor("en").copy(themeMode = ThemeMode.DARK).toProto()
        val rows =
            listOf(
                Triple(
                    "fresh store, Persian device",
                    UserPrefs.getDefaultInstance() to "fa",
                    UserPreferences.defaultsFor("fa"),
                ),
                Triple(
                    "fresh store, English device",
                    UserPrefs.getDefaultInstance() to "en",
                    UserPreferences.defaultsFor("en"),
                ),
                Triple(
                    "fresh store, unsupported device language",
                    UserPrefs.getDefaultInstance() to "xx",
                    UserPreferences.defaultsFor("en"),
                ),
                Triple(
                    "unversioned store keeps its language",
                    UserPrefs.newBuilder().setLanguageCode("ar").build() to "fa",
                    UserPreferences.defaultsFor("ar"),
                ),
                Triple("current store is untouched", stored to "fa", stored.toDomain()),
            )
        return rows.map { (name, input, expected) ->
            DynamicTest.dynamicTest(name) {
                runBlocking {
                    val (prefs, device) = input
                    val migration = UserPrefsMigration { device }
                    val result = if (migration.shouldMigrate(prefs)) migration.migrate(prefs) else prefs
                    result.schemaVersion shouldBe UserPrefsMigration.CURRENT_SCHEMA_VERSION
                    result.toDomain() shouldBe expected
                    migration.cleanUp()
                }
            }
        }
    }

    @Test
    fun `every enum value survives the proto and unknown values fall back to defaults`() {
        val base = UserPreferences.defaultsFor("fa")
        val variants =
            CalendarSystem.entries.map { base.copy(calendars = listOf(it)) } +
                NumeralSystem.entries.map { base.copy(numerals = it) } +
                Weekday.entries.map { base.copy(weekStart = it, weekend = setOf(it)) } +
                PrayerMethod.entries.map { base.copy(prayerMethod = it) } +
                AsrJuristic.entries.map { base.copy(asrJuristic = it) } +
                IslamicVariant.entries.map { base.copy(islamicVariant = it) } +
                ThemeMode.entries.map { base.copy(themeMode = it) } +
                base.copy(hijriOffsetDays = -1, hijriOffsetSetAtEpochMillis = 1_789_000_000_000L)

        variants.forEach { it.toProto().toDomain() shouldBe it }
        UserPrefs
            .newBuilder()
            .setLanguageCode("fa")
            .setNumeralsValue(99)
            .addCalendarsValue(42)
            .build()
            .toDomain() shouldBe base
        UserPrefs.getDefaultInstance().toDomain() shouldBe UserPreferences.defaultsFor("en")
    }

    @Test
    fun `serializer round-trips and reports corruption`(): Unit =
        runBlocking {
            val prefs = UserPreferences.defaultsFor("prs").toProto()
            val bytes = ByteArrayOutputStream().also { UserPrefsSerializer.writeTo(prefs, it) }.toByteArray()

            UserPrefsSerializer.readFrom(ByteArrayInputStream(bytes)) shouldBe prefs
            UserPrefsSerializer.defaultValue shouldBe UserPrefs.getDefaultInstance()
            shouldThrow<CorruptionException> {
                UserPrefsSerializer.readFrom(
                    ByteArrayInputStream(byteArrayOf(0x0A, 0x7F)),
                )
            }
        }
}
