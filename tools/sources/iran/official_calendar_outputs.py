#!/usr/bin/env python3
# Copyright (c) 2026 Saman Sohani. All Rights Reserved.
# Proprietary and confidential. See the LICENSE file in the repository root.
"""The files the official-calendar importer writes, as text. Every file starts with the golden-fixture provenance
header (source, url, retrieved, page, reviewer, notes) and is fully determined by the imported rows.

Used by tools/sources/iran/official_calendar_import.py; not a command of its own.
"""

import json
import pathlib

from official_calendar_pdf import SOURCES, URL
from official_calendar_sources import ERRATA, LAYOUTS, OVERRIDE_YEARS, REJECTED

DAYS_DIRECTORY = "core/calendar/src/test/resources/golden/persian/official"
MONTHS_DIRECTORY = "core/calendar/src/test/resources/golden/islamic-iran"
INDEX = f"{MONTHS_DIRECTORY}/official-calendars.csv"
MONTH_STARTS = f"{MONTHS_DIRECTORY}/official-month-starts.csv"
HISTORY = f"{MONTHS_DIRECTORY}/official-month-starts-1381-1405.csv"
OVERRIDES = "dataset/iran/islamic-iran-overrides.json"
HOLIDAYS_DIRECTORY = "tools/dataset/src/test/resources/golden/iran"
TOOL = "tools/sources/iran/official_calendar_import.py"
PUBLISHER = "University of Tehran, Institute of Geophysics, Calendar Center"

HIJRI_MONTH_LATIN = ["Muharram", "Safar", "Rabi al-Awwal", "Rabi al-Thani", "Jumada al-Ula", "Jumada al-Akhira",
                     "Rajab", "Sha'ban", "Ramadan", "Shawwal", "Dhu al-Qa'da", "Dhu al-Hijja"]
PERSIAN_MONTH_LATIN = ["Farvardin", "Ordibehesht", "Khordad", "Tir", "Mordad", "Shahrivar",
                       "Mehr", "Aban", "Azar", "Dey", "Bahman", "Esfand"]


def header(source, retrieved, page, notes):
    return [f"# source: {PUBLISHER} — {source}", f"# url: {URL}", f"# retrieved: {retrieved}",
            *([f"# page: {page}"] if page else []), "# reviewer: pending", f"# notes: {notes}"]


def lunar(row):
    return f"{row['hijri_year']}-{row['hijri_month']:02d}-{row['hijri_day']:02d}"


def printed_text(text):
    """An occasion as one CSV cell: the Latin comma, which the fixtures split on, becomes the Persian one."""
    return text.replace(",", "،").replace('"', "")


def errata_note(solar_year):
    return "".join(f"; erratum corrected from the document itself: {erratum.reason}"
                   for erratum in ERRATA.get(solar_year, ()))


def days_csv(reader, digest, retrieved):
    lines = header(
        f"Official calendar of Iran {reader.solar_year} SH ({pathlib.Path(reader.path).name}), daily table",
        retrieved, f"{reader.rows[0]['page']}-{reader.rows[-1]['page']}",
        f"owner-supplied PDF sha256={digest}, layout {LAYOUTS[reader.solar_year]}; extracted from the cited pages with "
        f"pdftotext -bbox by {TOOL}, which checks that the printed weekday, the Solar Hijri day, the lunar Hijri day "
        f"and the Gregorian date all run day by day; nothing is typed by hand; hijri_iran is the lunar date as "
        f"printed; occasion is the occasion text as printed (Latin commas turned into Persian ones)"
        + errata_note(reader.solar_year),
    ) + ["persian,weekday_iso,hijri_iran,gregorian,official_holiday,page,occasion"]
    lines += [f"{row['persian']},{row['weekday_iso']},{lunar(row)},{row['gregorian'].isoformat()},"
              f"{str(row['official_holiday']).lower()},{row['page']},{printed_text(row['occasion'])}"
              for row in reader.rows]
    return "\n".join(lines) + "\n"


def holidays_csv(reader, digest, retrieved, notice):
    holidays = [row for row in reader.rows if row["official_holiday"]]
    announcement = "".join(
        f"; the calendar's notice (page {page}) says {HIJRI_MONTH_LATIN[month - 1]} began one day "
        f"{'later' if shift > 0 else 'earlier'} by official announcement, so holidays in and after that month may "
        f"have moved with it; the dates here are the printed ones"
        for month, (shift, page) in sorted((notice or {}).items()))
    lines = header(
        f"Official calendar of Iran {reader.solar_year} SH ({pathlib.Path(reader.path).name}), days marked (تعطیل)",
        retrieved, f"{holidays[0]['page']}-{holidays[-1]['page']}" if holidays else None,
        f"{SOURCES}/{pathlib.Path(reader.path).name} sha256={digest}; one row per official holiday date, written by "
        f"{TOOL}; occasion is the text printed with the day; curated titles are in "
        f"dataset/iran/iran-official-holidays.json" + announcement,
    ) + ["persian,gregorian,hijri_iran,page,occasion"]
    lines += [f"{row['persian']},{row['gregorian'].isoformat()},{lunar(row)},{row['page']},"
              f"{printed_text(row['occasion'])}" for row in holidays]
    return "\n".join(lines) + "\n"


