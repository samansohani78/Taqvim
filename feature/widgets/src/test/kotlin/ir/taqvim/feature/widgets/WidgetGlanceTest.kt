/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.testing.unit.hasStartActivityClickAction
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasTextEqualTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Test
import org.junit.runner.RunWith

/** T-1200 Glance unit tests of the shared frame and date lines. */
@RunWith(AndroidJUnit4::class)
class WidgetGlanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val data = WidgetSamples.data(isHoliday = true)
    private val allParts = WidgetConfig.defaultFor(WidgetKind.DATE_1X1)

    private fun render(
        config: WidgetConfig,
        compact: Boolean,
        size: WidgetSize = WidgetSize.MEDIUM,
        checks: androidx.glance.appwidget.testing.unit.GlanceAppWidgetUnitTest.() -> Unit,
    ) {
        runGlanceAppWidgetUnitTest(GLANCE_TEST_TIMEOUT) {
            setContext(context)
            setAppWidgetSize(DpSize(size.widthDp.dp, size.heightDp.dp))
            provideComposable {
                WidgetFrame(config, WidgetLinks.intent(context, WidgetClickTarget.Today)) { style ->
                    WidgetDateLines(data, config, style, compact)
                }
            }
            checks()
        }
    }

    @Test
    fun dateLinesShowTheOfferedParts() {
        render(allParts, compact = false) {
            onNode(hasTextEqualTo("Sunday")).assertExists()
            onNode(hasTextEqualTo("22 Shahrivar 1405")).assertExists()
            onNode(hasTextEqualTo("13 September 2026")).assertExists()
            onNode(hasTextEqualTo("22")).assertDoesNotExist()
        }
    }

    @Test
    fun compactLayoutsShowTheDayNumberAndSwitchedOffPartsAreHidden() {
        val numberOnly = allParts.copy(contents = persistentSetOf(WidgetContent.HOLIDAYS))
        render(numberOnly, compact = true, size = WidgetSize.SMALL) {
            onNode(hasTextEqualTo("22")).assertExists()
            onNode(hasTextEqualTo("Sunday")).assertDoesNotExist()
            onNode(hasTextEqualTo("13 September 2026")).assertDoesNotExist()
            onNode(hasTextEqualTo("22 Shahrivar 1405")).assertDoesNotExist()
        }
    }

    @Test
    fun theFrameOpensTodayForEveryBackground() {
        WidgetBackground.entries.forEach { background ->
            render(allParts.copy(background = background, transparencyPercent = 50, scalePercent = 150), false) {
                onNode(hasStartActivityClickAction(WidgetLinks.intent(context, WidgetClickTarget.Today))).assertExists()
            }
        }
    }

    @Test
    fun messagesReplaceContent() {
        runGlanceAppWidgetUnitTest(GLANCE_TEST_TIMEOUT) {
            setContext(context)
            provideComposable {
                WidgetFrame(allParts, onClick = null) { style ->
                    WidgetMessage(context.getString(R.string.widget_load_failed), style)
                }
            }
            onNode(hasTextEqualTo("This widget could not be loaded. Tap to open Taqvim.")).assertExists()
        }
    }
}
