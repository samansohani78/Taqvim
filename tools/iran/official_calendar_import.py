#!/usr/bin/env python3
# Copyright (c) 2026 Saman Sohani. All Rights Reserved.
# Proprietary and confidential. See the LICENSE file in the repository root.
"""Imports the Calendar Center's official Iranian calendars into the golden fixtures.

Drop an official yearly calendar in `docs/sources/` as `Calendar-<solar year>.pdf`, add its row to
`docs/sources/MANIFEST.md`, and run this tool. It reads every `Calendar-*.pdf` it finds there and writes

  * `core/calendar/src/test/resources/golden/persian/iran-official-<year>-days.csv` — the daily
    Solar Hijri / lunar Hijri / Gregorian table with the official holidays and the page each row is printed on;
  * `core/calendar/src/test/resources/golden/islamic-iran/official-calendars.csv` — the index of imported
    calendars, so the tests pick up a newly imported year without being edited;
  * `core/calendar/src/test/resources/golden/islamic-iran/official-month-starts.csv` — every lunar Hijri month
    start the imported calendars establish, with its length where the calendars print the whole month;
  * `dataset/iran/islamic-iran-overrides.json` — the optional official override of ADR-0037.

Nothing here is typed by hand: every value comes from `pdftotext -bbox` output of the cited page (the reading itself
is in tools/iran/official_calendar_pdf.py). The tool fails loudly on an unreadable page, a checksum that does not
match the manifest, or a table that does not run day by day.

Usage: tools/iran/official_calendar_import.py [--check]
"""

import datetime
import json
import pathlib
import re
import sys

from official_calendar_pdf import (
    COLUMNS,
    HIJRI_MONTH_NAMES,
    MANIFEST,
    REPO,
    SOURCES,
    URL,
    Reader,
    manifest_supplied_on,
    page_count,
    verified,
)

DAYS_DIRECTORY = "core/calendar/src/test/resources/golden/persian"
MONTHS_DIRECTORY = "core/calendar/src/test/resources/golden/islamic-iran"
INDEX = f"{MONTHS_DIRECTORY}/official-calendars.csv"
MONTH_STARTS = f"{MONTHS_DIRECTORY}/official-month-starts.csv"
OVERRIDES = "dataset/iran/islamic-iran-overrides.json"

PERSIAN_MONTH_LENGTHS = [31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30]
LEAP_YEARS = "core/calendar/src/test/resources/golden/persian/official-leap-years-1206-1498.csv"


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


def follows(previous, row):
    """Whether the lunar Hijri date of [row] is the day after [previous]'s."""
    if row["hijri_day"] == previous["hijri_day"] + 1:
        return row["hijri_month"] == previous["hijri_month"] and row["hijri_year"] == previous["hijri_year"]
    if row["hijri_day"] != 1 or previous["hijri_day"] not in (29, 30):
        return False
    if previous["hijri_month"] == 12:
        return row["hijri_month"] == 1 and row["hijri_year"] == previous["hijri_year"] + 1
    return row["hijri_month"] == previous["hijri_month"] + 1 and row["hijri_year"] == previous["hijri_year"]


def month_starts(readers):
    """Every lunar Hijri month the imported calendars establish, earliest first, with its length where printed."""
    rows = [dict(row, solar_year=reader.solar_year) for reader in readers for row in reader.rows]
    rows.sort(key=lambda row: row["gregorian"])
    for left, right in zip(rows, rows[1:]):
        if (right["gregorian"] - left["gregorian"]).days != 1:
            raise SystemExit(f"the imported calendars leave a gap between {left['gregorian']} and {right['gregorian']}")
    starts = [start_of(rows[0])] if rows[0]["hijri_day"] != 1 else []
    starts += [start_of(row) for row in rows if row["hijri_day"] == 1]
    for start, following in zip(starts, starts[1:] + [None]):
        length = (following["gregorian"] - start["gregorian"]).days if following else None
        if length is not None and length not in (29, 30):
            raise SystemExit(f"{start['hijri_year']}-{start['hijri_month']} would have {length} days")
        start["length"] = length
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
        "printed": None if back == 0 else {
            "hijri_day": row["hijri_day"],
            "persian_year": row["solar_year"],
            "persian_month": row["persian_month"],
            "persian_day": row["persian_day"],
        },
    }


