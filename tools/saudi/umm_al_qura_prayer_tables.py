#!/usr/bin/env python3
# Copyright (c) 2026 Saman Sohani. All Rights Reserved.
# Proprietary and confidential. See the LICENSE file in the repository root.
"""Retrieve the Umm al-Qura prayer timetables from the Internet Archive and write the T-601/DT-011 archive.

ummulqura.org.sa is the calendar site of the King Abdulaziz City for Science and Technology (KACST), which computes
the Umm al-Qura calendar and the prayer times the Kingdom publishes. Each capture of its front page shows one day's
timetable for thirteen cities. The site is unreachable from the development network and its current pages render the
timetable in the browser, so only the Internet Archive's copies of the older server-rendered pages carry the numbers.

This script needs the network, so it never runs in CI. It writes docs/sources/saudi/umm-al-qura-prayer-tables.json:
the times as facts, with each capture's URL and the SHA-256 of the exact bytes they were read from. The pages
themselves are not committed (the owner's standing rule: cite a copyrighted page by URL and checksum, do not copy
it). tools/saudi/umm_al_qura_prayer_golden.py turns that archive into the golden the app is tested against.

    python3 tools/saudi/umm_al_qura_prayer_tables.py --cache /tmp/uq/pages
"""
import argparse
import concurrent.futures
import datetime
import hashlib
import html
import json
import pathlib
import re
import subprocess
import threading

REPO = pathlib.Path(__file__).resolve().parents[2]
ARCHIVE = REPO / "docs/sources/saudi/umm-al-qura-prayer-tables.json"
SITE = "http://www.ummulqura.org.sa/"
CDX = ("https://web.archive.org/cdx/search/cdx?url=ummulqura.org.sa&output=text"
       "&fl=timestamp,statuscode,digest&filter=statuscode:200&collapse=digest")

# The thirteen cities of the table, in the order the page prints them.
CITIES = [
    "مكة المكرمة", "المدينة المنورة", "الرياض", "بريدة", "الدمام", "أبها", "تبوك",
    "حائل", "عرعر", "جازان", "نجران", "الباحة", "سكاكا",
]
COLUMNS = 6  # fajr, sunrise, dhuhr, asr, maghrib, isha
ISHA_HEADER = "العشاء"
MONTHS = ["January", "February", "March", "April", "May", "June",
          "July", "August", "September", "October", "November", "December"]
RIYADH_OFFSET = datetime.timedelta(hours=3)
MINIMUM_PAGE_BYTES = 2000
ATTEMPTS = 3


def capture_url(timestamp: str) -> str:
    """The Internet Archive's raw (unrewritten) copy of the capture."""
    return f"https://web.archive.org/web/{timestamp}id_/{SITE}"


def cells(page: str) -> list[str]:
    """The page's visible text, one entry per markup boundary."""
    stripped = re.sub(r"<script.*?</script>", "", page, flags=re.S | re.I)
    return [c.strip() for c in html.unescape(re.sub(r"<[^>]+>", "|", stripped)).split("|") if c.strip()]


def gregorian_date(parts: list[str]) -> str | None:
    """The page's Gregorian date, read from the month NUMBER rather than its name.

    The site prints the name as months[number % 12], so every December is labelled "January ( 12 )" while its
    number, its day, its year and its Hijri date are all correct. Reading the name would move 31 days a year
    eleven months into the past.
    """
    for index, value in enumerate(parts):
        if value not in MONTHS or index < 2:
            continue
        month = year = day = None
        for part in parts[index + 1:index + 6]:
            if month is None and re.fullmatch(r"1[0-2]|[1-9]", part):
                month = int(part)
            if re.fullmatch(r"(19|20)\d\d", part):
                year = int(part)
                break
        for part in reversed(parts[max(index - 3, 0):index]):
            if re.fullmatch(r"\d{1,2}", part):
                day = int(part)
                break
        return f"{year:04d}-{month:02d}-{day:02d}" if year and month and day else None
    return None


def parse(page: str) -> dict | None:
    """The day and the thirteen cities' six times, or None when the page carries no timetable."""
    parts = cells(page)
    if ISHA_HEADER not in parts:
        return None
    date = gregorian_date(parts)
    if date is None:
        return None
    rest = parts[parts.index(ISHA_HEADER) + 1:]
    rows = {}
    for start in range(0, len(rest) - COLUMNS, COLUMNS + 1):
        name, times = rest[start], rest[start + 1:start + 1 + COLUMNS]
        if name in CITIES and all(re.fullmatch(r"\d{1,2}:\d{2}", t) for t in times):
            rows[name] = times
    return {"date": date, "rows": rows} if len(rows) == len(CITIES) else None


def shows_its_own_capture_day(date: str, timestamp: str) -> bool:
    """Whether the page shows the day it was captured on, in the Kingdom's own time zone.

    The archive replays a cached copy now and then, and such a page carries a date its capture cannot vouch for.
    A day either side is allowed: the site turns its table over at some hour of its own, which need not be midnight.
    """
    captured = datetime.datetime.strptime(timestamp, "%Y%m%d%H%M%S").replace(tzinfo=datetime.UTC)
    local = (captured + RIYADH_OFFSET).date()
    return abs((datetime.date.fromisoformat(date) - local).days) <= 1


