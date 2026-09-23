/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.database.backup

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.common.ExperimentalKotest
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import ir.taqvim.data.database.PersonalData
import ir.taqvim.data.database.backup.BackupFixtures.data
import ir.taqvim.data.database.backup.BackupFixtures.metadata
import ir.taqvim.data.database.backup.BackupFixtures.preferences
import ir.taqvim.data.preferences.AppSettings
import ir.taqvim.data.preferences.AthanPreferences
import ir.taqvim.data.preferences.AthanSound
import ir.taqvim.data.preferences.IslamicOverrideOrigin
import ir.taqvim.data.preferences.IslamicOverrideSetting
import ir.taqvim.data.preferences.LevelOffset
import java.security.SecureRandom
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test

/** T-605 (U): file format round trips, passphrase handling, damage detection and forward compatibility. */
class BackupCodecTest {
    /**
     * Fixed seed: the same inputs, and so the same covered branches, on every run and machine. The opt-in is for
     * `iterations`, which Kotest 6 still marks experimental.
     */
    @OptIn(ExperimentalKotest::class)
    private val propertyConfig = PropTestConfig(seed = 20_260_920L, iterations = 40)

    private val codec = BackupCodec(SecureRandom(), iterations = 1_000)
    private val secret = "رمز-قوی 2026"

    private fun plain(): ByteArray = codec.encode(data, preferences, metadata, BackupProtection.None)

    private fun encrypted(): ByteArray =
        codec.encode(data, preferences, metadata, BackupProtection.Passphrase(secret.toCharArray()))

    private fun ready(result: BackupReadResult): RestorableBackup =
        result.shouldBeInstanceOf<BackupReadResult.Ready>().backup

    private fun error(
        bytes: ByteArray,
        passphrase: String? = secret,
    ): BackupError = codec.decode(bytes, passphrase?.toCharArray()).shouldBeInstanceOf<BackupReadResult.Failed>().error

    private fun ByteArray.patched(
        index: Int,
        value: Int,
    ): ByteArray = copyOf().also { it[index] = value.toByte() }

    private fun ByteArray.flipped(index: Int): ByteArray = patched(index, this[index].toInt() xor 1)

    private fun jsonOf(bytes: ByteArray): JsonObject = Json.parseToJsonElement(bytes.decodeToString()).jsonObject

    private fun JsonObject.with(
        key: String,
        value: JsonElement?,
    ): ByteArray = JsonObject(if (value == null) this - key else this + (key to value)).toString().encodeToByteArray()

    private fun plainReplacing(
        old: String,
        new: String,
    ): ByteArray {
        val text = plain().decodeToString()
        return text.replace(old, new).also { it shouldNotBe text }.encodeToByteArray()
    }

    @Test
    fun `plaintext export is a JSON document that round-trips with a preview of every table`() {
        val bytes = plain()
        val backup = ready(codec.decode(bytes))

        bytes.decodeToString() shouldContain "\"format\":\"taqvim-backup\",\"formatVersion\":1"
        backup.data shouldBe data
        backup.preferences shouldBe preferences
        backup.preview shouldBe
            BackupPreview(
                formatVersion = 1,
                appVersion = "0.1.0",
                createdAtEpochMillis = metadata.createdAtEpochMillis,
                encrypted = false,
                rowCounts = BackupTable.entries.zip(listOf(2, 1, 2, 1, 1, 1, 1, 2)).toMap(),
                preferences = preferences,
            )
    }

    @Test
    fun `encrypted export round-trips, hides the content and uses a fresh salt and nonce`() {
        val bytes = encrypted()
        val backup = ready(codec.decode(bytes, secret.toCharArray()))

        bytes.copyOf(8).decodeToString() shouldBe "TAQVIMBK"
        bytes.decodeToString().contains("Dentist") shouldBe false
        backup.data shouldBe data
        backup.preferences shouldBe preferences
        backup.preview.encrypted shouldBe true
        encrypted().copyOf(EncryptedHeader.SIZE).contentEquals(bytes.copyOf(EncryptedHeader.SIZE)) shouldBe false
    }

    @Test
    fun `a wrong, missing or empty passphrase is a typed error`() {
        val bytes = encrypted()

        error(bytes, "$secret ") shouldBe BackupError.WrongPassphrase
        error(bytes, null) shouldBe BackupError.PassphraseRequired
        error(bytes, "") shouldBe BackupError.PassphraseRequired
        shouldThrow<IllegalArgumentException> {
            codec.encode(data, preferences, metadata, BackupProtection.Passphrase(CharArray(0)))
        }
        shouldThrow<IllegalArgumentException> { BackupCodec(SecureRandom(), iterations = 0) }
    }

