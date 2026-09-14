/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.backup

/** A hint of how hard a backup passphrase is to guess. */
enum class PassphraseStrength {
    EMPTY,
    WEAK,
    FAIR,
    STRONG,
}

/** Why a new backup passphrase cannot be used yet. */
enum class PassphraseProblem {
    TOO_SHORT,
    MISMATCH,
}

/**
 * Passphrase rules of the backup screen. The strength hint only counts length and kinds of characters (letters with
 * case, other letters such as Persian, digits in any script, everything else); it guides, it does not measure entropy.
 */
internal object Passphrases {
    const val MIN_LENGTH: Int = 8
    private const val STRONG_LENGTH = 14
    private const val MIXED_LENGTH = 10
    private const val MIXED_KINDS = 3

    fun strength(text: CharSequence): PassphraseStrength =
        when {
            text.isEmpty() -> PassphraseStrength.EMPTY
            text.length < MIN_LENGTH -> PassphraseStrength.WEAK
            text.length >= STRONG_LENGTH -> PassphraseStrength.STRONG
            text.length >= MIXED_LENGTH && kinds(text) >= MIXED_KINDS -> PassphraseStrength.STRONG
            else -> PassphraseStrength.FAIR
        }

    /** The first problem of [passphrase] typed again as [confirmation], or `null` when it can be used. */
    fun problem(
        passphrase: CharSequence,
        confirmation: CharSequence,
    ): PassphraseProblem? =
        when {
            passphrase.length < MIN_LENGTH -> PassphraseProblem.TOO_SHORT
            !sameCharacters(passphrase, confirmation) -> PassphraseProblem.MISMATCH
            else -> null
        }

    /** A copy of [text] that the caller wipes with [wipe] after use. */
    fun copyOf(text: CharSequence): CharArray = CharArray(text.length) { text[it] }

    /** Overwrites every character of [secret]. */
    fun wipe(secret: CharArray) {
        secret.fill(Char.MIN_VALUE)
    }

    private fun sameCharacters(
        first: CharSequence,
        second: CharSequence,
    ): Boolean = first.length == second.length && first.indices.all { first[it] == second[it] }

    private fun kinds(text: CharSequence): Int =
        text
            .map { char ->
                when {
                    char.isLetter() && char.isUpperCase() -> CharacterKind.UPPER
                    char.isLetter() -> CharacterKind.LETTER
                    char.isDigit() -> CharacterKind.DIGIT
                    else -> CharacterKind.OTHER
                }
            }.distinct()
            .size

    private enum class CharacterKind {
        UPPER,
        LETTER,
        DIGIT,
        OTHER,
    }
}
