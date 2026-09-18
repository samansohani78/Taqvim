#!/usr/bin/env python3
# Copyright (c) 2026 Saman Sohani. All Rights Reserved.
# Proprietary and confidential. See the LICENSE file in the repository root.
"""Imports the Calendar Center's list of past Nowruz instants (`docs/sources/iran/tahvil-sal.txt`) into
`core/calendar/src/test/resources/golden/persian/official-nowruz-instants-1360-1403.csv`.

The list gives, per year, four lines: the Solar Hijri year (a leap year marked *), the weekday and Solar Hijri day
on which the instant falls, its Gregorian date, and the instant in Iran Standard Time ("ساعت 6 و 36 دقیقه و 26 ثانیه";
"54 دقیقه و 28 ثانیه بامداد" is 00:54:28). The tool checks that the weekday, the Solar Hijri day and the Gregorian date
of each entry name the same day and that the day is 29/30 Esfand or 1 Farvardin; it fails on anything it cannot read.

Usage: tools/sources/iran/nowruz_instants_import.py [--check]
"""

import datetime
import hashlib
import pathlib
import re
import sys
import unicodedata

from official_calendar_pdf import (
    GREGORIAN_MONTHS,
    INVISIBLE,
    LETTERS,
    PERSIAN_MONTHS,
    SOURCES,
    URL,
    WEEKDAYS,
    index_of,
    manifest_digest,
    supplied_on,
)

REPO = pathlib.Path(__file__).resolve().parents[3]
SOURCE = "tahvil-sal.txt"
GOLDEN = "core/calendar/src/test/resources/golden/persian/official-nowruz-instants-1360-1403.csv"
DIGITS = str.maketrans("٠١٢٣٤٥٦٧٨٩۰۱۲۳۴۵۶۷۸۹", "01234567890123456789")
IRAN_STANDARD_TIME = datetime.timezone(datetime.timedelta(hours=3, minutes=30))

YEAR = re.compile(r"^(\*?)(\d{4})(\*?)$")  # the list sets the leap mark on either side
DAY = re.compile(r"^(\D+?)\s*(\d{1,2}|اول)\s+(\D+?)\s*(\d{4})$")
GREGORIAN = re.compile(r"^(\d{1,2})\s*(\D+?)\s*(\d{4})$")
NUMBER = r"(\d{1,2}|صفر)"  # the list writes a zero minute or second as the word صفر
TIME = re.compile(rf"^(?:ساعت\s*{NUMBER}\s*و\s*)?{NUMBER}\s*دقیقه\s*و\s*{NUMBER}\s*ثان[یب]ه(\s*بامداد)?$")  # 1362 misspells ثانیه


def normalised(line):
    text = INVISIBLE.sub("", unicodedata.normalize("NFKC", line)).replace("‌", "").translate(LETTERS)
    return " ".join(text.translate(DIGITS).split())


def entries(text):
    """The four-line entries of the list, from its first year line on."""
    lines = [normalised(line) for line in text.splitlines() if line.strip()]
    first = next(index for index, line in enumerate(lines) if YEAR.match(line))
    body = lines[first:]
    if len(body) % 4:
        raise SystemExit(f"{SOURCE}: {len(body)} lines after the header, not a multiple of four")
    return [body[index:index + 4] for index in range(0, len(body), 4)]


