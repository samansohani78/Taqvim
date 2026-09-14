/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.backup

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.Stable

/**
 * Passphrase inputs of the backup screen. They live only in memory — never in saved instance state — and are cleared
 * as soon as their content is handed over.
 */
@Stable
class PassphraseFields {
    val passphrase: TextFieldState = TextFieldState()
    val confirmation: TextFieldState = TextFieldState()
    val importPassphrase: TextFieldState = TextFieldState()

    /** A copy of the new passphrase for an export; both export inputs are cleared. The caller wipes the copy. */
    fun takeExportPassphrase(): CharArray =
        Passphrases.copyOf(passphrase.text).also {
            passphrase.clearText()
            confirmation.clearText()
        }

    /** A copy of the typed import passphrase; the input is cleared. The caller wipes the copy. */
    fun takeImportPassphrase(): CharArray =
        Passphrases.copyOf(importPassphrase.text).also { importPassphrase.clearText() }

    fun clearAll() {
        passphrase.clearText()
        confirmation.clearText()
        importPassphrase.clearText()
    }
}
