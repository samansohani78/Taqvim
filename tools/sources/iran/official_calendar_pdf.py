#!/usr/bin/env python3
# Copyright (c) 2026 Saman Sohani. All Rights Reserved.
# Proprietary and confidential. See the LICENSE file in the repository root.
"""Reads the daily table of a Calendar Center yearly calendar (`docs/sources/iran/Calendar-<solar year>.pdf`).

`pdftotext -bbox` gives every word with its box. Each printed day row is anchored on its weekday cell, the rightmost
column of the table; the remaining cells are read right to left until all of [COLUMNS] are found, and ditto marks
carry the value of the row above. A word arrives with its letters in visual order, so [normalise] reverses it; a
lam-alef ligature, which poppler expands in logical order inside that visual run, therefore arrives with its two
letters swapped, and [name_key] compares names on a key that sorts every run of alef and lam.

Used by tools/sources/iran/official_calendar_import.py; not a command of its own.
"""

import datetime
import hashlib
import pathlib
import re
import subprocess
import unicodedata

REPO = pathlib.Path(__file__).resolve().parents[3]
MANIFEST = "docs/sources/iran/MANIFEST.md"
SOURCES = "docs/sources/iran"
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
# The 1381–1386 editions print Wednesday as چهارشبه throughout (a typesetting slip, the same on every page).
WEEKDAY_NAMES = [["دوشنبه"], ["سهشنبه"], ["چهارشنبه", "چهارشبه"], ["پنجشنبه"], ["جمعه"], ["شنبه"], ["یکشنبه"]]
HOLIDAY = "تعطیل"
FOOTNOTE = "*"
DITTO = '"'
INFINITY = float("inf")

COLUMNS = ["persian_day", "persian_month", "hijri_day", "hijri_month", "hijri_year",
           "gregorian_day", "gregorian_month", "gregorian_year"]
CELL = re.compile(r'\d+|"|[^\d\s"]+')
COLUMN_GAP = 6.0
LINE_TOLERANCE = 3.0


LATIN_RUN = re.compile(r"[0-9A-Za-z٠-٩۰-۹]+")
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


def manifest_rows():
    """The rows of docs/sources/iran/MANIFEST.md, keyed by canonical file name."""
    rows, columns = {}, None
    for line in (REPO / MANIFEST).read_text(encoding="utf-8").splitlines():
        if not line.startswith("| "):
            continue
        cells = [cell.strip().strip("`") for cell in line.strip().strip("|").split("|")]
        if columns is None:
            columns = cells
        elif not set(cells[0]) <= {"-"}:
            rows[cells[0]] = dict(zip(columns, cells))
    return rows


def manifest_digest(name):
    row = manifest_rows().get(name)
    if row is None:
        raise SystemExit(f"{name}: no row in {MANIFEST}. Add one (see the columns there), for example\n"
                         f"{manifest_row(f'{SOURCES}/{name}')}")
    return row["SHA-256"]


def manifest_row(path):
    """A manifest row for the document at [path], so a new document's checksum and size are never typed by hand."""
    document = REPO / path
    digest = hashlib.sha256(document.read_bytes()).hexdigest()
    return (f"| `{document.name}` | `{document.name}` | `{digest}` | yearly-calendar | <year> | <layout> | "
            f"{page_count(path)} | {document.stat().st_size} | <date supplied> | <content> |")


def supplied_on(name):
    """The date the owner supplied [name], from the manifest's Supplied column."""
    manifest_digest(name)
    return manifest_rows()[name]["Supplied"]


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
            groups[-1]["words"].append(word)
        else:
            groups.append({"x0": word["x0"], "x1": word["x1"], "words": [word]})
    for group in groups:
        group["text"] = cell_text(group["words"])
    return groups


def cell_text(words):
    """Text of one cell from its words, right to left. A number the PDF splits into several words (2006 as "200" and
    "6", 26 as "2" and "6") is left to right, so a run of digit-only words is joined from its leftmost word on."""
    parts, digits = [], []
    for word in words:
        if word["text"].isdigit():
            digits.insert(0, word["text"])
            continue
        if digits:
            parts.append("".join(digits))
            digits = []
        parts.append(word["text"])
    if digits:
        parts.append("".join(digits))
    return "".join(parts)


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
            # The 1396 edition sets the Solar Hijri day so close to the weekday that both fall in one cell; the
            # weekday is then the cell's letters, and the day stays inside the row for [table_cells].
            letters = [word for word in group["words"] if not word["text"].isdigit()]
            name = cell_text(letters)
            if letters and name_key(name) in WEEKDAYS:
                cells.append({"y": line[0]["y"], "x0": min(word["x0"] for word in letters), "text": name})
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
        inside = [word for word in words if top <= word["y"] < bottom and word["x0"] < weekday["x0"]]
        row_cells, table_left = table_cells(groups_of(inside), weekday["y"])
        occasion = [word for word in inside if word["x1"] < table_left]
        rows.append({
            "weekday": weekday["text"],
            "cells": row_cells,
            "occasion": occasion_text(occasion),
            "footnote": any(FOOTNOTE in word["text"] for word in inside if word["x1"] >= table_left),
        })
    return rows