    @Test
    fun `any passphrase round-trips and no other passphrase opens the file`(): Unit =
        runBlocking {
            val passphrases =
                Arb.list(Arb.int(0x20..0x6FF), 1..12).map { codes ->
                    String(CharArray(codes.size) { codes[it].toChar() })
                }
            checkAll(propertyConfig, passphrases) { passphrase ->
                val bytes =
                    codec.encode(
                        data,
                        preferences,
                        metadata,
                        BackupProtection.Passphrase(passphrase.toCharArray()),
                    )
                ready(codec.decode(bytes, passphrase.toCharArray())).data shouldBe data
                error(bytes, passphrase + "x") shouldBe BackupError.WrongPassphrase
            }
        }

    @Test
    fun `the shortest, longest and codepoint-boundary passphrases round-trip`() {
        listOf(
            "${0x20.toChar()}",
            String(CharArray(12) { 0x20.toChar() }),
            String(CharArray(12) { 0x6FF.toChar() }),
            "${0x20.toChar()}${0x6FF.toChar()}",
        ).forEach { passphrase ->
            val bytes = codec.encode(data, preferences, metadata, BackupProtection.Passphrase(passphrase.toCharArray()))
            ready(codec.decode(bytes, passphrase.toCharArray())).data shouldBe data
            error(bytes, passphrase + "x") shouldBe BackupError.WrongPassphrase
        }
    }

    @Test
    fun `tampered encrypted files are rejected`() {
        val bytes = encrypted()

        error(bytes.flipped(bytes.size - 1)) shouldBe BackupError.Corrupted // tag
        error(bytes.flipped(EncryptedHeader.SIZE + 2)) shouldBe BackupError.Corrupted // ciphertext
        error(bytes.flipped(EncryptedHeader.SIZE - 1)) shouldBe BackupError.Corrupted // nonce
        error(bytes.flipped(15)) shouldBe BackupError.WrongPassphrase // salt: another key
        error(bytes.flipped(31)) shouldBe BackupError.WrongPassphrase // stored check value
        error(bytes.patched(10, 2)) shouldBe BackupError.Corrupted // unknown algorithm
        error(
            bytes
                .patched(11, 0)
                .patched(12, 0)
                .patched(13, 0)
                .patched(14, 0),
        ) shouldBe BackupError.Corrupted
        error(bytes.patched(11, 0x80)) shouldBe BackupError.Corrupted // negative iteration count
    }

    @Test
    fun `truncated encrypted files and newer header versions are typed errors`() {
        val bytes = encrypted()

        error(bytes.copyOf(9)) shouldBe BackupError.Truncated
        error(bytes.copyOf(EncryptedHeader.SIZE + BackupCrypto.TAG_BYTES - 1)) shouldBe BackupError.Truncated
        error(bytes.copyOf(bytes.size - 1)) shouldBe BackupError.Corrupted
        error(bytes.patched(9, 2)) shouldBe BackupError.UnsupportedFormat(2)
        error(bytes.copyOf(10).patched(8, 1)) shouldBe BackupError.UnsupportedFormat(257)
    }

    @Test
    fun `plain files that are not backups, damaged or newer are typed errors`() {
        val json = jsonOf(plain())

        error(ByteArray(64) { (it * 37).toByte() }) shouldBe BackupError.NotABackup
        error(ByteArray(0)) shouldBe BackupError.NotABackup
        error("[1, 2]".encodeToByteArray()) shouldBe BackupError.NotABackup
        error(json.with("format", JsonPrimitive("other-app"))) shouldBe BackupError.NotABackup
        error(json.with("format", buildJsonObject { put("name", "taqvim-backup") })) shouldBe BackupError.NotABackup
        error(plain().copyOf(plain().size / 2)) shouldBe BackupError.Corrupted
        error(json.with("formatVersion", JsonPrimitive(2))) shouldBe BackupError.UnsupportedFormat(2)
        error(json.with("formatVersion", JsonPrimitive("one"))).shouldBeInstanceOf<BackupError.InvalidContent>()
        error(json.with("formatVersion", null)).shouldBeInstanceOf<BackupError.InvalidContent>()
    }

    @Test
    fun `unknown fields written by newer versions are ignored`() {
        val json = jsonOf(plain())
        val dataRecord = json.getValue("data").jsonObject
        val events = Json.parseToJsonElement(dataRecord.getValue("personalEvents").toString())
        val future =
            JsonObject(
                json +
                    ("futureFeature" to buildJsonObject { put("enabled", true) }) +
                    ("data" to JsonObject(dataRecord + ("stickers" to events) + ("personalEvents" to events))),
            ).toString().replace("\"title\":\"Dentist\"", "\"title\":\"Dentist\",\"emoji\":\"🦷\"")

        val backup = ready(codec.decode(future.encodeToByteArray()))

        backup.data shouldBe data
        backup.preferences shouldBe preferences
    }