def timestamps() -> list[str]:
    """Every capture of the front page the archive holds, oldest first."""
    listing = subprocess.run(["curl", "-s", "--max-time", "180", CDX], capture_output=True, check=True).stdout
    return sorted({line.split()[0] for line in listing.decode().strip().splitlines() if line.split()})


def download(stamps: list[str], cache: pathlib.Path) -> None:
    """Fetches every capture that is not cached yet, once each; a page without a timetable is not retried."""
    cache.mkdir(parents=True, exist_ok=True)
    empty = cache / "no-table"
    empty.mkdir(exist_ok=True)
    lock = threading.Lock()
    done = [0]

    def fetch(timestamp: str) -> None:
        if (cache / f"{timestamp}.html").exists() or (empty / timestamp).exists():
            return
        for attempt in range(ATTEMPTS):
            page = subprocess.run(["curl", "-sL", "--max-time", "70", capture_url(timestamp)],
                                  capture_output=True).stdout
            if len(page) < MINIMUM_PAGE_BYTES:
                continue
            if parse(page.decode("utf-8", "ignore")):
                (cache / f"{timestamp}.html").write_bytes(page)
            else:
                (empty / timestamp).write_text(str(len(page)))
            break
        with lock:
            done[0] += 1
            if done[0] % 50 == 0:
                print(f"fetched {done[0]}", flush=True)

    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
        list(pool.map(fetch, stamps))


def coverage(cache: pathlib.Path, listing: pathlib.Path | None) -> dict:
    """What became of every capture the archive lists, so the record can be audited without the network."""
    if listing is None or not listing.exists():
        return {}
    stamps = {line.split()[0] for line in listing.read_text(encoding="utf-8").strip().splitlines() if line.split()}
    retrieved = {path.stem for path in cache.glob("*.html")}
    without = {path.name for path in (cache / "no-table").glob("*")} if (cache / "no-table").is_dir() else set()
    return {
        "captures_listed": len(stamps),
        "captures_with_a_timetable": len(stamps & retrieved),
        "captures_without_a_timetable": len(stamps & without),
        "captures_not_retrieved": sorted(stamps - retrieved - without),
    }


def archive(cache: pathlib.Path, listing: pathlib.Path | None = None) -> dict:
    """The archive: one entry per day, from the earliest capture that shows it, with agreeing captures counted."""
    days: dict[str, dict] = {}
    rejected: list[str] = []
    for path in sorted(cache.glob("*.html")):
        timestamp = path.stem
        blob = path.read_bytes()
        record = parse(blob.decode("utf-8", "ignore"))
        if record is None:
            rejected.append(f"{timestamp}: no timetable")
            continue
        if not shows_its_own_capture_day(record["date"], timestamp):
            rejected.append(f"{timestamp}: shows {record['date']}, which its capture time cannot vouch for")
            continue
        seen = days.get(record["date"])
        if seen is None:
            days[record["date"]] = {"capture": timestamp, "url": capture_url(timestamp),
                                    "page_sha256": hashlib.sha256(blob).hexdigest(),
                                    "confirmations": 0, "rows": record["rows"]}
        elif seen["rows"] == record["rows"]:
            seen["confirmations"] += 1
        else:
            rejected.append(f"{timestamp}: disagrees with capture {seen['capture']} of the same day")
    return {
        "source": "King Abdulaziz City for Science and Technology (KACST), Umm al-Qura calendar site — daily prayer "
                  "timetable of thirteen Saudi cities on the front page",
        "site": SITE,
        "retrieved_from": "https://web.archive.org/",
        "cities": CITIES,
        "columns": ["fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"],
        "note": "Times are as printed: the morning columns are AM and the afternoon columns PM, on a 12-hour clock "
                "with no marker. The pages themselves are not committed; each day cites the capture it was read "
                "from and the SHA-256 of those bytes.",
        "coverage": coverage(cache, listing),
        "rejected": sorted(rejected),
        "days": {date: days[date] for date in sorted(days)},
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--cache", type=pathlib.Path, required=True, help="directory the captures are kept in")
    parser.add_argument("--offline", action="store_true", help="build from the cache without fetching")
    parser.add_argument("--listing", type=pathlib.Path, help="the saved CDX listing, recorded as coverage")
    arguments = parser.parse_args()
    if not arguments.offline:
        stamps = timestamps()
        if arguments.listing:
            arguments.listing.write_text("\n".join(stamps) + "\n", encoding="utf-8")
        download(stamps, arguments.cache)
    built = archive(arguments.cache, arguments.listing)
    ARCHIVE.parent.mkdir(parents=True, exist_ok=True)
    ARCHIVE.write_text(json.dumps(built, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    print(f"wrote {ARCHIVE} ({len(built['days'])} days, {len(built['rejected'])} captures rejected)")


if __name__ == "__main__":
    main()
