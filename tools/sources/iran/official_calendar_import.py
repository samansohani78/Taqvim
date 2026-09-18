#!/usr/bin/env python3
# Copyright (c) 2026 Saman Sohani. All Rights Reserved.
# Proprietary and confidential. See the LICENSE file in the repository root.
"""Imports the Calendar Center's official Iranian calendars into the golden fixtures.

Drop an official yearly calendar in `docs/sources/iran/` as `Calendar-<solar year>.pdf`, add its row to
`docs/sources/iran/MANIFEST.md` and its layout to tools/sources/iran/official_calendar_sources.py, and run this tool.
It reads every readable calendar listed there and writes

  * `core/calendar/src/test/resources/golden/persian/official/<year>.csv` — the daily Solar Hijri / lunar Hijri /
    Gregorian table with the official holidays, the occasions as printed and the page of each row;
  * `core/calendar/src/test/resources/golden/islamic-iran/official-calendars.csv` — the index of imported calendars,
    so the tests pick up a newly imported year without being edited;
  * `core/calendar/src/test/resources/golden/islamic-iran/official-month-starts-1381-1405.csv` — every lunar Hijri
    month start the imported calendars establish, with the official announcements their notices state;
  * `core/calendar/src/test/resources/golden/islamic-iran/official-month-starts.csv` and
    `dataset/iran/islamic-iran-overrides.json` — the months of the optional official override (ADR-0037), from the
    calendars in OVERRIDE_YEARS;
  * `tools/dataset/src/test/resources/golden/iran/iran-official-holidays-<year>.csv` — the official holiday dates.

Nothing here is typed by hand: every value comes from `pdftotext -bbox` output of the cited page (the reading itself
is in official_calendar_pdf.py). The tool fails loudly on a checksum that does not match the manifest, a calendar
whose pages do not add up to the whole year, or a table that does not run day by day in all three calendars.

Usage: tools/sources/iran/official_calendar_import.py [--check]
"""

import datetime
import pathlib
import sys

from official_calendar_pdf import Reader, page_count, supplied_on, verified
from official_calendar_sources import (
    ERRATA,
    LAYOUTS,
    OVERRIDE_YEARS,
    REJECTED,
    announcements,
    calendar_path,
    readable_years,
)
from official_calendar_outputs import (
    DAYS_DIRECTORY,
    HOLIDAYS_DIRECTORY,
    HISTORY,
    INDEX,
    MONTH_STARTS,
    OVERRIDES,
    days_csv,
    history_csv,
    holidays_csv,
    index_csv,
    month_starts_csv,
    overrides_json,
)

PERSIAN_MONTH_LENGTHS = [31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30]
LEAP_YEARS = "core/calendar/src/test/resources/golden/persian/official-leap-years-1206-1498.csv"
REPO = pathlib.Path(__file__).resolve().parents[3]


def esfand_length(year):
    """29 or 30, from the Calendar Center's own table of common and leap years (never computed here)."""
    for line in (REPO / LEAP_YEARS).read_text(encoding="utf-8").splitlines():
        if line.startswith(f"{year},"):
            return 30 if line.split(",")[1] else 29
    raise SystemExit(f"{LEAP_YEARS}: no row for {year}; the official leap-year table must cover it")


def persian_length(year, month):
    return PERSIAN_MONTH_LENGTHS[month - 1] if month <= len(PERSIAN_MONTH_LENGTHS) else esfand_length(year)


def persian_before(date, days):
    """The Solar Hijri date [days] before [date] (year, month, day), stepping month by month."""
    year, month, day = date
    while days > 0:
        step = min(days, day - 1)
        day -= step
        days -= step
        if days > 0:
            month, year = (12, year - 1) if month == 1 else (month - 1, year)
            day = persian_length(year, month)
            days -= 1
    return year, month, day


