/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.i18n

import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.testing.SnapshotVerifier
import java.io.File
import org.junit.jupiter.api.Test

/**
 * The T-202 text matrix: 24 languages × 4 calendars × {long, numeric, iso} for Sunday 13 September 2026.
 * The Persian (1405-06-22) and Iran official lunar (1448-04-01) dates of that day come from the Calendar Center's
 * official 1405 calendar (core/calendar golden/persian fixtures). No verified Bikram Sambat conversion exists yet
 * (docs/DATA_TODO.md), so the Nepali row uses a placeholder date and shows structure only.
 */
class DateFormatSnapshotTest {
    @Test
    fun `formatting matrix matches the snapshot`() {
        val header =
            listOf(
                "# Sunday 2026-09-13 = 1405-06-22 SH = 1448-04-01 AH (Iran official calendar 1405)",
                "# NEPALI uses the placeholder date 2083-05-28 (structure only, not a verified conversion)",
                "# language<TAB>calendar<TAB>style<TAB>text",
            )
        val rows =
            LanguageTable.languages.flatMap { language ->
                DATES.flatMap { date ->
                    DateStyle.entries.map { style ->
                        "${language.code}\t${date.system}\t$style\t${DateFormatter.format(
                            date,
                            Weekday.SUNDAY,
                            language,
                            style,
                        )}"
                    }
                }
            }

        SnapshotVerifier()
            .verify(File(SNAPSHOT), (header + rows).joinToString("\n"), "DateFormatSnapshotTest")
            .getOrThrow()
    }

    private companion object {
        const val SNAPSHOT = "src/test/resources/snapshots/date-formats.tsv"
        val DATES =
            listOf(
                CalendarDate(CalendarSystem.PERSIAN, 1405, 6, 22),
                CalendarDate(CalendarSystem.ISLAMIC, 1448, 4, 1),
                CalendarDate(CalendarSystem.GREGORIAN, 2026, 9, 13),
                CalendarDate(CalendarSystem.NEPALI, 2083, 5, 28),
            )
    }
}