def index_csv(readers, digests, retrieved):
    rejected = "; ".join(f"{year} not imported: {reason}" for year, reason in sorted(REJECTED.items()))
    lines = header(
        f"official calendars of Iran stored in {SOURCES}, one row per imported calendar", retrieved, None,
        f"written by {TOOL} so that a calendar added to {SOURCES} is picked up by the tests without editing them; "
        f"days is the number of rows of the daily fixture named here; override is true for the calendars whose lunar "
        f"months make up the optional official override (ADR-0037); {rejected}",
    ) + ["solar_year,source_file,sha256,first_page,last_page,days,fixture,override"]
    lines += [f"{reader.solar_year},{reader.path},{digest},{reader.rows[0]['page']},{reader.rows[-1]['page']},"
              f"{len(reader.rows)},golden/persian/official/{reader.solar_year}.csv,"
              f"{str(reader.solar_year in OVERRIDE_YEARS).lower()}"
              for reader, digest in zip(readers, digests)]
    return "\n".join(lines) + "\n"


def persian_iso(persian):
    return f"{persian[0]}-{persian[1]:02d}-{persian[2]:02d}"


def history_csv(starts, readers, digests, retrieved):
    files = ", ".join(f"{pathlib.Path(reader.path).name} sha256={digest}" for reader, digest in zip(readers, digests))
    rejected = ", ".join(str(year) for year in sorted(REJECTED))
    lines = header(
        "first days of the Iranian official lunar Hijri months, read from the daily tables of the official calendars "
        f"of Iran {readers[0].solar_year}–{readers[-1].solar_year} SH", retrieved, None,
        f"{files}; derived by {TOOL} from the daily fixtures it writes, never typed. basis: printed = the calendar "
        "prints day 1 of the month; derived = the first imported day of a run is counted back to its day 1; "
        "announced = the calendar's notice says the month began a day later or earlier by official announcement than "
        "its computed table shows, and the month is moved by that day (the month before it gains or loses the day; "
        "the announced month's own length is left empty, since the notice does not say when it ended). "
        "length_days is empty where the calendars do not print the whole month, including the months on either "
        f"side of the years not imported ({rejected}, see official-calendars.csv). The calendars are the Calendar "
        "Center's computed calendars as published before each year; they are the official calendar, not a record "
        "of later sighting announcements, except where a notice says so",
    ) + ["hijri_month,first_day_gregorian,first_day_persian,length_days,basis,solar_year,page"]
    lines += [f"{start['hijri_year']}-{start['hijri_month']:02d},{start['gregorian'].isoformat()},"
              f"{persian_iso(start['persian'])},{start['length'] or ''},{start['basis']},{start['solar_year']},"
              f"{start['announcement']['page'] if start['announcement'] else start['page']}"
              for start in starts]
    return "\n".join(lines) + "\n"


def month_starts_csv(starts, readers, retrieved):
    derived = [f"{start['hijri_year']}-{start['hijri_month']:02d}: {derived_note(start)}"
               for start in starts if start["printed"]]
    lines = header(
        "first days of the Iranian official lunar Hijri months, read from the daily tables of the official calendars "
        f"of Iran ({', '.join(str(reader.solar_year) for reader in readers)} SH)", retrieved,
        f"{min(reader.rows[0]['page'] for reader in readers)}-{max(reader.rows[-1]['page'] for reader in readers)}",
        f"the months of the optional official override (ADR-0037); derived by {TOOL} from the daily fixtures it "
        "writes, never typed; length_days is empty where the calendars do not print the whole month"
        + ("; " + "; ".join(derived) if derived else ""),
    ) + ["hijri_month,first_day_gregorian,first_day_persian,length_days"]
    lines += [f"{start['hijri_year']}-{start['hijri_month']:02d},{start['gregorian'].isoformat()},"
              f"{persian_iso(start['persian'])},{start['length'] or ''}" for start in starts]
    return "\n".join(lines) + "\n"


def overrides_json(starts, retrieved):
    """The optional official override of ADR-0037, one record per month start of the override calendars."""
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
