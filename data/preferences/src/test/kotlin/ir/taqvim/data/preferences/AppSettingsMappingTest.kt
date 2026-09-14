/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.preferences

import io.kotest.assertions.throwables.shouldThrow
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
    fun `defaults are the same for every language`() {
        LanguageTable.languages.forEach { spec ->
            UserPreferences.defaultsFor(spec.code).app shouldBe
                AppSettings.DEFAULT
        }
        AppSettings.DEFAULT.enabledEventSources shouldBe
            setOf(
                EventSource.IRAN_OFFICIAL,
                EventSource.AFGHANISTAN_OFFICIAL,
                EventSource.NEPAL_OFFICIAL,
                EventSource.INTERNATIONAL,
            )
        AppSettings.DEFAULT.showWeekNumbers shouldBe false
        AppSettings.DEFAULT.dynamicColor shouldBe true
        AppSettings.DEFAULT.allDayReminderMinute shouldBe 9 * 60
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
        older.toDomain().app shouldBe AppSettings.DEFAULT
        UserPrefs.getDefaultInstance().toDomain().app shouldBe AppSettings.DEFAULT
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
}
