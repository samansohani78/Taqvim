/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.database.backup

import ir.taqvim.data.database.PersonalData
import ir.taqvim.data.preferences.UserPreferences
import java.security.SecureRandom
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Backup file format (T-605): a plain JSON [BackupDocument], or that document inside an [EncryptedHeader] +
 * AES-256-GCM container ([BackupCrypto]). Blocking and CPU-heavy when encrypted: call it off the main thread.
 */
class BackupCodec internal constructor(
    private val random: SecureRandom,
    private val iterations: Int,
) {
    constructor() : this(SecureRandom(), BackupCrypto.DEFAULT_ITERATIONS)

    init {
        require(iterations in 1..BackupCrypto.MAX_ITERATIONS) { "iterations out of range" }
    }

    /** The backup file of [data] and [preferences]. A [BackupProtection.Passphrase] must not be empty. */
    fun encode(
        data: PersonalData,
        preferences: UserPreferences,
        metadata: BackupMetadata,
        protection: BackupProtection,
    ): ByteArray {
        val document =
            BackupDocument(
                appVersion = metadata.appVersion,
                createdAtEpochMillis = metadata.createdAtEpochMillis,
                preferences = preferences.toRecord(),
                data = data.toRecord(),
            )
        val json = BackupDocument.json.encodeToString(BackupDocument.serializer(), document).encodeToByteArray()
        return when (protection) {
            BackupProtection.None -> json
            is BackupProtection.Passphrase -> seal(json, protection.passphrase)
        }
    }

    /** Reads, decrypts (with [passphrase] when the file is encrypted) and validates a backup file. */
    fun decode(
        bytes: ByteArray,
        passphrase: CharArray? = null,
    ): BackupReadResult =
        if (EncryptedHeader.hasMagic(bytes)) {
            headerProblem(bytes, passphrase)?.let(BackupReadResult::Failed)
                ?: decrypt(bytes, EncryptedHeader.decode(bytes), requireNotNull(passphrase))
        } else {
            parse(bytes, encrypted = false)
        }

    private fun seal(
        plaintext: ByteArray,
        passphrase: CharArray,
    ): ByteArray {
        require(passphrase.isNotEmpty()) { "passphrase must not be empty" }
        val salt = ByteArray(BackupCrypto.SALT_BYTES).also(random::nextBytes)
        val nonce = ByteArray(BackupCrypto.NONCE_BYTES).also(random::nextBytes)
        val keys = BackupCrypto.deriveKeys(passphrase, salt, iterations)
        val header =
            EncryptedHeader(
                formatVersion = BackupDocument.FORMAT_VERSION,
                algorithm = BackupCrypto.ALGORITHM_PBKDF2_SHA256_AES_256_GCM,
                iterations = iterations,
                salt = salt,
                verifier = keys.verifier,
                nonce = nonce,
            ).encode()
        return (header + BackupCrypto.seal(keys.encryption, nonce, header, plaintext)).also { keys.encryption.fill(0) }
    }

    private fun headerProblem(
        bytes: ByteArray,
        passphrase: CharArray?,
    ): BackupError? {
        val version = EncryptedHeader.formatVersionOf(bytes)
        return when {
            version == null -> BackupError.Truncated
            version != BackupDocument.FORMAT_VERSION -> BackupError.UnsupportedFormat(version)
            bytes.size < EncryptedHeader.SIZE + BackupCrypto.TAG_BYTES -> BackupError.Truncated
            !EncryptedHeader.decode(bytes).isValid() -> BackupError.Corrupted
            passphrase == null || passphrase.isEmpty() -> BackupError.PassphraseRequired
            else -> null
        }
    }

    private fun EncryptedHeader.isValid(): Boolean =
        algorithm == BackupCrypto.ALGORITHM_PBKDF2_SHA256_AES_256_GCM && iterations in 1..BackupCrypto.MAX_ITERATIONS

    private fun decrypt(
        bytes: ByteArray,
        header: EncryptedHeader,
        passphrase: CharArray,
    ): BackupReadResult {
        val keys = BackupCrypto.deriveKeys(passphrase, header.salt, header.iterations)
        val passphraseMatches = BackupCrypto.sameVerifier(header.verifier, keys.verifier)
        val plaintext =
            if (passphraseMatches) {
                val associatedData = bytes.copyOf(EncryptedHeader.SIZE)
                val ciphertext = bytes.copyOfRange(EncryptedHeader.SIZE, bytes.size)
                BackupCrypto.open(keys.encryption, header.nonce, associatedData, ciphertext)
            } else {
                null
            }
        keys.encryption.fill(0)
        return when {
            !passphraseMatches -> BackupReadResult.Failed(BackupError.WrongPassphrase)
            plaintext == null -> BackupReadResult.Failed(BackupError.Corrupted)
            else -> parse(plaintext, encrypted = true)
        }
    }

    private fun parse(
        bytes: ByteArray,
        encrypted: Boolean,
    ): BackupReadResult {
        val text = bytes.decodeToString().trimStart()
        val root = runCatching { BackupDocument.json.parseToJsonElement(text).jsonObject }.getOrNull()
        val version = root?.primitive(FORMAT_VERSION_KEY)?.intOrNull
        val problem =
            when {
                !text.startsWith(JSON_OBJECT_START) -> BackupError.NotABackup
                root == null -> BackupError.Corrupted
                root.primitive(FORMAT_KEY)?.contentOrNull != BackupDocument.FORMAT_NAME -> BackupError.NotABackup
                version == null -> BackupError.InvalidContent("missing $FORMAT_VERSION_KEY")
                version != BackupDocument.FORMAT_VERSION -> BackupError.UnsupportedFormat(version)
                else -> null
            }
        return problem?.let(BackupReadResult::Failed) ?: toBackup(requireNotNull(root), encrypted)
    }

    private fun toBackup(
        root: JsonObject,
        encrypted: Boolean,
    ): BackupReadResult =
        runCatching {
            val document = BackupDocument.json.decodeFromJsonElement(BackupDocument.serializer(), root)
            val data = document.data.toPersonalData()
            val preferences = document.preferences.toPreferences()
            val preview =
                BackupPreview(
                    formatVersion = document.formatVersion,
                    appVersion = document.appVersion,
                    createdAtEpochMillis = document.createdAtEpochMillis,
                    encrypted = encrypted,
                    rowCounts = data.rowCounts(),
                    preferences = preferences,
                )
            RestorableBackup(preview, data, preferences)
        }.fold(
            onSuccess = BackupReadResult::Ready,
            onFailure = { BackupReadResult.Failed(BackupError.InvalidContent(it.message ?: it.javaClass.name)) },
        )

    private fun JsonObject.primitive(key: String): JsonPrimitive? =
        get(key)?.let { runCatching { it.jsonPrimitive }.getOrNull() }

    private companion object {
        const val FORMAT_KEY = "format"
        const val FORMAT_VERSION_KEY = "formatVersion"
        const val JSON_OBJECT_START = "{"
    }
}
