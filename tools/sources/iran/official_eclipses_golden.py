#!/usr/bin/env python3
# Copyright (c) 2026 Saman Sohani. All Rights Reserved.
# Proprietary and confidential. See the LICENSE file in the repository root.
"""Generates core/astronomy's golden/iran/official-eclipses-1399-1405.csv and iran-provincial-capitals-1405.csv (T-403).

Usage: tools/sources/iran/official_eclipses_golden.py [--check] (run from anywhere; paths are relative to the repository root)

Sources are the owner-supplied Calendar Center documents in docs/sources, each checked against the SHA-256 in
docs/sources/iran/MANIFEST.md before use:
- Calendar-1404.pdf page 2 (the eclipse visible in Iran in 1404, with contact times) and page 3 (eclipses of 1404 not
  visible in Iran);
- Calendar-1405.pdf page 2 (eclipses of 1405 not visible in Iran, plus the section it keeps for 1399);
- the Calendar Center's notice on the lunar eclipse of 16 Shahrivar 1404, whose times must equal the calendar's.
Text comes from `pdftotext -raw`, which keeps each line but not always the right-to-left word order, so every field is
read from the words of the item's lines rather than from phrases. Unicode presentation forms, tatweel and Arabic
letter variants are normalised and digits converted, nothing else. The run fails unless each section yields exactly
the expected number of records, so no value is typed by hand.

The site list takes the coordinates each official 1405 prayer-time document states for its provincial capital, from
the headers of core/praytimes' golden/iran-prayer-times-1405 fixtures (which cite and checksum those PDFs).
"""
import hashlib
import pathlib
import re
import subprocess
import sys
import unicodedata

from official_calendar_pdf import MANIFEST, manifest_digest

REPO = pathlib.Path(__file__).resolve().parents[3]
CALENDAR_1404 = "docs/sources/iran/Calendar-1404.pdf"
CALENDAR_1405 = "docs/sources/iran/Calendar-1405.pdf"
NOTICE = "docs/sources/iran/اطلاعیه ماه گرفتگی 16 شهریور 1404.pdf"
ECLIPSES = "core/astronomy/src/test/resources/golden/iran/official-eclipses-1399-1405.csv"
SITES = "core/astronomy/src/test/resources/golden/iran/iran-provincial-capitals-1405.csv"
PRAYER_GOLDENS = "core/praytimes/src/test/resources/golden/iran-prayer-times-1405"

MONTHS = ["فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"]
# Longest names first so that "شنبه" does not match inside the others; the test re-checks every weekday.
WEEKDAYS = [
    ("چهارشنبه", "WEDNESDAY"), ("پنجشنبه", "THURSDAY"), ("سهشنبه", "TUESDAY"), ("یکشنبه", "SUNDAY"),
    ("دوشنبه", "MONDAY"), ("جمعه", "FRIDAY"), ("شنبه", "SATURDAY"),
]
CONTACTS = ["partial_begin", "total_begin", "total_end", "partial_end"]
LETTERS = str.maketrans({"ك": "ک", "ي": "ی", "ى": "ی", "ۀ": "ه", "ة": "ه", "ـ": None})
DIGITS = str.maketrans("٠١٢٣٤٥٦٧٨٩۰۱۲۳۴۵۶۷۸۹", "01234567890123456789")
INVISIBLE = re.compile("[‌‍‎‏‪-‮⁦-⁩]")
ITEM_START = re.compile(r"^\s*(\d+)\s*-\s*")
TIME = re.compile(r"(?<!\d)(\d{1,2})\s+و\s+(\d{1,2})(?!\d)")


def verified(path):
    digest = hashlib.sha256((REPO / path).read_bytes()).hexdigest()
    expected = manifest_digest(pathlib.Path(path).name)
    if digest != expected:
        raise SystemExit(f"{path}: sha256 {digest} does not match {MANIFEST} ({expected})")
    return digest


def page_lines(path, page):
    raw = subprocess.run(
        ["pdftotext", "-raw", "-f", str(page), "-l", str(page), str(REPO / path), "-"],
        check=True, capture_output=True, text=True,
    ).stdout
    text = INVISIBLE.sub("", unicodedata.normalize("NFKC", raw)).translate(LETTERS).translate(DIGITS)
    return [line.strip() for line in text.splitlines() if line.strip()]


def squeeze(text):
    return re.sub(r"\s+", "", text)


