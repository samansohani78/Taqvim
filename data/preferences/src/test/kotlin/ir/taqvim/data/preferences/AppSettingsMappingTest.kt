/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.preferences

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.set
import io.kotest.property.checkAll
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.praytimes.HighLatitudeRule
import ir.taqvim.core.testing.PropertyTesting
import ir.taqvim.data.preferences.proto.AppSettingsProto
import ir.taqvim.data.preferences.proto.EventSourceProto
import ir.taqvim.data.preferences.proto.LevelOffsetProto
import ir.taqvim.data.preferences.proto.UserPrefs
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** T-1500: settings-screen values survive the proto, older stores read as defaults and stored oddities are repaired. */
class AppSettingsMappingTest {
    private val flags = Arb.list(Arb.boolean(), 8..8)
    private val sources = Arb.set(Arb.element(AppSettings.SELECTABLE_SOURCES), 0..AppSettings.SELECTABLE_SOURCES.size)
    private val words = Arb.list(Arb.int(0..999).map { "query $it" }, 0..AppSettings.MAX_RECENT_SEARCHES)
    private val offsets = Arb.int(-45..45).map { LevelOffset(it.toDouble(), -it / 2.0) }

    @Test
    fun `defaults differ per language only in the national official source`() {
        NATIONAL_SOURCE_BY_LANGUAGE.keys shouldBe LanguageTable.languages.map { it.code }.toSet()
        LanguageTable.languages.forEach { spec ->
            val app = UserPreferences.defaultsFor(spec.code).app
            withClue(spec.code) {
                app.enabledEventSources shouldBe
                    setOfNotNull(EventSource.INTERNATIONAL, NATIONAL_SOURCE_BY_LANGUAGE.getValue(spec.code))
                app.eventSourcesChosen shouldBe false
                app.copy(enabledEventSources = AppSettings.DEFAULT.enabledEventSources) shouldBe AppSettings.DEFAULT
            }
        }
        AppSettings.DEFAULT.enabledEventSources shouldBe setOf(EventSource.INTERNATIONAL)
        AppSettings.DEFAULT.showWeekNumbers shouldBe false
        AppSettings.DEFAULT.dynamicColor shouldBe true
        AppSettings.DEFAULT.allDayReminderMinute shouldBe 9 * 60
        // T-1804: no network use until the user allows subscriptions to refresh.
        AppSettings.DEFAULT.subscriptionsNetworkAllowed shouldBe false
        // T-1213/T-1214: the large day number and the dynamic launcher icon are opt-in.
        AppSettings.DEFAULT.persistentNotificationLargeNumber shouldBe false
        AppSettings.DEFAULT.dynamicLauncherIcon shouldBe false
        UserPrefs
            .getDefaultInstance()
            .toDomain()
            .app.dynamicLauncherIcon shouldBe false
    }

    @Test
    fun `random settings round-trip through the proto and the serializer`(): Unit =
        runBlocking {
            checkAll(
                PropertyTesting.iterations,
                flags,
                sources,
                Arb.element(HighLatitudeRule.entries),
                words,
                offsets,
                Arb.int(AppSettings.ALL_DAY_REMINDER_MINUTES),
            ) { flag, enabled, rule, recent, offset, minute ->
                // Chosen sources are kept as stored; unchosen ones follow the language (tested below).
                val app =
                    AppSettings(
                        dynamicColor = flag[0],
                        highContrast = flag[1],
                        boldText = flag[2],
                        gradient = flag[3],
                        showWeekNumbers = flag[4],
                        enabledEventSources = enabled,
                        highLatitudeRule = rule,
                        subscriptionsNetworkAllowed = flag[5],
                        persistentNotification = flag[6],
                        rememberRecentSearches = flag[7],
                        recentSearches = recent.distinct(),
                        timeZoneBoard = listOf("Asia/Tehran", "Europe/Berlin").take(recent.size % 3),
                        levelOffsets = if (flag[0]) mapOf("FLAT" to offset, "PORTRAIT" to offset) else emptyMap(),
                        allDayReminderMinute = minute,
                        eventSourcesChosen = true,
                        persistentNotificationLargeNumber = flag[1] != flag[6],
                        dynamicLauncherIcon = flag[2] != flag[7],
                    )
                val prefs = UserPreferences.defaultsFor("fa").copy(app = app)
                val bytes = ByteArrayOutputStream().also { UserPrefsSerializer.writeTo(prefs.toProto(), it) }
                UserPrefsSerializer.readFrom(ByteArrayInputStream(bytes.toByteArray())).toDomain() shouldBe prefs
            }
        }

    @Test
    fun `stores written before T-1500 read as the defaults`() {
        val older =
            UserPreferences
                .defaultsFor("en")
                .toProto()
                .toBuilder()
                .clearAppSettings()
                .build()

        older.hasAppSettings() shouldBe false
        older.toDomain().app shouldBe AppSettings.defaultsFor("en")
        UserPrefs.getDefaultInstance().toDomain().app shouldBe
            AppSettings.defaultsFor(UserPreferences.FALLBACK_LANGUAGE)
    }