def occasion_text(words):
    """The occasions printed with a day, in reading order: line by line, each line right to left."""
    lines = []
    for word in sorted(words, key=lambda word: (word["y"], -word["x1"])):
        if lines and abs(word["y"] - lines[-1][0]["y"]) <= LINE_TOLERANCE:
            lines[-1].append(word)
        else:
            lines.append([word])
    text = " ".join(mirrored(" ".join(word["text"] for word in line)) for line in lines)
    return OPENING_SPACE.sub("(", CLOSING_SPACE.sub(")", " ".join(text.split())))


OPENING_SPACE = re.compile(r"\(\s+")
CLOSING_SPACE = re.compile(r"\s+\)")


def mirrored(line):
    """Older editions keep parentheses in their visual shape, so a line reads ")holiday(": its first bracket closes."""
    brackets = [character for character in line if character in "()"]
    if brackets and brackets[0] == ")":
        return line.translate(str.maketrans("()", ")("))
    return line


def table_cells(groups, y):
    """The [COLUMNS] cells of one day row, read right to left until all of them are found."""
    tokens, table_left = [], INFINITY
    for group in groups:
        if len(tokens) >= len(COLUMNS):
            break
        # A footnote mark (*) stands next to the lunar month it annotates; it is not a cell of its own.
        tokens += [token for token in CELL.findall(group["text"].replace(FOOTNOTE, " ")) if token.strip()]
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
    """Reads the daily table of one calendar, carrying the ditto state from row to row and page to page.

    A page is taken whole or not at all: a page that is not part of the daily table (a year overview, a list of
    occasions) fails to parse and leaves no rows behind; [skipped] records why. Whether the pages read make up the
    whole year is checked afterwards by the importer.
    """

    def __init__(self, path, solar_year, errata=()):
        self.path = path
        self.solar_year = solar_year
        self.errata = list(errata)
        self.applied = set()
        self.previous = {column: None for column in COLUMNS}
        self.rows = []
        self.skipped = []

    def read_page(self, page):
        previous, count = dict(self.previous), len(self.rows)
        try:
            for row in day_rows(page_words(self.path, page)):
                self.add(page, row)
        except ValueError as error:
            self.previous, self.rows[count:] = previous, []
            self.skipped.append((page, str(error)))
            return 0
        return len(self.rows) - count

    def add(self, page, row):
        resolved = {column: cell(row["cells"][column], self.previous[column], column) for column in COLUMNS}
        self.previous = dict(resolved)
        persian_month = index_of(resolved["persian_month"], PERSIAN_MONTHS, "Solar Hijri month")
        persian_day = int(resolved["persian_day"])
        corrected = self.corrected(persian_month, persian_day, resolved)
        gregorian = datetime.date(
            int(corrected["gregorian_year"]),
            index_of(corrected["gregorian_month"], GREGORIAN_MONTHS, "Gregorian month"),
            int(corrected["gregorian_day"]),
        )
        hijri_month = index_of(corrected["hijri_month"], HIJRI_MONTHS, "lunar Hijri month")
        weekday_iso = index_of(row["weekday"], WEEKDAYS, "weekday")
        if weekday_iso != gregorian.isoweekday():
            raise ValueError(f"{self.path} page {page}: {gregorian} is printed on {row['weekday']}")
        self.rows.append({
            "persian": f"{self.solar_year}-{persian_month:02d}-{persian_day:02d}",
            "persian_month": persian_month,
            "persian_day": persian_day,
            "weekday_iso": weekday_iso,
            "hijri_year": int(corrected["hijri_year"]),
            "hijri_month": hijri_month,
            "hijri_day": int(corrected["hijri_day"]),
            "gregorian": gregorian,
            "official_holiday": HOLIDAY in name_key(row["occasion"]),
            "occasion": row["occasion"],
            "footnote": row["footnote"],
            "erratum": corrected is not resolved,
            "page": page,
        })

    def corrected(self, month, day, resolved):
        """The printed cells with every erratum of this day applied; the printed cells themselves if none applies."""
        result = resolved
        for index, erratum in enumerate(self.errata):
            if erratum.applies(month, day) and int(resolved[erratum.column]) == erratum.printed(month, day):
                result = dict(result)
                result[erratum.column] = str(erratum.value(month, day))
                self.applied.add(index)
        return result


class Erratum:
    """A misprint in one column over a run of days, corrected from the document's own dates around it.

    [first] and [last] are Solar Hijri (month, day) pairs; [printed] is what the page prints on [first] (a lunar day
    then counts up by one a day) and [shift] what the correction adds. An erratum changes a cell only where the page
    prints the value stated, and the importer fails if one is never used, so a re-issued document is not corrected
    by accident; the corrected table must still pass every day-by-day check.
    """

    def __init__(self, column, first, last, printed, shift, reason):
        self.column, self.first, self.last = column, first, last
        self.printed_first, self.shift, self.reason = printed, shift, reason

    def applies(self, month, day):
        return self.first <= (month, day) <= self.last

    def printed(self, month, day):
        if self.column == "hijri_day":
            return self.printed_first + (day - self.first[1]) if month == self.first[0] else None
        return self.printed_first

    def value(self, month, day):
        return self.printed(month, day) + self.shift
