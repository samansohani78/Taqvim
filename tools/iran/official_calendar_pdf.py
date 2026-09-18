#!/usr/bin/env python3
# Copyright (c) 2026 Saman Sohani. All Rights Reserved.
# Proprietary and confidential. See the LICENSE file in the repository root.
"""Reads the daily table of a Calendar Center yearly calendar (`docs/sources/Calendar-<solar year>.pdf`).

`pdftotext -bbox` gives every word with its box. Each printed day row is anchored on its weekday cell, the rightmost
column of the table; the remaining cells are read right to left until all of [COLUMNS] are found, and ditto marks
carry the value of the row above. A word arrives with its letters in visual order, so [normalise] reverses it; a
lam-alef ligature, which poppler expands in logical order inside that visual run, therefore arrives with its two
letters swapped, and [name_key] compares names on a key that sorts every run of alef and lam.

Used by tools/iran/official_calendar_import.py; not a command of its own.
"""

import datetime
import hashlib
import pathlib
import re
import subprocess
import unicodedata

REPO = pathlib.Path(__file__).resolve().parents[2]
MANIFEST = "docs/sources/MANIFEST.md"
SOURCES = "docs/sources"
URL = "https://calendar.ut.ac.ir/Fa/"

WORD = re.compile(
    r'<word xMin="([\d.-]+)" yMin="([\d.-]+)" xMax="([\d.-]+)" yMax="([\d.-]+)">(.*)</word>'
)
LETTERS = str.maketrans({"ك": "ک", "ي": "ی", "ى": "ی", "ۀ": "ه", "ة": "ه", "ـ": None, "‌": None})
DIGITS = str.maketrans("٠١٢٣٤٥٦٧٨٩۰۱۲۳۴۵۶۷۸۹", "01234567890123456789")
INVISIBLE = re.compile("[​-‏‪-‮⁦-⁩]")

# Month and weekday names as the calendars print them. `pdftotext -bbox` reads a page in visual order, so a word
# comes out with its letters reversed; reversing it back restores the name except that a lam-alef ligature, which
# poppler expands in logical order inside the visual run, swaps its two letters. [name_key] therefore sorts every
# run of alef and lam, which makes the two orders equal and still tells the printed names apart. The 1405 calendar's
# text layer also drops the ayn of ربیع, so that spelling is listed as well. Anything not listed here fails loudly.
PERSIAN_MONTH_NAMES = ["فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
                       "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"]
HIJRI_MONTH_NAMES = [
    ["محرم"], ["صفر"], ["ربیعالاول", "ربیالاول"], ["ربیعالثانی", "ربیالثانی", "ربیعالاخر"],
    ["جمادیالاولی", "جمادیالاول"], ["جمادیالثانیه", "جمادیالثانی", "جمادیالاخر"],
    ["رجب"], ["شعبان"], ["رمضان"], ["شوال"], ["ذیالقعده", "ذیقعده"], ["ذیالحجه", "ذیحجه"],
]
GREGORIAN_MONTH_NAMES = [
    ["ژانویه"], ["فوریه"], ["مارس"], ["آوریل"], ["مه", "می"], ["ژوئن"],
    ["ژوئیه", "ژوییه"], ["اوت"], ["سپتامبر"], ["اکتبر"], ["نوامبر"], ["دسامبر"],
]
WEEKDAY_NAMES = [["دوشنبه"], ["سهشنبه"], ["چهارشنبه"], ["پنجشنبه"], ["جمعه"], ["شنبه"], ["یکشنبه"]]
HOLIDAY = "تعطیل"
DITTO = '"'
INFINITY = float("inf")

COLUMNS = ["persian_day", "persian_month", "hijri_day", "hijri_month", "hijri_year",
           "gregorian_day", "gregorian_month", "gregorian_year"]
CELL = re.compile(r'\d+|"|[^\d\s"]+')
COLUMN_GAP = 6.0
LINE_TOLERANCE = 3.0


LATIN_RUN = re.compile(r"[0-9A-Za-z]+")
ENTITIES = {"&quot;": DITTO, "&amp;": "&", "&lt;": "<", "&gt;": ">", "&apos;": "'"}


def normalise(text):
    """Logical-order, normalised form of one `pdftotext -bbox` word: its letters come out in visual order."""
    plain = text
    for entity, character in ENTITIES.items():
        plain = plain.replace(entity, character)
    plain = INVISIBLE.sub("", plain)
    # Reversing the word restores the Arabic letters; digit and Latin runs are already logical, so they flip back.
    # It happens before NFKC so that a lam-alef ligature, still one character here, expands in the right order.
    logical = LATIN_RUN.sub(lambda run: run.group(0)[::-1], plain[::-1])
    return unicodedata.normalize("NFKC", logical).translate(LETTERS).translate(DIGITS).strip()