def days_csv(reader, digest, retrieved):
    header = [
        f"# source: University of Tehran, Institute of Geophysics, Calendar Center — Official calendar of Iran "
        f"{reader.solar_year} SH ({pathlib.Path(reader.path).name}), daily table",
        f"# url: {URL}",
        f"# retrieved: {retrieved}",
        f"# page: {reader.rows[0]['page']}-{reader.rows[-1]['page']}",
        "# reviewer: pending",
        f"# notes: owner-supplied PDF sha256={digest}; extracted from the cited pages with pdftotext -bbox by "
        f"tools/iran/official_calendar_import.py, which checks that the printed weekday, the Solar Hijri day, the "
        f"lunar Hijri day and the Gregorian date all run day by day; nothing is typed by hand",
        "persian,weekday_iso,hijri_iran,gregorian,official_holiday,page",
    ]
    body = [
        f"{row['persian']},{row['weekday_iso']},"
        f"{row['hijri_year']}-{row['hijri_month']:02d}-{row['hijri_day']:02d},"
        f"{row['gregorian'].isoformat()},{str(row['official_holiday']).lower()},{row['page']}"
        for row in reader.rows
    ]
    return "\n".join(header + body) + "\n"


def index_csv(readers, digests, retrieved):
    header = [
        "# source: University of Tehran, Institute of Geophysics, Calendar Center — official calendars of Iran "
        "stored in docs/sources, one row per imported calendar",
        f"# url: {URL}",
        f"# retrieved: {retrieved}",
        "# reviewer: pending",
        "# notes: written by tools/iran/official_calendar_import.py so that a calendar added to docs/sources is "
        "picked up by the tests without editing them; days is the number of rows of the daily fixture named here",
        "solar_year,source_file,sha256,first_page,last_page,days,fixture",
    ]
    body = [
        f"{reader.solar_year},{reader.path},{digest},{reader.rows[0]['page']},{reader.rows[-1]['page']},"
        f"{len(reader.rows)},golden/persian/{pathlib.Path(days_path(reader.solar_year)).name}"
        for reader, digest in zip(readers, digests)
    ]
    return "\n".join(header + body) + "\n"


def month_starts_csv(starts, readers, digests, retrieved):
    files = ", ".join(f"{pathlib.Path(reader.path).name} sha256={digest}"
                      for reader, digest in zip(readers, digests))
    derived = [f"{start['hijri_year']}-{start['hijri_month']:02d}: {derived_note(start)}"
               for start in starts if start["printed"]]
    header = [
        "# source: University of Tehran, Institute of Geophysics, Calendar Center — first days of the Iranian "
        "official lunar Hijri months, read from the daily tables of the official calendars of Iran "
        f"({', '.join(str(reader.solar_year) for reader in readers)} SH)",
        f"# url: {URL}",
        f"# retrieved: {retrieved}",
        f"# page: {min(reader.rows[0]['page'] for reader in readers)}-"
        f"{max(reader.rows[-1]['page'] for reader in readers)}",
        "# reviewer: pending",
        f"# notes: {files}; derived by tools/iran/official_calendar_import.py from the daily fixtures it writes, "
        f"never typed; length_days is empty where the calendars do not print the whole month"
        + ("; " + "; ".join(derived) if derived else ""),
        "hijri_month,first_day_gregorian,first_day_persian,length_days",
    ]
    body = [
        f"{start['hijri_year']}-{start['hijri_month']:02d},{start['gregorian'].isoformat()},"
        f"{start['persian'][0]}-{start['persian'][1]:02d}-{start['persian'][2]:02d},"
        f"{start['length'] if start['length'] else ''}"
        for start in starts
    ]
    return "\n".join(header + body) + "\n"


def days_path(solar_year):
    return f"{DAYS_DIRECTORY}/iran-official-{solar_year}-days.csv"


HIJRI_MONTH_LATIN = ["Muharram", "Safar", "Rabi al-Awwal", "Rabi al-Thani", "Jumada al-Ula", "Jumada al-Akhira",
                     "Rajab", "Sha'ban", "Ramadan", "Shawwal", "Dhu al-Qa'da", "Dhu al-Hijja"]
PERSIAN_MONTH_LATIN = ["Farvardin", "Ordibehesht", "Khordad", "Tir", "Mordad", "Shahrivar",
                       "Mehr", "Aban", "Azar", "Dey", "Bahman", "Esfand"]


