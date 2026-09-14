/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.backup

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** T-1503: passphrase strength hints, new-passphrase checks, wiping, and a label for every value. */
class PassphrasesTest {
    @Test
    fun `strength counts length and kinds of characters`() {
        mapOf(
            "" to PassphraseStrength.EMPTY,
            "abc1234" to PassphraseStrength.WEAK,
            "abcdefgh" to PassphraseStrength.FAIR,
            "abcdefghij" to PassphraseStrength.FAIR,
            "Abcdefgh12" to PassphraseStrength.STRONG,
            "abcdefghijklmn" to PassphraseStrength.STRONG,
            "کوه دماوند ۱۲" to PassphraseStrength.STRONG,
            "کوهدماوند" to PassphraseStrength.FAIR,
        ).forEach { (text, strength) -> Passphrases.strength(text) shouldBe strength }
    }

    @Test
    fun `new passphrases need eight characters typed the same way twice`() {
        Passphrases.problem("short", "short") shouldBe PassphraseProblem.TOO_SHORT
        Passphrases.problem("short", "other") shouldBe PassphraseProblem.TOO_SHORT
        Passphrases.problem("abcdefgh", "abcdefgH") shouldBe PassphraseProblem.MISMATCH
        Passphrases.problem("abcdefgh", "abcdefgh1") shouldBe PassphraseProblem.MISMATCH
        Passphrases.problem("abcdefgh", "abcdefgh") shouldBe null
    }

    @Test
    fun `a copied passphrase keeps its text until it is wiped`(): Unit =
        runBlocking {
            checkAll(200, Arb.string(0..40)) { text ->
                val copy = Passphrases.copyOf(text)
                copy.concatToString() shouldBe text
                Passphrases.wipe(copy)
                copy.size shouldBe text.length
                copy.all { it == Char.MIN_VALUE } shouldBe true
            }
        }

    @Test
    fun `every value has its own label`() {
        val labels =
            BackupFailure.entries.map(BackupLabels::failure) +
                BackupTableKind.entries.map(BackupLabels::table) +
                PassphraseStrength.entries.map(BackupLabels::strength) +
                PassphraseProblem.entries.map(BackupLabels::problem) +
                StoredDataKind.entries.map(BackupLabels::dataTitle) +
                PermissionKind.entries.map(BackupLabels::permissionTitle) +
                PermissionKind.entries.map(BackupLabels::permissionWhy)
        labels.toSet() shouldHaveSize labels.size
        StoredDataKind.entries.filter { BackupLabels.clearAction(it) != null } shouldBe
            StoredDataKind.entries.filter { it.clearable }
        StoredDataKind.entries.map(BackupLabels::dataWhere).toSet() shouldHaveSize 4
    }
}
