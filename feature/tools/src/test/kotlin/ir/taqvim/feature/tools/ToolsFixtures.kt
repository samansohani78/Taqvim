/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.tools

import androidx.compose.runtime.Composable
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.events.EventLookup
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.i18n.TextDirection
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.testing.TimeZones
import ir.taqvim.core.ui.theme.TaqvimTheme
import ir.taqvim.core.ui.theme.ThemeMode
import ir.taqvim.core.ui.theme.ThemeSettings
import ir.taqvim.core.workdays.WorkdayCalculator
import ir.taqvim.core.workdays.WorkdayProfile
import kotlin.time.Instant

/** Sample settings, instants and states for the Tools tests (synthetic data). */
object ToolsFixtures {
    /** 2026-06-21 23:30 in Tehran (1405-03-31). */
    val NOW: Instant = Instant.parse("2026-06-21T20:00:00Z")

    fun language(code: String): LanguageSpec = requireNotNull(LanguageTable.forCode(code)) { "no language $code" }

    /** A profile with only a Friday weekend: no holidays, so workdays are plain arithmetic. */
    val fridayWeekend: WorkdayCalculator =
        WorkdayCalculator(
            EventLookup(emptyList()),
            WorkdayProfile(weekend = setOf(Weekday.FRIDAY), holidaySources = emptySet()),
        )

    fun settings(
        languageCode: String = "en",
        boardZones: List<String> = listOf("Asia/Kabul", "America/Los_Angeles"),
        workdays: WorkdayCalculator? = fridayWeekend,
    ): ToolsSettings =
        ToolsSettings(
            language = language(languageCode),
            homeZone = TimeZones.TEHRAN,
            boardZones = boardZones,
            workdays = workdays,
        )

    /** The state the ViewModel shows for [inputs] on [tab] at [now]. */
    fun state(
        tab: ToolsTab,
        inputs: ToolsInputs = ToolsInputs(),
        settings: ToolsSettings = settings(),
        now: Instant = NOW,
    ): ToolsUiState {
        val today = now.toJdn(settings.homeZone)
        val content =
            ToolsContent.Ready(
                converter = DateTools.convert(inputs.converter, today, settings),
                distance = DateTools.distance(inputs.distanceFrom, inputs.distanceTo, today, settings),
                duration = DurationPresenter.present(inputs.duration, settings.language),
                board = TimeZoneBoardBuilder.build(now, settings, settings.boardZones, inputs.zoneQuery),
                qr = QrEncoder.encode(inputs.qr),
            )
        return ToolsUiState(tab, inputs, content)
    }
}

/** The app theme with fixed (non-dynamic) colors for tests and screenshots. */
@Composable
fun ToolsTestTheme(
    rtl: Boolean = false,
    dark: Boolean = false,
    content: @Composable () -> Unit,
) {
    val settings = ThemeSettings(mode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, dynamicColor = false)
    TaqvimTheme(settings, if (rtl) TextDirection.RTL else TextDirection.LTR, content = content)
}
