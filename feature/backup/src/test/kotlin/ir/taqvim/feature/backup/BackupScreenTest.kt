/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.backup

import android.net.Uri
import android.os.Looper
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.core.app.ActivityOptionsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** T-1503 UI tests: export and restore round trip through the route, passphrase handling and every failure message. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BackupScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val operations = FakeOperations()
    private val launched = mutableListOf<Any?>()

    /** Answers every picker: a destination for new documents, the source file for opened ones. */
    private val registryOwner =
        object : ActivityResultRegistryOwner {
            override val activityResultRegistry: ActivityResultRegistry =
                object : ActivityResultRegistry() {
                    override fun <I, O> onLaunch(
                        requestCode: Int,
                        contract: ActivityResultContract<I, O>,
                        input: I,
                        options: ActivityOptionsCompat?,
                    ) {
                        launched += (input as? Array<*>)?.toList() ?: input
                        val uri = if (input is String) BackupFixtures.DESTINATION_URI else BackupFixtures.SOURCE_URI
                        dispatchResult(requestCode, Uri.parse(uri))
                    }
                }
        }

    private fun showRoute() {
        val viewModel = BackupViewModel(operations, FakeLanguages(BackupFixtures.english), BackupFixtures.tehran)
        composeRule.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides registryOwner) {
                BackupTestTheme { BackupRoute(viewModel = viewModel) }
            }
        }
        settle()
    }

    private fun settle() {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        composeRule.waitForIdle()
    }

    private fun click(text: String) {
        composeRule.onNodeWithText(text).performScrollTo().performClick()
        settle()
    }

    private fun type(
        label: String,
        text: String,
    ) {
        composeRule.onNodeWithText(label).performScrollTo().performTextInput(text)
        settle()
    }

    @Test
    fun encryptedExportUsesThePassphraseOnceAndClearsIt() {
        showRoute()

        composeRule.onNodeWithText("Export backup").assertIsNotEnabled()
        type("Passphrase", BackupFixtures.PASSPHRASE)
        composeRule.onNodeWithText("Strength: strong").assertIsDisplayed()
        type("Repeat passphrase", "kooh-e Damavand 5672")
        composeRule.onNodeWithText("The passphrases do not match.").assertIsDisplayed()
        composeRule.onNodeWithText("Export backup").assertIsNotEnabled()

        composeRule.onNodeWithText("Repeat passphrase").performTextReplacement(BackupFixtures.PASSPHRASE)
        settle()
        composeRule.onNodeWithText("Export backup").assertIsEnabled()
        click("Export backup")

        assertEquals(listOf<Any?>(ENCRYPTED_FILE_NAME), launched)
        assertEquals(listOf(BackupFixtures.DESTINATION_URI to BackupFixtures.PASSPHRASE), operations.exports)
        assertTrue(operations.passphraseArrays.single().all { it == Char.MIN_VALUE })
        composeRule.onNodeWithText("Backup saved (13 KB).").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Strength: not entered").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Export backup").assertIsNotEnabled()
    }

    @Test
    fun plainExportAndRestoreRoundTrip() {
        showRoute()

        composeRule.onNodeWithContentDescription("Encrypt with a passphrase").performClick()
        settle()
        click("Export backup")
        assertEquals(listOf(BackupFixtures.DESTINATION_URI to null), operations.exports)
        assertEquals(listOf<Any?>(PLAIN_FILE_NAME), launched)
        composeRule.onNodeWithText("Backup saved (13 KB).").performScrollTo().assertIsDisplayed()
        click("Dismiss")

        click("Choose backup file")
        assertEquals(listOf(IMPORT_MIME_TYPE), launched.last())
        composeRule.onNodeWithText("Not encrypted").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Tehran").performScrollTo().assertIsDisplayed()
        click("Restore this backup")
        composeRule.onNodeWithText("Replace your data?").assertIsDisplayed()
        composeRule.onNodeWithText("Replace").performClick()
        settle()

        assertEquals(1, operations.restores)
        composeRule.onNodeWithText("Restore complete: 37 items restored.").performScrollTo().assertIsDisplayed()
        click("Dismiss")
        composeRule.onNodeWithText("Choose backup file").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun encryptedImportAsksForThePassphraseAndRetries() {
        operations.encrypted()
        showRoute()

        // Plain export hides the export passphrase inputs, so "Passphrase" names only the import prompt.
        composeRule.onNodeWithContentDescription("Encrypt with a passphrase").performClick()
        settle()
        click("Choose backup file")
        composeRule.onNodeWithText("This backup is encrypted. Enter its passphrase.").assertIsDisplayed()
        composeRule.onNodeWithText("Open backup").assertIsNotEnabled()
        type("Passphrase", "wrong passphrase")
        click("Open backup")
        composeRule.onNodeWithText("That passphrase is not correct.").assertIsDisplayed()
        composeRule.onNodeWithText("Open backup").assertIsNotEnabled()

        type("Passphrase", BackupFixtures.PASSPHRASE)
        click("Open backup")

        composeRule.onNodeWithText("Encrypted").performScrollTo().assertIsDisplayed()
        assertTrue(operations.passphraseArrays.all { array -> array.all { it == Char.MIN_VALUE } })
        click("Cancel")
        composeRule.onNodeWithText("Choose backup file").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun everyFailureIsExplained() {
        var stage by mutableStateOf<ImportStage>(ImportStage.Idle)
        composeRule.setContent {
            BackupTestTheme {
                BackupScreen(
                    BackupUiState(loading = false, import = stage),
                    BackupActions(),
                    PassphraseFields(),
                )
            }
        }
        val messages =
            mapOf(
                BackupFailure.NOT_A_BACKUP to "This file is not a Taqvim backup.",
                BackupFailure.TRUNCATED to "The file is incomplete. Choose the whole backup file.",
                BackupFailure.UNSUPPORTED_FORMAT to
                    "This backup was made by a newer version of Taqvim. Update the app to restore it.",
                BackupFailure.PASSPHRASE_REQUIRED to "Enter the passphrase again to continue.",
                BackupFailure.WRONG_PASSPHRASE to "That passphrase is not correct.",
                BackupFailure.CORRUPTED to "The backup is damaged or was changed, so it cannot be restored.",
                BackupFailure.INVALID_CONTENT to "The content of this backup is not valid, so it cannot be restored.",
                BackupFailure.RESTORE_FAILED to "Your data could not be replaced. Nothing was changed.",
                BackupFailure.RESTORE_INCOMPLETE to
                    "The restore did not finish, and your data may be partly replaced. " +
                    "Taqvim will finish or undo it the next time it starts.",
                BackupFailure.FILE_UNREADABLE to "The chosen file could not be opened.",
                BackupFailure.FILE_UNWRITABLE to "The backup could not be saved there. Choose another place.",
            )
        assertEquals(BackupFailure.entries.toSet(), messages.keys)
        messages.forEach { (failure, message) ->
            stage = ImportStage.Failed(failure)
            composeRule.waitForIdle()
            composeRule.onNodeWithText(message).performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    @Config(qualifiers = "fa")
    fun failuresAreExplainedInPersian() {
        composeRule.setContent {
            BackupTestTheme(rtl = true) {
                BackupScreen(
                    BackupUiState(loading = false, import = ImportStage.Failed(BackupFailure.WRONG_PASSPHRASE)),
                    BackupActions(),
                    PassphraseFields(),
                )
            }
        }

        composeRule.onNodeWithText("این گذرواژه درست نیست.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun loadingIsAnnounced() {
        composeRule.setContent {
            BackupTestTheme { BackupScreen(BackupUiState(), BackupActions(), PassphraseFields()) }
        }

        composeRule.onNodeWithContentDescription("Loading backup and restore").assertIsDisplayed()
    }
}
