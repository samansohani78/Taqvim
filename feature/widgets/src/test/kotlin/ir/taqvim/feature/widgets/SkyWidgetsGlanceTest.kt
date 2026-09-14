/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.testing.unit.GlanceAppWidgetUnitTest
import androidx.glance.appwidget.testing.unit.hasStartActivityClickAction
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasAnyDescendant
import androidx.glance.testing.unit.hasContentDescriptionEqualTo
import androidx.glance.testing.unit.hasTextEqualTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.ui.painter.NormalizedPoint
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** T-1210…T-1212 Glance unit tests: the Moon, map and countdown widgets per size bucket, language and link. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SkyWidgetsGlanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val today = LocalDate(2026, 9, 13).toJdn()
    private val en = requireNotNull(LanguageTable.forCode("en"))
    private val fa = requireNotNull(LanguageTable.forCode("fa"))
    private val moon = WidgetMoon(0.45f, waxing = true, rotationDegrees = 0f, "45", "7 Mehr 1405", "22 Mehr 1405")
    private val map =
        WidgetSkyBuilder.map(
            land = listOf(floatArrayOf(0.1f, 0.1f, 0.4f, 0.1f, 0.4f, 0.4f)),
            columns = 4,
            rows = 2,
            darkness = { column, _ -> column / 4f },
            marker = NormalizedPoint(0.7f, 0.3f),
        )

    private fun render(
        widget: TaqvimGlanceWidget,
        data: WidgetData,
        size: WidgetSize,
        checks: GlanceAppWidgetUnitTest.() -> Unit,
    ) {
        val config = WidgetConfig.defaultFor(widget.kind)
        runGlanceAppWidgetUnitTest {
            setContext(context)
            setAppWidgetSize(DpSize(size.widthDp.dp, size.heightDp.dp))
            provideComposable {
                WidgetFrame(config, WidgetLinks.intent(context, WidgetClickTarget.Today)) { style ->
                    widget.Content(data, config, style, size)
                }
            }
            checks()
        }
    }

    private fun link(target: WidgetClickTarget) = hasStartActivityClickAction(WidgetLinks.intent(context, target))

    private fun countdown(
        countdown: WidgetCountdown,
        language: LanguageSpec = en,
    ): WidgetData {
        val calendar =
            if (countdown.calendar ==
                CalendarSystem.PERSIAN
            ) {
                PersianCalendarSystem
            } else {
                GregorianCalendarSystem
            }
        return WidgetSamples.data().copy(countdown = WidgetCountdownBuilder.view(countdown, calendar, today, language))
    }

    private fun trip(day: Int = 30) =
        WidgetCountdown(CalendarSystem.GREGORIAN, 2026, 9, day, startJdn = today.value - 10, title = "Trip")

    @Test
    fun theMoonShowsItsDiscAndOnMediumTheNextPhases() {
        val data = WidgetSamples.data().copy(moon = moon)
        render(MoonWidget(), data, WidgetSize.SMALL) {
            onNode(
                link(WidgetClickTarget.Astronomy).and(hasAnyDescendant(hasContentDescriptionEqualTo("Moon. Lit 45%"))),
            ).assertExists()
            onNode(hasTextEqualTo("Lit 45%")).assertDoesNotExist()
        }
        render(MoonWidget(), data, WidgetSize.MEDIUM) {
            onNode(hasTextEqualTo("Lit 45%")).assertExists()
            onNode(hasTextEqualTo("Full moon 7 Mehr 1405")).assertExists()
            onNode(hasTextEqualTo("New moon 22 Mehr 1405")).assertExists()
        }
        render(MoonWidget(), data.copy(moon = moon.copy(nextFullMoon = null)), WidgetSize.MEDIUM) {
            onNode(hasTextEqualTo("Full moon 7 Mehr 1405")).assertDoesNotExist()
        }
        render(MoonWidget(), WidgetSamples.data(), WidgetSize.MEDIUM) {
            onNode(hasTextEqualTo(context.getString(R.string.widget_load_failed))).assertExists()
        }
    }

    @Test
    @Config(qualifiers = "fa-ldrtl")
    fun persianMoonsUsePersianDigits() {
        val built =
            WidgetSkyBuilder.moon(
                Instant.parse("2026-09-14T12:00:00Z"),
                null,
                PersianCalendarSystem,
                WidgetSamples.tehran,
                fa,
            )
        assertTrue(built.illumination.all { it in '۰'..'۹' })
        render(MoonWidget(), WidgetSamples.data().copy(moon = built), WidgetSize.MEDIUM) {
            onNode(hasTextEqualTo("${built.illumination}٪ روشن")).assertExists()
            onNode(hasTextEqualTo("ماه کامل ${built.nextFullMoon}")).assertExists()
        }
    }

    @Test
    fun theMapDrawsDayAndNightAndOpensTheMap() {
        render(MapWidget(), WidgetSamples.data().copy(map = map), WidgetSize.LARGE) {
            onNode(
                hasContentDescriptionEqualTo(
                    "World map with day and night and your place",
                ).and(link(WidgetClickTarget.WorldMap)),
            ).assertExists()
        }
        render(MapWidget(), WidgetSamples.data().copy(map = map.copy(marker = null)), WidgetSize.LARGE) {
            onNode(hasContentDescriptionEqualTo("World map with day and night")).assertExists()
        }
        render(MapWidget(), WidgetSamples.data(), WidgetSize.LARGE) {
            onNode(hasTextEqualTo(context.getString(R.string.widget_load_failed))).assertExists()
        }
    }

    @Test
    fun theCountdownShowsDaysLeftWithWeeksOnMediumAndOpensTheDay() {
        render(CountdownWidget(), countdown(trip()), WidgetSize.MEDIUM) {
            onNode(hasTextEqualTo("Trip")).assertExists()
            onNode(hasTextEqualTo("days left")).assertExists()
            onNode(hasTextEqualTo("2 weeks, 3 days")).assertExists()
            onNode(link(WidgetClickTarget.Day(LocalDate(2026, 9, 30))).and(hasAnyDescendant(hasTextEqualTo("17"))))
                .assertExists()
        }
        render(CountdownWidget(), countdown(trip()), WidgetSize.SMALL) {
            onNode(hasTextEqualTo("17")).assertExists()
            onNode(hasTextEqualTo("Trip")).assertDoesNotExist()
            onNode(hasTextEqualTo("2 weeks, 3 days")).assertDoesNotExist()
        }
        render(CountdownWidget(), countdown(trip(day = 14)), WidgetSize.MEDIUM) {
            onNode(hasTextEqualTo("1")).assertExists()
            onNode(hasTextEqualTo("day left")).assertExists()
        }
        render(CountdownWidget(), countdown(trip(day = 10)), WidgetSize.MEDIUM) {
            onNode(hasTextEqualTo("3")).assertExists()
            onNode(hasTextEqualTo("days ago")).assertExists()
        }
        render(CountdownWidget(), countdown(trip(day = 13)), WidgetSize.MEDIUM) {
            onNode(hasTextEqualTo("Today")).assertExists()
        }
        render(CountdownWidget(), WidgetSamples.data(), WidgetSize.MEDIUM) {
            onNode(hasTextEqualTo("Choose a date in the widget settings")).assertExists()
        }
    }

    @Test
    fun timeSinceShowsTheAge() {
        val birthday =
            WidgetCountdown(CalendarSystem.PERSIAN, 1370, 6, 15, startJdn = 0, CountdownMode.SINCE, title = "Birthday")
        render(CountdownWidget(), countdown(birthday), WidgetSize.MEDIUM) {
            onNode(hasTextEqualTo("35")).assertExists()
            onNode(hasTextEqualTo("years")).assertExists()
            onNode(hasTextEqualTo("35 years, 7 days")).assertExists()
            onNode(link(WidgetClickTarget.Day(LocalDate(1991, 9, 6))).and(hasAnyDescendant(hasTextEqualTo("Birthday"))))
                .assertExists()
        }
    }

    @Test
    @Config(qualifiers = "fa-ldrtl")
    fun persianCountdownsUsePersianDigitsAndWords() {
        render(CountdownWidget(), countdown(trip(), fa), WidgetSize.MEDIUM) {
            onNode(hasTextEqualTo("۱۷")).assertExists()
            onNode(hasTextEqualTo("روز مانده")).assertExists()
            onNode(hasTextEqualTo("۲ هفته، ۳ روز")).assertExists()
        }
    }
}
