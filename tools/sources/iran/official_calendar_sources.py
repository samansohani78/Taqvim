#!/usr/bin/env python3
# Copyright (c) 2026 Saman Sohani. All Rights Reserved.
# Proprietary and confidential. See the LICENSE file in the repository root.
"""What the importer knows about each official calendar in `docs/sources/iran/`: file, layout, misprints, and the
editions it cannot read. Everything here is a statement about a document, checked against that document when the
importer runs; none of it is a calendar value.

Used by tools/sources/iran/official_calendar_import.py; not a command of its own.
"""

import re
import subprocess
import unicodedata

from official_calendar_pdf import INVISIBLE, LETTERS, REPO, SOURCES, HIJRI_MONTHS, Erratum, index_of

# Layout ids of docs/sources/iran/MANIFEST.md. All four print one Solar Hijri month per page as a table of weekday,
# Solar Hijri day and month, lunar Hijri day, month and year, Gregorian day, month and year, and occasions.
LAYOUT_2002 = "ut-daily-2002"  # 1381–1386: a "Dear user" notice page in 1381 and 1383–1385; Wednesday misspelt in 1381
LAYOUT_2008 = "ut-daily-2008"  # 1387–1394, 1397–1400, 1403: year-overview and Nowruz pages among the month pages
LAYOUT_2025 = "ut-daily-2025"  # 1404–1405: month pages only
# 1395, 1396, 1401 and 1402 have the 2008 layout, but their text layer names several digit glyphs with one character
# (0, 7, 8 and 9 all extract as 1 in the 1401 edition, whose own title then reads 0410), so their digits are read from
# the glyphs the page draws (official_calendar_glyphs.py).
LAYOUT_GLYPHS = "ut-daily-2008-glyphs"

LAYOUTS = {year: LAYOUT_2002 for year in range(1381, 1387)}
LAYOUTS.update({year: LAYOUT_2008 for year in (*range(1387, 1395), *range(1397, 1401), 1403)})
LAYOUTS.update({1404: LAYOUT_2025, 1405: LAYOUT_2025})
LAYOUTS.update({year: LAYOUT_GLYPHS for year in (1395, 1396, 1401, 1402)})

# Editions the importer cannot read at all, with the reason. Empty since the glyph reader recovered the last four;
# a year whose glyphs fail the reader's gate is reported by the importer instead of being imported.
REJECTED = {}

# The calendars whose lunar months make up the optional official override of ADR-0037. Extending the shipped
# override to earlier years is a product decision of its own; the other years are test oracles only.
OVERRIDE_YEARS = (1404, 1405)

ERRATA = {
    1381: [Erratum(
        "hijri_day", (6, 2), (6, 17), 6, 9,
        "1381 Shahrivar page: after 14 Jumada II on 1 Shahrivar the lunar day column prints 6 to 21 for 2 to 17 "
        "Shahrivar, then 1 Rajab on 18 Shahrivar; the days are 15 to 30, which both the first row and the start of "
        "Rajab fix",
    )],
    1383: [Erratum(
        "hijri_year", (12, 1), (12, 30), 1427, -1,
        "1383 Esfand page: prints 9 Muharram 1427 on 1 Esfand, the day after 8 Muharram 1426 on 30 Bahman; the "
        "Muharram of February 2005 is that of 1426",
    )],
}

ANNOUNCEMENT = re.compile(r"ماه\s+(?:مبارک\s+)?(\S+)\W*طبق\s+اعلام\s+رسمی\W*یک\s+روز\s+(دیرتر|زودتر)")
LATER, EARLIER = "دیرتر", "زودتر"


def calendar_path(solar_year):
    return f"{SOURCES}/Calendar-{solar_year}.pdf"


def readable_years():
    """Every year in [LAYOUTS] whose text layer the importer reads directly, oldest first."""
    return [year for year in sorted(LAYOUTS) if year not in REJECTED and LAYOUTS[year] != LAYOUT_GLYPHS]


def glyph_years():
    """Every year whose digits are read from their glyphs, oldest first (see [LAYOUT_GLYPHS])."""
    return [year for year in sorted(LAYOUTS) if year not in REJECTED and LAYOUTS[year] == LAYOUT_GLYPHS]


def page_text(path, page):
    """The page's text in reading order, normalised like the table cells."""
    raw = subprocess.run(["pdftotext", "-f", str(page), "-l", str(page), str(REPO / path), "-"],
                         check=True, capture_output=True, text=True).stdout
    return " ".join(INVISIBLE.sub("", unicodedata.normalize("NFKC", raw)).translate(LETTERS).split())


def announcements(path, pages):
    """The official announcements a calendar's notice states: {lunar month: shift in days, first page stating it}.

    The 1381 and 1383–1385 editions are the Calendar Center's computed calendar, with a notice that one month began
    a day later or earlier by official announcement ("ماه ... (طبق اعلام رسمی)، یک روز دیرتر/زودتر ...").
    """
    found = {}
    for page in range(1, pages + 1):
        for match in ANNOUNCEMENT.finditer(page_text(path, page)):
            month = index_of(match.group(1), HIJRI_MONTHS, "lunar Hijri month")
            shift = 1 if match.group(2) == LATER else -1
            if found.setdefault(month, (shift, page))[0] != shift:
                raise SystemExit(f"{path}: the notices disagree about month {month}")
    return found