def check_days(reader):
    """Fails unless the imported table runs day by day in all three calendars."""
    rows = reader.rows
    if not rows:
        raise SystemExit(f"{reader.path}: no daily table found")
    year_length = sum(persian_length(reader.solar_year, month) for month in range(1, 13))
    if len(rows) != year_length:
        raise SystemExit(f"{reader.path}: {len(rows)} days imported, {year_length} printed in {reader.solar_year}")
    month, day = 1, 1
    for index, row in enumerate(rows):
        if (row["persian_month"], row["persian_day"]) != (month, day):
            raise SystemExit(f"{reader.path}: expected {month}-{day} at row {index + 1}, found {row['persian']}")
        month, day = (month + 1, 1) if day == persian_length(reader.solar_year, month) else (month, day + 1)
        if index and (row["gregorian"] - rows[index - 1]["gregorian"]).days != 1:
            raise SystemExit(f"{reader.path}: {rows[index - 1]['gregorian']} is followed by {row['gregorian']}")
        if index and not follows(rows[index - 1], row):
            raise SystemExit(f"{reader.path}: the lunar date of {row['persian']} does not follow the row above it")
    unused = set(range(len(reader.errata))) - reader.applied
    if unused:
        raise SystemExit(f"{reader.path}: errata never applied: {[reader.errata[i].reason for i in sorted(unused)]}")


def follows(previous, row):
    """Whether the lunar Hijri date of [row] is the day after [previous]'s."""
    if row["hijri_day"] == previous["hijri_day"] + 1:
        return row["hijri_month"] == previous["hijri_month"] and row["hijri_year"] == previous["hijri_year"]
    if row["hijri_day"] != 1 or previous["hijri_day"] not in (29, 30):
        return False
    if previous["hijri_month"] == 12:
        return row["hijri_month"] == 1 and row["hijri_year"] == previous["hijri_year"] + 1
    return row["hijri_month"] == previous["hijri_month"] + 1 and row["hijri_year"] == previous["hijri_year"]


def segments(readers):
    """The imported days, earliest first, split wherever a year is missing between two imported calendars."""
    rows = [dict(row, solar_year=reader.solar_year) for reader in readers for row in reader.rows]
    rows.sort(key=lambda row: row["gregorian"])
    runs = [[rows[0]]] if rows else []
    for row in rows[1:]:
        gap = (row["gregorian"] - runs[-1][-1]["gregorian"]).days
        if gap < 1:
            raise SystemExit(f"{row['gregorian']} is imported twice")
        runs[-1].append(row) if gap == 1 else runs.append([row])
    return runs


def month_starts(readers):
    """Every lunar Hijri month the imported calendars establish, earliest first, with its length where printed."""
    starts = []
    for run in segments(readers):
        found = [start_of(run[0])] if run[0]["hijri_day"] != 1 else []
        found += [start_of(row) for row in run if row["hijri_day"] == 1]
        for start, following in zip(found, found[1:] + [None]):
            length = (following["gregorian"] - start["gregorian"]).days if following else None
            if length is not None and length not in (29, 30):
                raise SystemExit(f"{start['hijri_year']}-{start['hijri_month']} would have {length} days")
            start["length"] = length
        starts += found
    return starts


def start_of(row):
    """The month start [row] establishes: the row itself on day 1, otherwise the day its printed date counts back to."""
    back = row["hijri_day"] - 1
    persian = persian_before((row["solar_year"], row["persian_month"], row["persian_day"]), back)
    return {
        "hijri_year": row["hijri_year"],
        "hijri_month": row["hijri_month"],
        "gregorian": row["gregorian"] - datetime.timedelta(days=back),
        "persian": persian,
        "page": row["page"],
        "solar_year": row["solar_year"],
        "basis": "printed" if back == 0 else "derived",
        "announcement": None,
        "printed": None if back == 0 else {
            "hijri_day": row["hijri_day"],
            "persian_year": row["solar_year"],
            "persian_month": row["persian_month"],
            "persian_day": row["persian_day"],
        },
    }