    @Test
    fun `the Islamic override round-trips and older documents have none`() {
        val official =
            preferences.copy(
                islamicOverride = IslamicOverrideSetting(IslamicOverrideOrigin.IMPORTED, "{\"schemaVersion\":1}"),
            )
        val bytes = codec.encode(data, official, metadata, BackupProtection.None)

        ready(codec.decode(bytes)).preferences.islamicOverride shouldBe official.islamicOverride
        ready(codec.decode(plain())).preferences.islamicOverride shouldBe IslamicOverrideSetting.NONE
    }

    @Test
    fun `the chosen place round-trips, is absent in older documents and dropped when unusable here`() {
        ready(codec.decode(plain())).preferences.place shouldBe preferences.place

        val json = jsonOf(plain())
        val older = JsonObject(json.getValue("preferences").jsonObject - "place")
        ready(codec.decode(json.with("preferences", older))).preferences shouldBe preferences.copy(place = null)

        val unusable = plainReplacing("\"zoneId\":\"Asia/Tehran\"", "\"zoneId\":\"Mars/Olympus_Mons\"")
        ready(codec.decode(unusable)).preferences shouldBe preferences.copy(place = null)
    }

    @Test
    fun `settings-screen values round-trip, device-only values stay on the device and older documents use defaults`() {
        ready(codec.decode(plain())).preferences.app shouldBe preferences.app

        val json = jsonOf(plain())
        val written =
            json
                .getValue("preferences")
                .jsonObject
                .getValue("app")
                .jsonObject
        written.keys.contains("recentSearches") shouldBe false
        written.keys.contains("levelOffsets") shouldBe false

        val older = JsonObject(json.getValue("preferences").jsonObject - "app")
        ready(codec.decode(json.with("preferences", older))).preferences shouldBe
            preferences.copy(app = AppSettings.defaultsFor("fa"))

        val unusable = plainReplacing("\"ANCIENT_IRAN\"", "\"USER\"")
        ready(codec.decode(unusable)).preferences.app shouldBe AppSettings.defaultsFor("fa")

        val device =
            preferences.copy(
                app =
                    preferences.app.copy(
                        recentSearches = listOf("نوروز"),
                        levelOffsets = mapOf("FLAT" to LevelOffset(1.0, -2.0)),
                    ),
            )
        val restored = ready(codec.decode(plain())).preferences.keepingDeviceOnlyValuesOf(device)
        restored.app shouldBe device.app

        // T-1501: finishing the onboarding is device state; a restore neither brings nor clears it.
        val onboarded = device.copy(onboardingCompleted = true)
        codec
            .encode(data, onboarded, metadata, BackupProtection.None)
            .decodeToString()
            .contains("onboarding", ignoreCase = true) shouldBe false
        ready(codec.decode(plain())).preferences.keepingDeviceOnlyValuesOf(onboarded).onboardingCompleted shouldBe true
        ready(codec.decode(plain())).preferences.keepingDeviceOnlyValuesOf(device).onboardingCompleted shouldBe false
    }

    @Test
    fun `athan settings round-trip, the picked sound stays on the device and older documents use defaults`() {
        ready(codec.decode(plain())).preferences.athan shouldBe preferences.athan

        val json = jsonOf(plain())
        val older = JsonObject(json.getValue("preferences").jsonObject - "athan")
        ready(codec.decode(json.with("preferences", older))).preferences shouldBe
            preferences.copy(athan = AthanPreferences.DEFAULT)

        val loud = plainReplacing("\"volumePercent\":60", "\"volumePercent\":500")
        ready(codec.decode(loud)).preferences.athan.volumePercent shouldBe 100

        val device = preferences.copy(athan = preferences.athan.copy(sound = AthanSound("content://sounds/9", "Adhan")))
        codec
            .encode(data, device, metadata, BackupProtection.None)
            .decodeToString()
            .contains("content://sounds/9") shouldBe false
        ready(codec.decode(plain())).preferences.keepingDeviceOnlyValuesOf(device).athan shouldBe device.athan
    }

