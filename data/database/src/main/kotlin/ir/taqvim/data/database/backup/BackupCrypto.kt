/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.database.backup

import java.nio.ByteBuffer
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Keys derived from a passphrase: the AES-256 key and the passphrase check value stored in the header. */
internal class DerivedKeys(
    val encryption: ByteArray,
    val verifier: ByteArray,
)

/**
 * Passphrase-based encryption of a backup (T-605), platform JCA only:
 * - PBKDF2-HMAC-SHA256 (NIST SP 800-132) turns the passphrase and a random 128-bit salt into a 256-bit master key; the
 *   default iteration count follows the OWASP Password Storage Cheat Sheet.
 * - Two keys are separated from the master key with the NIST SP 800-108r1 KDF in counter mode (HMAC-SHA256, one block):
 *   the AES-256 key and a 64-bit check value that tells a wrong passphrase apart from a damaged file.
 * - AES-256-GCM (NIST SP 800-38D) with a random 96-bit nonce and a 128-bit tag authenticates the header as associated
 *   data and encrypts the JSON document.
 */
internal object BackupCrypto {
    const val ALGORITHM_PBKDF2_SHA256_AES_256_GCM: Int = 1
    const val DEFAULT_ITERATIONS: Int = 600_000
    const val MAX_ITERATIONS: Int = 10_000_000
    const val SALT_BYTES: Int = 16
    const val NONCE_BYTES: Int = 12
    const val VERIFIER_BYTES: Int = 8
    const val TAG_BYTES: Int = 16

    private const val KEY_BITS = 256
    private const val PBKDF2 = "PBKDF2WithHmacSHA256"
    private const val HMAC = "HmacSHA256"
    private const val AES_GCM = "AES/GCM/NoPadding"
    private const val ENCRYPTION_LABEL = "taqvim-backup/aes-256-gcm"
    private const val VERIFIER_LABEL = "taqvim-backup/verifier"
    private const val FIRST_BLOCK = 1
    private const val SEPARATOR: Byte = 0

    /** Encryption key and check value for [passphrase]; the intermediate master key is wiped. */
    fun deriveKeys(
        passphrase: CharArray,
        salt: ByteArray,
        iterations: Int,
    ): DerivedKeys {
        val master = pbkdf2(passphrase, salt, iterations)
        return DerivedKeys(
            encryption = counterModeKdf(master, ENCRYPTION_LABEL, KEY_BITS),
            verifier = counterModeKdf(master, VERIFIER_LABEL, VERIFIER_BYTES * Byte.SIZE_BITS),
        ).also { master.fill(0) }
    }

    /** PBKDF2-HMAC-SHA256 with a 256-bit output; the copy of the passphrase inside the key spec is cleared. */
    fun pbkdf2(
        passphrase: CharArray,
        salt: ByteArray,
        iterations: Int,
    ): ByteArray {
        val spec = PBEKeySpec(passphrase, salt, iterations, KEY_BITS)
        val derived = runCatching { SecretKeyFactory.getInstance(PBKDF2).generateSecret(spec).encoded }
        spec.clearPassword()
        return derived.getOrThrow()
    }

    /**
     * SP 800-108r1 §4.1 KDF in counter mode with HMAC-SHA256 and a 32-bit counter, for outputs of one block
     * ([bits] ≤ 256): `HMAC(key, [1]₃₂ ‖ label ‖ 0x00 ‖ [bits]₃₂)` with an empty context, truncated to [bits].
     */
    fun counterModeKdf(
        key: ByteArray,
        label: String,
        bits: Int,
    ): ByteArray {
        require(bits in 1..KEY_BITS && bits % Byte.SIZE_BITS == 0) { "one-block output of whole bytes" }
        val labelBytes = label.encodeToByteArray()
        val input =
            ByteBuffer
                .allocate(Int.SIZE_BYTES + labelBytes.size + 1 + Int.SIZE_BYTES)
                .putInt(FIRST_BLOCK)
                .put(labelBytes)
                .put(SEPARATOR)
                .putInt(bits)
                .array()
        val mac = Mac.getInstance(HMAC)
        mac.init(SecretKeySpec(key, HMAC))
        return mac.doFinal(input).copyOf(bits / Byte.SIZE_BITS)
    }

