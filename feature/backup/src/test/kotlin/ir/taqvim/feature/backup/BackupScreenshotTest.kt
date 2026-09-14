/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.backup

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import ir.taqvim.core.uitesting.ScreenshotEnvironment
import ir.taqvim.core.uitesting.ScreenshotTheme
import ir.taqvim.core.uitesting.captureScreenshot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * T-1503 screenshots: the export card with empty passphrase inputs (no passphrase is ever captured), the restore
 * preview and the privacy dashboard, light/dark × LTR (English) / RTL (Persian). Recorded to
 * `src/test/screenshots/backup_<sample>/`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BackupScreenshotTest(
    private val sample: String,
    private val environment: ScreenshotEnvironment,
) {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun captureBackupScreens() {
        val rtl = environment.layoutDirection == LayoutDirection.Rtl
        val language = if (rtl) BackupFixtures.persian else BackupFixtures.english
        composeRule.captureScreenshot("backup_$sample", environment) {
            BackupTestTheme(rtl = rtl, dark = environment.theme.isDark) {
                when (sample) {
                    "privacy" -> {
                        PrivacyScreen(BackupFixtures.privacyState(language), PrivacyActions())
                    }

                    else -> {
                        val import =
                            if (sample == "preview") {
                                ImportModel.Preview(BackupFixtures.summary, confirming = false)
                            } else {
                                ImportModel.Idle
                            }
                        val state = BackupStateMapper.state(language, ExportModel(), import, BackupFixtures.tehran)
                        BackupScreen(state, BackupActions(), PassphraseFields())
                    }
                }
            }
        }
    }

    companion object {
        private fun environment(
            theme: ScreenshotTheme,
            direction: LayoutDirection,
        ) = ScreenshotEnvironment(theme = theme, layoutDirection = direction)

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun parameters(): List<Array<Any>> =
            listOf(
                arrayOf<Any>("export", environment(ScreenshotTheme.LIGHT, LayoutDirection.Ltr)),
                arrayOf<Any>("export", environment(ScreenshotTheme.DARK, LayoutDirection.Rtl)),
                arrayOf<Any>("preview", environment(ScreenshotTheme.LIGHT, LayoutDirection.Rtl)),
                arrayOf<Any>("preview", environment(ScreenshotTheme.DARK, LayoutDirection.Ltr)),
                arrayOf<Any>("privacy", environment(ScreenshotTheme.LIGHT, LayoutDirection.Ltr)),
                arrayOf<Any>("privacy", environment(ScreenshotTheme.DARK, LayoutDirection.Rtl)),
            )
    }
}