def parsed(entry):
    year_line, day_line, gregorian_line, time_line = entry
    year, day, gregorian, time = (YEAR.match(year_line), DAY.match(day_line), GREGORIAN.match(gregorian_line),
                                  TIME.match(time_line))
    if not (year and day and gregorian):
        raise SystemExit(f"{SOURCE}: cannot read entry {entry!r}")
    persian_year = int(year.group(2))
    date = datetime.date(int(gregorian.group(3)), index_of(gregorian.group(2), GREGORIAN_MONTHS, "Gregorian month"),
                         int(gregorian.group(1)))
    weekday = index_of(day.group(1), WEEKDAYS, "weekday")
    persian_day = 1 if day.group(2) == "اول" else int(day.group(2))
    persian_month = index_of(day.group(3), PERSIAN_MONTHS, "Solar Hijri month")
    persian = (int(day.group(4)), persian_month, persian_day)
    if weekday != date.isoweekday():
        raise SystemExit(f"{SOURCE}: {persian_year}: {date} is printed on {day.group(1)}")
    if persian not in ((persian_year, 1, 1), (persian_year - 1, 12, 29), (persian_year - 1, 12, 30)):
        raise SystemExit(f"{SOURCE}: {persian_year}: the instant falls on {persian}, not on the eve or day of Nowruz")
    instant = None
    if time:
        if time.group(4) and time.group(1):
            raise SystemExit(f"{SOURCE}: {persian_year}: an hour and 'بامداد' together")
        hour, minute, second = (0 if value in (None, "صفر") else int(value) for value in time.group(1, 2, 3))
        instant = datetime.datetime(date.year, date.month, date.day, hour, minute, second, tzinfo=IRAN_STANDARD_TIME)
    return {"time_as_printed": None if time else time_line, "year": persian_year, "leap": bool(year.group(1) or year.group(3)), "instant": instant, "persian": persian,
            "gregorian": date, "weekday": weekday}


def golden_csv(rows, digest, retrieved):
    lines = [
        "# source: University of Tehran, Institute of Geophysics, Calendar Center — list of past Nowruz instants "
        f"('لحظه تحویل سال‌های گذشته', {SOURCES}/{SOURCE})",
        f"# url: {URL}",
        f"# retrieved: {retrieved}",
        "# reviewer: pending",
        f"# notes: owner-supplied text sha256={digest}; written by tools/sources/iran/nowruz_instants_import.py, which "
        "checks that each entry's weekday, Solar Hijri day and Gregorian date name the same day; instants in Iran "
        "Standard Time (+03:30); leap marks the years the list stars; instant_day is the day the instant falls on",
        "persian_year,leap,equinox_instant,instant_day_persian,instant_day_gregorian,weekday_iso",
    ]
    unread = "; ".join(f"{row['year']}: the time is printed as '{row['time_as_printed']}', which names no minute, "
                       "so its instant is left empty" for row in rows if row["instant"] is None)
    lines[4] += f"; {unread}" if unread else ""
    lines += [f"{row['year']},{'*' if row['leap'] else ''},{row['instant'].isoformat() if row['instant'] else ''},"
              f"{row['persian'][0]}-{row['persian'][1]:02d}-{row['persian'][2]:02d},{row['gregorian'].isoformat()},"
              f"{row['weekday']}" for row in sorted(rows, key=lambda row: row["year"])]
    return "\n".join(lines) + "\n"


def main():
    path = REPO / SOURCES / SOURCE
    digest = hashlib.sha256(path.read_bytes()).hexdigest()
    if digest != manifest_digest(SOURCE):
        raise SystemExit(f"{SOURCE}: sha256 {digest} does not match the manifest")
    rows = [parsed(entry) for entry in entries(path.read_text(encoding="utf-8"))]
    years = [row["year"] for row in rows]
    if len(set(years)) != len(years):
        raise SystemExit(f"{SOURCE}: a year is listed twice")
    text = golden_csv(rows, digest, supplied_on(SOURCE))
    current = REPO / GOLDEN
    if "--check" in sys.argv[1:]:
        if not current.is_file() or current.read_text(encoding="utf-8") != text:
            raise SystemExit(f"out of date: {GOLDEN}; rerun {pathlib.Path(__file__).name}")
        print(f"up to date: {len(rows)} Nowruz instants, {min(years)}–{max(years)}")
        return
    current.write_text(text, encoding="utf-8")
    print(f"{len(rows)} Nowruz instants ({min(years)}–{max(years)}) -> {GOLDEN}")


if __name__ == "__main__":
    main()