def manifest_digest(name):
    pattern = re.compile(r"^\| `" + re.escape(name) + r"` \|[^|]*\|[^|]*\| `([0-9a-f]{64})`")
    for line in (REPO / MANIFEST).read_text(encoding="utf-8").splitlines():
        match = pattern.match(line)
        if match:
            return match.group(1)
    raise SystemExit(f"{name}: no SHA-256 row in {MANIFEST}. Add this row to the table there, in file-name order, "
                     f"with the content description filled in:\n{manifest_row(f'{SOURCES}/{name}')}")


def manifest_row(path):
    """The manifest row of the document at [path], so a new document is never described by hand."""
    document = REPO / path
    digest = hashlib.sha256(document.read_bytes()).hexdigest()
    return (f"| `{document.name}` | {page_count(path)} | {document.stat().st_size} | `{digest}` | "
            "Official calendar of Iran for <year> SH (occasions approved by the Public Culture Council; compiled by "
            "the Calendar Center Council, Institute of Geophysics, University of Tehran). Daily Solar Hijri / lunar "
            "Hijri / Gregorian table with official holidays. |")


def manifest_supplied_on():
    match = re.search(r"supplied by the repository owner on (\d{4}-\d{2}-\d{2})",
                      (REPO / MANIFEST).read_text(encoding="utf-8"))
    if match is None:
        raise SystemExit(f"{MANIFEST}: no 'supplied by the repository owner on <date>' line")
    return match.group(1)


def verified(path):
    digest = hashlib.sha256((REPO / path).read_bytes()).hexdigest()
    expected = manifest_digest(pathlib.Path(path).name)
    if digest != expected:
        raise SystemExit(f"{path}: sha256 {digest} does not match {MANIFEST} ({expected})")
    return digest


def page_count(path):
    output = subprocess.run(["pdfinfo", str(REPO / path)], check=True, capture_output=True, text=True).stdout
    match = re.search(r"^Pages:\s+(\d+)$", output, re.MULTILINE)
    if match is None:
        raise SystemExit(f"{path}: pdfinfo printed no page count")
    return int(match.group(1))


def page_words(path, page):
    raw = subprocess.run(
        ["pdftotext", "-bbox", "-f", str(page), "-l", str(page), str(REPO / path), "-"],
        check=True, capture_output=True, text=True,
    ).stdout
    words = []
    for match in WORD.finditer(raw):
        x_min, y_min, x_max, y_max = (float(match.group(index)) for index in range(1, 5))
        text = normalise(match.group(5))
        if text:
            words.append({"x0": x_min, "x1": x_max, "y": y_min, "text": text})
    return words


def groups_of(words):
    """Printed cells of one row, right to left: words closer than [COLUMN_GAP] belong to the same cell."""
    groups = []
    for word in sorted(words, key=lambda word: word["x1"], reverse=True):
        if groups and groups[-1]["x0"] - word["x1"] <= COLUMN_GAP:
            groups[-1]["x0"] = word["x0"]
            groups[-1]["text"] += word["text"]
        else:
            groups.append({"x0": word["x0"], "x1": word["x1"], "text": word["text"]})
    return groups


def weekday_cells(words):
    """The weekday cell of every printed day row, top to bottom: the rightmost column of the daily table."""
    lines = []
    for word in sorted(words, key=lambda word: (word["y"], word["x0"])):
        if lines and abs(word["y"] - lines[-1][0]["y"]) <= LINE_TOLERANCE:
            lines[-1].append(word)
        else:
            lines.append([word])
    cells = []
    for line in lines:
        for group in groups_of(line):
            if name_key(group["text"]) in WEEKDAYS:
                cells.append({"y": line[0]["y"], "x0": group["x0"], "text": group["text"]})
    return sorted(cells, key=lambda cell: cell["y"])