def items(lines):
    """Numbered items of a section: each is the list of its lines, the first starting with "N -"."""
    found = []
    for line in lines:
        if ITEM_START.match(line):
            found.append([line])
        elif found:
            found[-1].append(line)
    return found


def one(values, what, item):
    if len(values) != 1:
        raise ValueError(f"expected one {what}, found {values!r} in {item!r}")
    return values[0]


def record(item, year, visible):
    first = ITEM_START.sub("", item[0])
    words = set(re.findall(r"[^\s:،.,()]+", first))
    flat = squeeze(first)
    body = "solar" if any("خورشیدگرفتگی" in word for word in words) else "lunar"
    if body == "lunar" and not {"ماه", "گرفتگی"} <= words and "ماهگرفتگی" not in flat:
        raise ValueError(f"no eclipse named in {item!r}")
    kinds = [kind for kind, test in [
        ("partial", any(word.startswith("جزی") for word in words)),
        ("annular", any("حلقوی" in word for word in words)),
        ("total", bool(words & {"کل", "کلی"})),
    ] if test]
    # Month names may be split across words ("ف ر وردین"), so they are matched in the squeezed line, except "دی", which
    # also occurs inside "فروردین" and must stand as a word.
    month = one([index for index, name in enumerate(MONTHS, 1)
                 if (name in words if len(name) < 3 else name in flat)], "month", item)
    weekday = next((code for name, code in WEEKDAYS if name in flat), None)
    day = one([int(number) for number in re.findall(r"(?<!\d)\d{1,2}(?!\d)", first)], "day", item)
    stated_invisible = any("غیر" in line.split() for line in item)
    if weekday is None or stated_invisible == visible:
        raise ValueError(f"weekday or visibility statement missing in {item!r}")
    return {"persian_date": f"{year}-{month:02d}-{day:02d}", "weekday": weekday, "body": body,
            "kind": one(kinds, "eclipse type", item), "visible": "true" if visible else "false"}


def calendar_contacts(item):
    """The four clock times of the visible lunar eclipse, one per line of Calendar-1404 page 2."""
    times = {}
    for line in item[1:]:
        match = TIME.search(line)
        if match is None:
            continue
        flat = squeeze(line)
        key = ("partial_" if "کلی" not in flat else "total_") + ("begin" if "آغاز" in flat else "end")
        if key in times or not ("آغاز" in flat or "پایان" in flat):
            raise ValueError(f"unexpected contact line {line!r}")
        times[key] = f"{int(match.group(1)):02d}:{int(match.group(2)):02d}"
    if sorted(times) != sorted(CONTACTS):
        raise ValueError(f"contacts incomplete: {times!r}")
    return times


def notice_contacts(lines):
    text = " ".join(lines)
    found = TIME.findall(text)
    if len(found) != len(CONTACTS) or "تمام" not in text.split() or "ایران" not in text.split():
        raise ValueError(f"notice: expected four times and the all-of-Iran statement, found {found!r}")
    return dict(zip(CONTACTS, (f"{int(hour):02d}:{int(minute):02d}" for hour, minute in found)))


def section(lines, year, visible, count, source):
    rows = [record(item, year, visible) for item in items(lines)]
    if len(rows) != count:
        raise ValueError(f"{source}: expected {count} records for {year}, found {len(rows)}")
    for row in rows:
        row.update(source=source, scope="Iran")
    return rows


def split_1405(lines):
    """Calendar-1405 page 2 holds a 1399 section followed by the 1405 section; each starts at its heading line."""
    start_1399 = one([i for i, line in enumerate(lines) if "1399" in line.split()], "1399 heading", lines)
    start_1405 = one([i for i, line in enumerate(lines) if i > start_1399 and "1405" in line.split()], "1405 heading",
                     lines)
    return lines[start_1399:start_1405], lines[start_1405:]