    @Test
    fun `official reminders and the all-day reminder time round-trip and older documents have neither`() {
        val backup = ready(codec.decode(plain()))
        backup.data.officialReminders shouldBe data.officialReminders
        backup.preferences.app.allDayReminderMinute shouldBe 480

        val json = jsonOf(plain())
        val olderData = JsonObject(json.getValue("data").jsonObject - "officialReminders")
        ready(codec.decode(json.with("data", olderData))).data shouldBe data.copy(officialReminders = emptyList())

        val preferencesJson = json.getValue("preferences").jsonObject
        val olderApp = JsonObject(preferencesJson.getValue("app").jsonObject - "allDayReminderMinute")
        val olderPreferences = JsonObject(preferencesJson + ("app" to olderApp))
        ready(codec.decode(json.with("preferences", olderPreferences))).preferences.app.allDayReminderMinute shouldBe
            AppSettings.DEFAULT_ALL_DAY_REMINDER_MINUTE

        val late = plainReplacing("\"allDayReminderMinute\":480", "\"allDayReminderMinute\":5000")
        ready(codec.decode(late)).preferences.app.allDayReminderMinute shouldBe
            AppSettings.DEFAULT_ALL_DAY_REMINDER_MINUTE
    }

    @Test
    fun `event exceptions and overrides round-trip and older documents have none`() {
        val backup = ready(codec.decode(plain()))
        backup.data.eventExceptions shouldBe data.eventExceptions
        backup.data.eventOverrides shouldBe data.eventOverrides

        val json = jsonOf(plain())
        val olderData = JsonObject(json.getValue("data").jsonObject - "eventExceptions" - "eventOverrides")
        ready(codec.decode(json.with("data", olderData))).data shouldBe
            data.copy(eventExceptions = emptyList(), eventOverrides = emptyList())
    }

    @Test
    fun `event exceptions and overrides that break the format are invalid content`() {
        fun invalid(changed: PersonalData): String =
            error(codec.encode(changed, preferences, metadata, BackupProtection.None))
                .shouldBeInstanceOf<BackupError.InvalidContent>()
                .reason

        val exception = data.eventExceptions[0]
        val override = data.eventOverrides[0]
        invalid(data.copy(eventExceptions = data.eventExceptions + exception)) shouldContain "duplicate event exception"
        invalid(data.copy(eventOverrides = data.eventOverrides + override.copy(title = "again"))) shouldContain
            "duplicate event override"
        invalid(data.copy(eventExceptions = listOf(exception.copy(eventId = 404)))) shouldContain "missing parent 404"
        invalid(data.copy(eventOverrides = listOf(override.copy(eventId = 405)))) shouldContain "missing parent 405"
        listOf(
            override.copy(endJdn = override.startJdn - 1),
            override.copy(endMinute = override.startMinute?.minus(1)),
            override.copy(startMinute = 1_440),
            override.copy(startMinute = null),
        ).forEach { broken ->
            invalid(data.copy(eventOverrides = listOf(broken))) shouldContain "invalid start or end"
        }
    }

    @Test
    fun `documents that break the format are invalid content`() {
        fun invalid(bytes: ByteArray): String = error(bytes).shouldBeInstanceOf<BackupError.InvalidContent>().reason

        invalid(plainReplacing("\"eventId\":9,", "\"eventId\":404,")) shouldContain "reminder"
        invalid(plainReplacing("\"eventId\":3,\"frequency\"", "\"eventId\":8,\"frequency\"")) shouldContain "recurrence"
        invalid(plainReplacing("\"rotationId\":2,", "\"rotationId\":7,")) shouldContain "shift rotation record"
        invalid(plainReplacing("\"id\":9,\"title\"", "\"id\":3,\"title\"")) shouldContain "duplicate"
        invalid(plainReplacing("\"ordinal\":-1", "\"ordinal\":0")) shouldContain "BYDAY"
        invalid(plainReplacing("\"calendarSystem\":\"GREGORIAN\"", "\"calendarSystem\":\"MARTIAN\""))
        invalid(jsonOf(plain()).with("appVersion", null))
        val twice = data.copy(shiftRecords = data.shiftRecords + data.shiftRecords)
        invalid(codec.encode(twice, preferences, metadata, BackupProtection.None)) shouldContain "duplicate"
        invalid(plainReplacing("\"daysBefore\":3", "\"daysBefore\":31")) shouldContain "official reminder"
        invalid(plainReplacing("\"eventId\":\"ir.ancient.yalda\"", "\"eventId\":\" \"")) shouldContain
            "official reminder"
        val repeated = data.copy(officialReminders = data.officialReminders + data.officialReminders[0].copy(id = 99))
        invalid(codec.encode(repeated, preferences, metadata, BackupProtection.None)) shouldContain
            "duplicate official reminder"
    }
}