def bands_of(cells):
    """Vertical band of every day row: from halfway to the row above to halfway to the row below."""
    if len(cells) < 2:
        return [(-INFINITY, INFINITY)] * len(cells)
    spacing = sorted(right["y"] - left["y"] for left, right in zip(cells, cells[1:]))[len(cells) // 2]
    edges = [(left["y"] + right["y"]) / 2 for left, right in zip(cells, cells[1:])]
    tops = [cells[0]["y"] - spacing / 2] + edges
    bottoms = edges + [cells[-1]["y"] + spacing / 2]
    return list(zip(tops, bottoms))


def day_rows(words):
    """One entry per day of the printed table: its weekday, its [COLUMNS] cells and the occasions printed with it."""
    cells = weekday_cells(words)
    rows = []
    for (top, bottom), weekday in zip(bands_of(cells), cells):
        inside = [word for word in words if top <= word["y"] < bottom and word["x1"] <= weekday["x0"]]
        row_cells, table_left = table_cells(groups_of(inside), weekday["y"])
        occasion = " ".join(word["text"] for word in inside if word["x1"] < table_left)
        rows.append({"weekday": weekday["text"], "cells": row_cells, "occasion": occasion})
    return rows


def table_cells(groups, y):
    """The [COLUMNS] cells of one day row, read right to left until all of them are found."""
    tokens, table_left = [], INFINITY
    for group in groups:
        if len(tokens) >= len(COLUMNS):
            break
        tokens += CELL.findall(group["text"])
        table_left = group["x0"]
    if len(tokens) != len(COLUMNS):
        raise ValueError(f"row at {y}: expected {len(COLUMNS)} printed cells, found {tokens!r}")
    return dict(zip(COLUMNS, tokens)), table_left


ALEF_LAM = re.compile(r"[اآأإل]+")


def name_key(name):
    """Comparison key of a printed name: alef/lam runs sorted, so a swapped lam-alef ligature still matches."""
    cleaned = name.translate(LETTERS).replace(" ", "")
    return ALEF_LAM.sub(lambda run: "".join(sorted(run.group(0))), cleaned)


def lookup(spellings):
    """Maps the key of every accepted spelling to its 1-based index."""
    table = {}
    for index, names in enumerate(spellings, 1):
        for name in [names] if isinstance(names, str) else names:
            table[name_key(name)] = index
    return table


PERSIAN_MONTHS = lookup(PERSIAN_MONTH_NAMES)
HIJRI_MONTHS = lookup(HIJRI_MONTH_NAMES)
GREGORIAN_MONTHS = lookup(GREGORIAN_MONTH_NAMES)
WEEKDAYS = lookup(WEEKDAY_NAMES)


def index_of(name, table, what):
    key = name_key(name)
    if key not in table:
        raise ValueError(f"unknown {what} name {name!r}")
    return table[key]


def cell(value, previous, what):
    """Resolves a ditto cell against the value of the row above."""
    if value == DITTO:
        if previous is None:
            raise ValueError(f"{what}: a ditto mark with nothing above it")
        return previous
    return value


class Reader:
    """Reads the daily table of one calendar, carrying the ditto state from row to row and page to page."""

    def __init__(self, path, solar_year):
        self.path = path
        self.solar_year = solar_year
        self.previous = {column: None for column in COLUMNS}
        self.rows = []

    def read_page(self, page):
        rows = day_rows(page_words(self.path, page))
        for row in rows:
            self.add(page, row)
        return len(rows)

    def add(self, page, row):
        resolved = {column: cell(row["cells"][column], self.previous[column], column) for column in COLUMNS}
        self.previous = resolved
        gregorian = datetime.date(
            int(resolved["gregorian_year"]),
            index_of(resolved["gregorian_month"], GREGORIAN_MONTHS, "Gregorian month"),
            int(resolved["gregorian_day"]),
        )
        persian_month = index_of(resolved["persian_month"], PERSIAN_MONTHS, "Solar Hijri month")
        hijri_month = index_of(resolved["hijri_month"], HIJRI_MONTHS, "lunar Hijri month")
        weekday_iso = index_of(row["weekday"], WEEKDAYS, "weekday")
        if weekday_iso != gregorian.isoweekday():
            raise ValueError(f"{self.path} page {page}: {gregorian} is printed on {row['weekday']}")
        self.rows.append({
            "persian": f"{self.solar_year}-{persian_month:02d}-{int(resolved['persian_day']):02d}",
            "persian_month": persian_month,
            "persian_day": int(resolved["persian_day"]),
            "weekday_iso": weekday_iso,
            "hijri_year": int(resolved["hijri_year"]),
            "hijri_month": hijri_month,
            "hijri_day": int(resolved["hijri_day"]),
            "gregorian": gregorian,
            "official_holiday": HOLIDAY in name_key(row["occasion"]),
            "page": page,
        })


