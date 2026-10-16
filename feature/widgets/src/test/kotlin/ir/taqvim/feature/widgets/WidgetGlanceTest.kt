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
import ir.taqvim.core.i18n.BidiText
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
                    WidgetDateLines(data, config, style, compact, size)
                }
            }
            checks()
        }
    }

    /**
     * One cell tall keeps the day and drops the rest, however the widget is configured.
     *
     * 40 dp less the frame's padding does not hold three lines, and before this the lower two were laid out past
     * the bottom edge: `EveryWidgetFitsItsBoxTest` caught DATE_1X1 drawing its secondary date 24x0 px at y=32 in a
     * 40 dp box. Dropping a line is the same answer T-1216 gives when two cells cannot hold three dates.
     */
    @Test
    fun oneCellTallShowsTheDayAndNothingElse() {
        render(allParts, compact = true, size = WidgetSize.SMALL) {
            onNode(hasTextEqualTo("22")).assertExists()
            onNode(hasTextEqualTo("Sunday")).assertDoesNotExist()
            onNode(hasTextEqualTo(BidiText.isolate("13 September 2026"))).assertDoesNotExist()
        }
    }

    @Test
    fun dateLinesShowTheOfferedParts() {
        render(allParts, compact = false) {
            onNode(hasTextEqualTo("Sunday")).assertExists()
            onNode(hasTextEqualTo(BidiText.isolate("22 Shahrivar 1405"))).assertExists()
            onNode(hasTextEqualTo(BidiText.isolate("13 September 2026"))).assertExists()
            onNode(hasTextEqualTo("22")).assertDoesNotExist()
        }
    }

    /**
     * Measured at [WidgetSize.MEDIUM], not at [WidgetSize.SMALL]: one cell has no room for the optional lines at
     * all, so at that size this would pass whatever the configuration said, and the configuration is the subject.
     */
    @Test
    fun compactLayoutsShowTheDayNumberAndSwitchedOffPartsAreHidden() {
        val numberOnly = allParts.copy(contents = persistentSetOf(WidgetContent.HOLIDAYS))
        render(numberOnly, compact = true, size = WidgetSize.MEDIUM) {
            onNode(hasTextEqualTo("22")).assertExists()
            onNode(hasTextEqualTo("Sunday")).assertDoesNotExist()
            onNode(hasTextEqualTo(BidiText.isolate("13 September 2026"))).assertDoesNotExist()
            onNode(hasTextEqualTo(BidiText.isolate("22 Shahrivar 1405"))).assertDoesNotExist()
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