    /** [plaintext] encrypted and authenticated together with [associatedData]; the tag is appended. */
    fun seal(
        key: ByteArray,
        nonce: ByteArray,
        associatedData: ByteArray,
        plaintext: ByteArray,
    ): ByteArray = cipher(Cipher.ENCRYPT_MODE, key, nonce, associatedData).doFinal(plaintext)

    /** The plaintext of [ciphertext], or `null` when the tag does not verify (modified or damaged data, wrong key). */
    fun open(
        key: ByteArray,
        nonce: ByteArray,
        associatedData: ByteArray,
        ciphertext: ByteArray,
    ): ByteArray? =
        runCatching { cipher(Cipher.DECRYPT_MODE, key, nonce, associatedData).doFinal(ciphertext) }.getOrNull()

    /** Constant-time comparison of check values. */
    fun sameVerifier(
        expected: ByteArray,
        actual: ByteArray,
    ): Boolean = MessageDigest.isEqual(expected, actual)

    private fun cipher(
        mode: Int,
        key: ByteArray,
        nonce: ByteArray,
        associatedData: ByteArray,
    ): Cipher =
        Cipher.getInstance(AES_GCM).apply {
            init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BYTES * Byte.SIZE_BITS, nonce))
            updateAAD(associatedData)
        }
}

/**
 * Binary header of an encrypted backup, big-endian, [SIZE] bytes, followed by the AES-GCM ciphertext and tag:
 * magic `TAQVIMBK` · format version (u16) · algorithm (u8) · PBKDF2 iterations (i32) · salt (16) · check value (8) ·
 * nonce (12). A plaintext export is the JSON document itself.
 */
internal class EncryptedHeader(
    val formatVersion: Int,
    val algorithm: Int,
    val iterations: Int,
    val salt: ByteArray,
    val verifier: ByteArray,
    val nonce: ByteArray,
) {
    fun encode(): ByteArray =
        ByteBuffer
            .allocate(SIZE)
            .put(MAGIC.encodeToByteArray())
            .putShort(formatVersion.toShort())
            .put(algorithm.toByte())
            .putInt(iterations)
            .put(salt)
            .put(verifier)
            .put(nonce)
            .array()

    companion object {
        const val MAGIC: String = "TAQVIMBK"
        private const val MAGIC_BYTES = 8
        private const val UNSIGNED_BYTE = 0xFF
        private const val UNSIGNED_SHORT = 0xFFFF
        const val SIZE: Int =
            MAGIC_BYTES + Short.SIZE_BYTES + Byte.SIZE_BYTES + Int.SIZE_BYTES +
                BackupCrypto.SALT_BYTES + BackupCrypto.VERIFIER_BYTES + BackupCrypto.NONCE_BYTES

        /** Whether [bytes] start with the encrypted-backup magic. */
        fun hasMagic(bytes: ByteArray): Boolean =
            bytes.size >= MAGIC_BYTES && bytes.copyOf(MAGIC_BYTES).contentEquals(MAGIC.encodeToByteArray())

        /** The format version after the magic, or `null` when the file ends before it. */
        fun formatVersionOf(bytes: ByteArray): Int? =
            if (bytes.size < MAGIC_BYTES + Short.SIZE_BYTES) {
                null
            } else {
                ByteBuffer.wrap(bytes, MAGIC_BYTES, Short.SIZE_BYTES).short.toInt() and UNSIGNED_SHORT
            }

        /** The header at the start of [bytes], which must hold at least [SIZE] bytes. */
        fun decode(bytes: ByteArray): EncryptedHeader {
            require(bytes.size >= SIZE) { "header needs $SIZE bytes" }
            val buffer = ByteBuffer.wrap(bytes, MAGIC_BYTES, SIZE - MAGIC_BYTES)
            val version = buffer.short.toInt() and UNSIGNED_SHORT
            val algorithm = buffer.get().toInt() and UNSIGNED_BYTE
            val iterations = buffer.int
            val salt = ByteArray(BackupCrypto.SALT_BYTES).also(buffer::get)
            val verifier = ByteArray(BackupCrypto.VERIFIER_BYTES).also(buffer::get)
            val nonce = ByteArray(BackupCrypto.NONCE_BYTES).also(buffer::get)
            return EncryptedHeader(version, algorithm, iterations, salt, verifier, nonce)
        }
    }
}
