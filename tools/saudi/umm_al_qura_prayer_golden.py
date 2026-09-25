#!/usr/bin/env python3
# Copyright (c) 2026 Saman Sohani. All Rights Reserved.
# Proprietary and confidential. See the LICENSE file in the repository root.
"""Generate the T-601/DT-011 Umm al-Qura prayer-time golden from the archived KACST timetables.

Reads docs/sources/saudi/umm-al-qura-prayer-tables.json (written by tools/saudi/umm_al_qura_prayer_tables.py from
the Internet Archive's copies of ummulqura.org.sa) and writes one CSV row per day and city in 24-hour local
standard time. The published tables use a 12-hour clock with no AM/PM marker: Fajr, sunrise and Dhuhr are morning
times, Asr, Maghrib and Isha afternoon ones, and a Dhuhr printed as 12:xx is already noon.

Run with --check to verify the committed golden still matches the archive.
"""
import argparse
import hashlib
import json
import pathlib

REPO = pathlib.Path(__file__).resolve().parents[2]
ARCHIVE = REPO / "docs/sources/saudi/umm-al-qura-prayer-tables.json"
SUMS = REPO / "docs/sources/saudi/SHA256SUMS"
GOLDEN = REPO / "core/praytimes/src/test/resources/golden/umm-al-qura-prayer-times/saudi-cities.csv"
SOURCE_FILE = "docs/sources/saudi/umm-al-qura-prayer-tables.json"
HEADER_LINES = 6

# The page's city names, in its own order, with the spelling the golden uses.
CITIES = {
    "مكة المكرمة": "Makkah", "المدينة المنورة": "Madinah", "الرياض": "Riyadh",
    "بريدة": "Buraydah", "الدمام": "Dammam", "أبها": "Abha", "تبوك": "Tabuk",
    "حائل": "Hail", "عرعر": "Arar", "جازان": "Jazan", "نجران": "Najran",
    "الباحة": "AlBaha", "سكاكا": "Sakaka",
}
MORNING = 3  # fajr, sunrise and dhuhr are printed as morning times; the rest are afternoon ones
NOON = 12


def to_24_hour(printed: str, afternoon: bool) -> str:
    """A printed 12-hour time as 24-hour local standard time."""
    hour, minute = (int(part) for part in printed.split(":"))
    if not 1 <= hour <= NOON or not 0 <= minute < 60:
        raise ValueError(f"not a clock time: {printed}")
    if afternoon and hour != NOON:
        hour += NOON
    return f"{hour:02d}:{minute:02d}"


def load() -> dict:
    """The archive, after checking it against the checksum committed beside it."""
    data = ARCHIVE.read_bytes()
    expected = {line.split()[1]: line.split()[0] for line in SUMS.read_text().splitlines() if line.strip()}
    digest = hashlib.sha256(data).hexdigest()
    if expected.get(ARCHIVE.name) != digest:
        raise SystemExit(f"{ARCHIVE.name}: checksum {digest} does not match {SUMS}")
    return json.loads(data)


def rows(archive: dict) -> list[str]:
    """One row per day and city: the six times and the capture they were read from."""
    out = []
    for date, day in sorted(archive["days"].items()):
        for arabic, city in CITIES.items():
            printed = day["rows"][arabic]
            if len(printed) != len(archive["columns"]):
                raise ValueError(f"{date} {city}: {len(printed)} times")
            times = [to_24_hour(value, afternoon=index >= MORNING) for index, value in enumerate(printed)]
            if times != sorted(times):
                raise ValueError(f"{date} {city}: times are out of order: {times}")
            out.append(",".join([date, city, *times, day["capture"]]))
    return out


def text() -> str:
    """The golden file's provenance header and body."""
    archive = load()
    body = rows(archive)
    days = sorted(archive["days"])
    return "\n".join([
        "# source: King Abdulaziz City for Science and Technology (KACST) — Umm al-Qura calendar site, daily prayer "
        "timetable of 13 Saudi cities on the front page of ummulqura.org.sa",
        "# url: http://www.ummulqura.org.sa/",
        "# retrieved: 2026-09-25",
        f"# page: {days[0]}..{days[-1]}",
        "# reviewer: pending",
        "# notes: KACST computes the prayer times the Kingdom publishes and the Umm al-Qura calendar itself, so "
        "these tables are the MAKKAH method's own authority. The site is unreachable from the development network "
        "and its current pages build the table in the browser, so every day here was read from an Internet Archive "
        "capture of the older server-rendered page; the capture column names it and page_sha256 is the SHA-256 of "
        "the bytes it was read from (capture URL: https://web.archive.org/web/<capture>id_/http://www.ummulqura."
        "org.sa/). The pages are cited rather than copied. Facts extracted into "
        f"{SOURCE_FILE} (sha256 in docs/sources/saudi/SHA256SUMS), from which "
        "tools/saudi/umm_al_qura_prayer_golden.py generates this file; nothing is typed by hand. Times are local "
        "standard time (UTC+3, no daylight saving) converted from the page's unmarked 12-hour clock, and are "
        "rounded to the minute by the authority. Only Makkah is compared against the app: KACST publishes no "
        "coordinates, and Makkah's are the ones the method is defined at (21.4225 N, 39.8262 E).",
        "gregorian,city,fajr,sunrise,dhuhr,asr,maghrib,isha,capture",
        *body,
    ]) + "\n"


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="verify the golden matches the archive")
    arguments = parser.parse_args()
    generated = text()
    count = len(generated.splitlines()) - HEADER_LINES
    if arguments.check:
        current = GOLDEN.read_text() if GOLDEN.exists() else ""
        if current != generated:
            raise SystemExit(f"{GOLDEN} is stale; regenerate with tools/saudi/umm_al_qura_prayer_golden.py")
        print(f"{GOLDEN.name} is up to date ({count} rows)")
    else:
        GOLDEN.parent.mkdir(parents=True, exist_ok=True)
        GOLDEN.write_text(generated)
        print(f"wrote {GOLDEN} ({count} rows)")


if __name__ == "__main__":
    main()