def overrides_json(starts, retrieved):
    """The optional official override of ADR-0037, one record per imported month start."""
    months = []
    for start in starts:
        source = start["solar_year"]
        citation = {
            "url": URL,
            "title": f"Official calendar of Iran {source} SH ({SOURCES}/Calendar-{source}.pdf)",
            "page": str(start["page"]),
            "retrieved": retrieved,
        }
        if start["printed"]:
            citation["note"] = derived_note(start)
        months.append({
            "hijriYear": start["hijri_year"],
            "hijriMonth": start["hijri_month"],
            "persianStart": {"year": start["persian"][0], "month": start["persian"][1], "day": start["persian"][2]},
            "citation": citation,
        })
    document = {"$schema": "../islamic-iran-overrides.v1.json", "schemaVersion": 1, "months": months}
    return json.dumps(document, ensure_ascii=False, indent=2) + "\n"


def derived_note(start):
    """Explains a month start the calendars do not print, in the words of the row it was counted back from."""
    printed, persian = start["printed"], start["persian"]
    return (f"Derived: the calendar prints {printed['hijri_day']} {HIJRI_MONTH_LATIN[start['hijri_month'] - 1]} "
            f"{start['hijri_year']} on {printed['persian_day']} "
            f"{PERSIAN_MONTH_LATIN[printed['persian_month'] - 1]} {printed['persian_year']}, so the month began "
            f"{printed['hijri_day'] - 1} days earlier ({persian[2]} {PERSIAN_MONTH_LATIN[persian[1] - 1]} "
            f"{persian[0]}).")


def calendars_in_sources():
    """Every `Calendar-<solar year>.pdf` stored in docs/sources, oldest first."""
    found = {}
    for pdf in sorted((REPO / SOURCES).glob("Calendar-*.pdf")):
        match = re.fullmatch(r"Calendar-(\d{3,4})\.pdf", pdf.name)
        if match is None:
            raise SystemExit(f"{pdf.name}: a calendar in {SOURCES} must be named Calendar-<solar year>.pdf")
        found[int(match.group(1))] = pdf.name
    if not found:
        raise SystemExit(f"no Calendar-<solar year>.pdf in {SOURCES}")
    return dict(sorted(found.items()))


def read_calendar(solar_year):
    path = f"{SOURCES}/Calendar-{solar_year}.pdf"
    digest = verified(path)
    reader = Reader(path, solar_year)
    for page in range(1, page_count(path) + 1):
        try:
            reader.read_page(page)
        except ValueError as error:
            raise SystemExit(f"{path} page {page}: {error}") from error
    check_days(reader)
    return reader, digest


def main():
    retrieved = manifest_supplied_on()
    calendars = calendars_in_sources()
    readers, digests = [], []
    for solar_year in calendars:
        reader, digest = read_calendar(solar_year)
        readers.append(reader)
        digests.append(digest)
    starts = month_starts(readers)
    outputs = {days_path(reader.solar_year): days_csv(reader, digest, retrieved)
               for reader, digest in zip(readers, digests)}
    outputs[INDEX] = index_csv(readers, digests, retrieved)
    outputs[MONTH_STARTS] = month_starts_csv(starts, readers, digests, retrieved)
    outputs[OVERRIDES] = overrides_json(starts, retrieved)
    stale = [path for path, text in outputs.items()
             if not (REPO / path).is_file() or (REPO / path).read_text(encoding="utf-8") != text]
    if "--check" in sys.argv[1:]:
        if stale:
            raise SystemExit(f"out of date: {', '.join(sorted(stale))}; rerun {pathlib.Path(__file__).name}")
        print(f"up to date: {len(readers)} calendars, {sum(len(r.rows) for r in readers)} days, "
              f"{len(starts)} lunar months")
        return
    for path, text in outputs.items():
        (REPO / path).parent.mkdir(parents=True, exist_ok=True)
        (REPO / path).write_text(text, encoding="utf-8")
    for reader in readers:
        print(f"{reader.solar_year} SH: {len(reader.rows)} days from {reader.path} -> "
              f"{days_path(reader.solar_year)}")
    print(f"{len(starts)} lunar months -> {MONTH_STARTS}, {INDEX}, {OVERRIDES}")
    print(f"changed: {', '.join(sorted(stale)) if stale else 'nothing'}")


if __name__ == "__main__":
    main()