def announced(starts, readers, notices):
    """[starts] with the official announcements of [notices] ({solar year: {lunar month: (shift, page)}}) applied.

    The announced month moves by the stated day, the month before it gets that day more or less, and the announced
    month's own length is left unknown: the notice says when it began, not when it ended.
    """
    persian_of = {row["gregorian"]: row for reader in readers for row in reader.rows}
    result = [dict(start) for start in starts]
    for year, months in sorted(notices.items()):
        for month, (shift, page) in sorted(months.items()):
            matches = [index for index, start in enumerate(result)
                       if start["solar_year"] == year and start["hijri_month"] == month and start["basis"] != "derived"]
            if len(matches) != 1:
                raise SystemExit(f"Calendar-{year}.pdf: the notice about lunar month {month} matches {len(matches)} "
                                 "printed month starts")
            index = matches[0]
            start = result[index]
            moved = start["gregorian"] + datetime.timedelta(days=shift)
            day = persian_of[moved]
            before = result[index - 1] if index else None
            if before is not None and (before["hijri_month"] % 12) + 1 != month:
                before = None
            if before is None or before["length"] is None or before["length"] + shift not in (29, 30):
                raise SystemExit(f"Calendar-{year}.pdf: the announced start of month {month} leaves the month before "
                                 "it without a length of 29 or 30 days")
            before["length"] += shift
            start.update(gregorian=moved, persian=(year, day["persian_month"], day["persian_day"]), length=None,
                         basis="announced", announcement={"shift": shift, "page": page})
    return result


def read_calendar(solar_year):
    path = calendar_path(solar_year)
    digest = verified(path)
    pages = page_count(path)
    reader = Reader(path, solar_year, ERRATA.get(solar_year, ()))
    for page in range(1, pages + 1):
        reader.read_page(page)
    check_days(reader)
    return reader, digest, announcements(path, pages)


def outputs_for(readers, digests, notices):
    starts = month_starts(readers)
    history = announced(starts, readers, notices)
    override = [reader for reader in readers if reader.solar_year in OVERRIDE_YEARS]
    override_starts = month_starts(override)
    retrieved = {reader.solar_year: supplied_on(pathlib.Path(reader.path).name) for reader in readers}
    latest = max(retrieved.values())
    outputs = {}
    for reader, digest in zip(readers, digests):
        outputs[f"{DAYS_DIRECTORY}/{reader.solar_year}.csv"] = days_csv(reader, digest, retrieved[reader.solar_year])
        notice = notices.get(reader.solar_year)
        outputs[f"{HOLIDAYS_DIRECTORY}/iran-official-holidays-{reader.solar_year}.csv"] = \
            holidays_csv(reader, digest, retrieved[reader.solar_year], notice)
    override_retrieved = max(retrieved[reader.solar_year] for reader in override)
    outputs[INDEX] = index_csv(readers, digests, latest)
    outputs[HISTORY] = history_csv(history, readers, digests, latest)
    outputs[MONTH_STARTS] = month_starts_csv(override_starts, override, override_retrieved)
    outputs[OVERRIDES] = overrides_json(override_starts, override_retrieved)
    return outputs, history


def main():
    readers, digests, notices = [], [], {}
    for solar_year in readable_years():
        reader, digest, notice = read_calendar(solar_year)
        readers.append(reader)
        digests.append(digest)
        if notice:
            notices[solar_year] = notice
    outputs, history = outputs_for(readers, digests, notices)
    stale = [path for path, text in outputs.items()
             if not (REPO / path).is_file() or (REPO / path).read_text(encoding="utf-8") != text]
    summary = (f"{len(readers)} calendars ({readers[0].solar_year}–{readers[-1].solar_year}), "
               f"{sum(len(reader.rows) for reader in readers)} days, {len(history)} lunar months; not readable: "
               f"{', '.join(str(year) for year in sorted(REJECTED))}")
    if "--check" in sys.argv[1:]:
        if stale:
            raise SystemExit(f"out of date: {', '.join(sorted(stale))}; rerun {pathlib.Path(__file__).name}")
        print(f"up to date: {summary}")
        return
    for path, text in outputs.items():
        (REPO / path).parent.mkdir(parents=True, exist_ok=True)
        (REPO / path).write_text(text, encoding="utf-8")
    for reader in readers:
        skipped = ", ".join(str(page) for page, _ in reader.skipped) or "none"
        print(f"{reader.solar_year} SH ({LAYOUTS[reader.solar_year]}): {len(reader.rows)} days; pages not in the "
              f"daily table: {skipped}")
    for year, reason in sorted(REJECTED.items()):
        print(f"{year} SH: not imported — {reason}")
    print(summary)
    print(f"changed: {len(stale)} file(s)")


if __name__ == "__main__":
    main()
