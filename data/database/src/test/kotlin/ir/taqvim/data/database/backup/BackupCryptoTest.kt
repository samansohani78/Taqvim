/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.database.backup

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * T-605 (U): key derivation and authenticated encryption. The PBKDF2 and SP 800-108 vectors were computed with Python's
 * `hashlib.pbkdf2_hmac` and `hmac` (an implementation independent of the JCA provider under test); the first PBKDF2
 * vector is also the RFC 7914 §11 PBKDF2-HMAC-SHA256 example (first 32 bytes).
 */
class BackupCryptoTest {
    private fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it) }

    private fun bytes(hex: String) = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    @Test
    fun `PBKDF2-HMAC-SHA256 matches independent vectors`() {
        hex(BackupCrypto.pbkdf2("passwd".toCharArray(), "salt".encodeToByteArray(), 1)) shouldBe
            "55ac046e56e3089fec1691c22544b605f94185216dde0465e68b9d57c20dacbc"
        hex(BackupCrypto.pbkdf2("Password".toCharArray(), "NaCl".encodeToByteArray(), 80_000)) shouldBe
            "4ddcd8f60b98be21830cee5ef22701f9641a4418d04c0414aeff08876b34ab56"
        // Non-ASCII passphrases are encoded as UTF-8.
        hex(BackupCrypto.pbkdf2("پسورد".toCharArray(), ByteArray(16) { it.toByte() }, 1_000)) shouldBe
            "2bf54ae37c064f2fbde913810fd05d7d2a1a81f4797c881ab3969083ee8818c3"
    }

    @Test
    fun `SP 800-108 counter-mode KDF separates the encryption key and the check value`() {
        val master = ByteArray(32) { it.toByte() }

        hex(BackupCrypto.counterModeKdf(master, "taqvim-backup/aes-256-gcm", 256)) shouldBe
            "4b4df428a9843e5442c9ca23be0e195edb86d0eb11d121e750858b232f9d01da"
        hex(BackupCrypto.counterModeKdf(master, "taqvim-backup/verifier", 64)) shouldBe "9b77e426d47b076a"
        shouldThrow<IllegalArgumentException> { BackupCrypto.counterModeKdf(master, "x", 257) }
        shouldThrow<IllegalArgumentException> { BackupCrypto.counterModeKdf(master, "x", 12) }
        shouldThrow<IllegalArgumentException> { BackupCrypto.counterModeKdf(master, "x", 0) }
    }

    @Test
    fun `AES-GCM opens only with the same key, nonce, associated data and ciphertext`() {
        val key = bytes("4b4df428a9843e5442c9ca23be0e195edb86d0eb11d121e750858b232f9d01da")
        val nonce = ByteArray(BackupCrypto.NONCE_BYTES) { 7 }
        val header = "header".encodeToByteArray()
        val sealed = BackupCrypto.seal(key, nonce, header, "سلام".encodeToByteArray())

        sealed.size shouldBe "سلام".encodeToByteArray().size + BackupCrypto.TAG_BYTES
        BackupCrypto.open(key, nonce, header, sealed)?.decodeToString() shouldBe "سلام"
        BackupCrypto.open(key, nonce, "Header".encodeToByteArray(), sealed).shouldBeNull()
        BackupCrypto.open(key, ByteArray(BackupCrypto.NONCE_BYTES), header, sealed).shouldBeNull()
        BackupCrypto.open(key.reversedArray(), nonce, header, sealed).shouldBeNull()
        BackupCrypto.open(key, nonce, header, sealed.copyOf().also { it[0] = (it[0] + 1).toByte() }).shouldBeNull()
    }

    @Test
    fun `header layout round-trips and derived keys differ per salt`() {
        val header =
            EncryptedHeader(
                formatVersion = 1,
                algorithm = BackupCrypto.ALGORITHM_PBKDF2_SHA256_AES_256_GCM,
                iterations = 600_000,
                salt = ByteArray(BackupCrypto.SALT_BYTES) { 1 },
                verifier = ByteArray(BackupCrypto.VERIFIER_BYTES) { 2 },
                nonce = ByteArray(BackupCrypto.NONCE_BYTES) { 3 },
            )
        val encoded = header.encode()
        val decoded = EncryptedHeader.decode(encoded)

        EncryptedHeader.SIZE shouldBe 51
        encoded.size shouldBe EncryptedHeader.SIZE
        encoded.copyOf(8).decodeToString() shouldBe "TAQVIMBK"
        EncryptedHeader.hasMagic(encoded) shouldBe true
        EncryptedHeader.formatVersionOf(encoded) shouldBe 1
        EncryptedHeader.formatVersionOf(encoded.copyOf(9)).shouldBeNull()
        listOf(decoded.formatVersion, decoded.algorithm, decoded.iterations) shouldBe listOf(1, 1, 600_000)
        listOf(decoded.salt, decoded.verifier, decoded.nonce).map(::hex) shouldBe
            listOf(header.salt, header.verifier, header.nonce).map(::hex)
        shouldThrow<IllegalArgumentException> { EncryptedHeader.decode(encoded.copyOf(50)) }

        val one = BackupCrypto.deriveKeys("pass".toCharArray(), ByteArray(16), 10)
        val other = BackupCrypto.deriveKeys("pass".toCharArray(), ByteArray(16) { 1 }, 10)
        (hex(one.encryption) == hex(other.encryption)) shouldBe false
        one.verifier.size shouldBe BackupCrypto.VERIFIER_BYTES
    }
}