    @Test
    fun `event sources follow the language until the user chooses them`(): Unit =
        runBlocking {
            // A store written by T-1500 before the choice flag: its list was the old all-sources default.
            val t1500 =
                UserPreferences
                    .defaultsFor("fa")
                    .toProto()
                    .toBuilder()
                    .setAppSettings(
                        AppSettings.DEFAULT
                            .copy(enabledEventSources = OLD_T1500_DEFAULT_SOURCES)
                            .toProto(),
                    ).build()
            t1500.appSettings.eventSourcesChosen shouldBe false
            t1500.toDomain().app.enabledEventSources shouldBe
                setOf(EventSource.IRAN_OFFICIAL, EventSource.INTERNATIONAL)
            t1500
                .toBuilder()
                .setLanguageCode("prs")
                .build()
                .toDomain()
                .app.enabledEventSources shouldBe setOf(EventSource.AFGHANISTAN_OFFICIAL, EventSource.INTERNATIONAL)

            val persian = UserPreferences.defaultsFor("fa")
            val chosen =
                persian.copy(
                    app =
                        persian.app.copy(
                            enabledEventSources = setOf(EventSource.AFGHANISTAN_OFFICIAL),
                            eventSourcesChosen = true,
                        ),
                )
            val bytes = ByteArrayOutputStream().also { UserPrefsSerializer.writeTo(chosen.toProto(), it) }
            val reread = UserPrefsSerializer.readFrom(ByteArrayInputStream(bytes.toByteArray())).toDomain()
            reread shouldBe chosen
            reread
                .toProto()
                .toBuilder()
                .setLanguageCode("ne")
                .build()
                .toDomain()
                .app.enabledEventSources shouldBe setOf(EventSource.AFGHANISTAN_OFFICIAL)
            chosen
                .copy(app = chosen.app.copy(enabledEventSources = emptySet()))
                .toProto()
                .toDomain()
                .app.enabledEventSources shouldBe emptySet()
        }

    @Test
    fun `stored oddities are repaired`() {
        fun offset(
            orientation: String,
            pitch: Double,
            roll: Double,
        ) = LevelOffsetProto
            .newBuilder()
            .setOrientation(orientation)
            .setPitchDegrees(pitch)
            .setRollDegrees(roll)
            .build()

        val stored =
            AppSettingsProto
                .newBuilder()
                .addEnabledEventSources(EventSourceProto.EVENT_SOURCE_UNSPECIFIED)
                .addEnabledEventSources(EventSourceProto.EVENT_SOURCE_ANCIENT_IRAN)
                .addEnabledEventSourcesValue(42)
                .setHighLatitudeRuleValue(99)
                .addAllRecentSearches(listOf(" نوروز ", "", "نوروز") + (1..20).map { "q$it" })
                .addAllTimeZoneBoard(listOf("Asia/Kabul", " Asia/Kabul", " "))
                .addLevelOffsets(offset("FLAT", 90.0, -3.0))
                .addLevelOffsets(offset(" ", 1.0, 1.0))
                .addLevelOffsets(offset("PORTRAIT", Double.NaN, 1.0))
                .build()

        val app = stored.toDomain()

        app.enabledEventSources shouldBe setOf(EventSource.ANCIENT_IRAN)
        app.highLatitudeRule shouldBe AppSettings.DEFAULT.highLatitudeRule
        app.recentSearches shouldBe listOf("نوروز") + (1..9).map { "q$it" }
        app.timeZoneBoard shouldBe listOf("Asia/Kabul")
        app.levelOffsets shouldBe mapOf("FLAT" to LevelOffset(45.0, -3.0))
        app.allDayReminderMinute shouldBe AppSettings.DEFAULT_ALL_DAY_REMINDER_MINUTE
        stored
            .toBuilder()
            .setAllDayReminderMinute(0)
            .build()
            .toDomain()
            .allDayReminderMinute shouldBe 0
        stored
            .toBuilder()
            .setAllDayReminderMinute(2_000)
            .build()
            .toDomain()
            .allDayReminderMinute shouldBe
            AppSettings.DEFAULT_ALL_DAY_REMINDER_MINUTE
    }

    @Test
    fun `app settings are validated`() {
        shouldThrow<IllegalArgumentException> { LevelOffset(Double.POSITIVE_INFINITY, 0.0) }
        shouldThrow<IllegalArgumentException> { LevelOffset(0.0, -46.0) }
        shouldThrow<IllegalArgumentException> {
            AppSettings.DEFAULT.copy(enabledEventSources = setOf(EventSource.USER))
        }
        shouldThrow<IllegalArgumentException> {
            AppSettings.DEFAULT.copy(recentSearches = (0..AppSettings.MAX_RECENT_SEARCHES).map { "q$it" })
        }
        shouldThrow<IllegalArgumentException> {
            AppSettings.DEFAULT.copy(timeZoneBoard = (0..AppSettings.MAX_BOARD_ZONES).map { "Zone/$it" })
        }
        shouldThrow<IllegalArgumentException> { AppSettings.DEFAULT.copy(allDayReminderMinute = 1_440) }
    }

    private companion object {
        /** The T-1500 default before the ADR-0007 addendum: every selectable source but ancient Iranian festivals. */
        val OLD_T1500_DEFAULT_SOURCES: Set<EventSource> =
            AppSettings.SELECTABLE_SOURCES.toSet() - EventSource.ANCIENT_IRAN

        /** ADR-0007 §3 addendum: the national official source each launch language shows by default. */
        val NATIONAL_SOURCE_BY_LANGUAGE: Map<String, EventSource?> =
            mapOf(
                "fa" to EventSource.IRAN_OFFICIAL,
                "prs" to EventSource.AFGHANISTAN_OFFICIAL,
                "ps" to EventSource.AFGHANISTAN_OFFICIAL,
                "ne" to EventSource.NEPAL_OFFICIAL,
            ) +
                listOf(
                    "ar",
                    "ckb",
                    "kmr",
                    "az",
                    "tr",
                    "ur",
                    "hi",
                    "ta",
                    "bn",
                    "tg",
                    "uz",
                    "en",
                    "ru",
                    "de",
                    "fr",
                    "es",
                    "id",
                    "ms",
                    "zh",
                    "ja",
                ).associateWith { null }
    }
}