def eclipse_rows():
    page_1404_2, page_1404_3 = page_lines(CALENDAR_1404, 2), page_lines(CALENDAR_1404, 3)
    section_1399, section_1405 = split_1405(page_lines(CALENDAR_1405, 2))
    visible_items = items(page_1404_2)
    visible = section(page_1404_2, 1404, True, 1, f"{CALENDAR_1404}#page=2")
    times = calendar_contacts(one(visible_items, "visible eclipse", page_1404_2))
    if notice_contacts(page_lines(NOTICE, 1)) != times:
        raise ValueError("the notice and Calendar-1404 page 2 disagree")
    visible[0].update(times, source=f"{CALENDAR_1404}#page=2; {NOTICE}#page=1", scope="all of Iran")
    rows = visible
    rows += section(page_1404_3, 1404, False, 4, f"{CALENDAR_1404}#page=3")
    rows += section(section_1399, 1399, False, 1, f"{CALENDAR_1405}#page=2")
    rows += section(section_1405, 1405, False, 3, f"{CALENDAR_1405}#page=2")
    return sorted(rows, key=lambda row: row["persian_date"])


def site_rows():
    rows = []
    pattern = re.compile(r"(docs/sources/\S+\.pdf) sha256=([0-9a-f]{64}); coordinates stated in the document: "
                         r"latitude ([0-9.]+) N, longitude ([0-9.]+) E")
    for golden in sorted((REPO / PRAYER_GOLDENS).glob("*.csv")):
        match = next(filter(None, map(pattern.search, golden.read_text(encoding="utf-8").splitlines())), None)
        if match is None:
            raise SystemExit(f"{golden}: no cited coordinates in the header")
        if verified(match.group(1)) != match.group(2):
            raise SystemExit(f"{golden}: header checksum is stale")
        rows.append(f"{golden.stem},{match.group(3)},{match.group(4)},{match.group(1)}")
    if len(rows) != 31:
        raise SystemExit(f"expected 31 provincial capitals, found {len(rows)}")
    return rows


def eclipses_csv(rows, digests):
    columns = ["persian_date", "weekday", "body", "kind", "visible", "scope", *CONTACTS, "source"]
    header = [
        "# source: University of Tehran, Institute of Geophysics, Calendar Center — Official calendars of Iran 1404 and "
        "1405 SH (astronomical events pages) and the notice on the lunar eclipse of 16 Shahrivar 1404",
        "# url: https://calendar.ut.ac.ir/Fa/",
        "# retrieved: 2026-09-13",
        "# page: Calendar-1404.pdf 2-3; Calendar-1405.pdf 2; notice 1",
        "# reviewer: pending",
        f"# notes: {CALENDAR_1404} sha256={digests[0]}; {CALENDAR_1405} sha256={digests[1]}; {NOTICE} "
        f"sha256={digests[2]}; contact times are the documents' clock times, in official Iran time (UTC+03:30; the "
        "documents name no zone), to the minute; visible=false rows are stated as not visible in Iran; Calendar-1405 "
        "page 2 also keeps a section for 1399; generated by tools/sources/iran/official_eclipses_golden.py with pdftotext "
        "-raw, not typed",
        ",".join(columns),
    ]
    return "\n".join(header + [",".join(row.get(column, "") for column in columns) for row in rows]) + "\n"


def sites_csv(rows):
    header = [
        "# source: University of Tehran, Institute of Geophysics — Religious (prayer) times of the 31 provincial "
        "capitals for 1405 SH, coordinates stated in each document",
        "# url: https://calendar.ut.ac.ir/Fa/",
        "# retrieved: 2026-09-13",
        "# reviewer: pending",
        "# notes: copied from the cited headers of core/praytimes golden/iran-prayer-times-1405 (each PDF's sha256 "
        "re-checked against docs/sources/iran/MANIFEST.md); degrees north and east; generated by "
        "tools/sources/iran/official_eclipses_golden.py, not typed",
        "city,latitude,longitude,source_file",
    ]
    return "\n".join(header + rows) + "\n"


def main():
    digests = [verified(CALENDAR_1404), verified(CALENDAR_1405), verified(NOTICE)]
    eclipses = eclipse_rows()
    sites = site_rows()
    outputs = {ECLIPSES: eclipses_csv(eclipses, digests), SITES: sites_csv(sites)}
    if "--check" in sys.argv[1:]:
        stale = [path for path, text in outputs.items() if (REPO / path).read_text(encoding="utf-8") != text]
        if stale:
            raise SystemExit(f"out of date: {', '.join(stale)}; rerun tools/sources/iran/official_eclipses_golden.py")
        print("up to date")
        return
    for path, text in outputs.items():
        (REPO / path).write_text(text, encoding="utf-8")
    print(f"{len(eclipses)} eclipses; {ECLIPSES}")
    print(f"{len(sites)} sites; {SITES}")


if __name__ == "__main__":
    main()
