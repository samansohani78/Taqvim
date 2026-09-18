#!/usr/bin/env python3
# Copyright (c) 2026 Saman Sohani. All Rights Reserved.
# Proprietary and confidential. See the LICENSE file in the repository root.
"""Reads the digits of a calendar whose text layer names them wrongly, from the shapes the page draws.

The 1395, 1396, 1401 and 1402 editions embed their fonts with a ToUnicode map that gives several digit glyphs one
character: in 1401, for example, the glyphs of 7, 8, 9 and 0 all extract as "1", and the others are permuted (the
glyph of 1 extracts as "0", that of 2 as "6"). The glyphs themselves are drawn correctly, and each digit is a word of
its own with a box from `pdftotext -bbox`. So:

1. Every page is rendered with `pdftoppm` at [DPI]; each digit word's box is cut out and split into its connected
   ink components, one per digit (a word whose component count differs from its character count is not read).
2. The digits whose value the document fixes are labelled: on every month page of the daily table, row n is Solar
   Hijri day n, and its Gregorian day and year follow from the first day of the year, which the caller takes from
   the neighbouring official calendars and the official leap-year table — never from the app's own calendar. A page
   counts only if its row count is the month's length and every printed weekday matches that date.
3. A glyph is read as the labelled glyph nearest to it among those extracted as the same character (L1 distance on
   a [GRID] x [GRID] image of its ink plus its height and width). It is accepted only if the nearest one is within
   [SAME_GLYPH] and every glyph of another digit is at least [OTHER_GLYPH] away; otherwise the digit is written as
   [UNREADABLE], so a table cell holding it fails to parse and the page is dropped, never guessed.
4. The gate: before any digit of a year is used, each month page's labelled digits are read with templates from the
   other month pages only. A single miss, a year with fewer than twelve month pages, or a labelled glyph that does
   not clear the margins rejects the year with the measured accuracy.

In these editions the same glyph renders to the same pixels wherever the table uses it (the nearest distance is 0
for all but three lunar days of 1402, at 0.03, against at least 44 to any other digit), so the margins are wide.
Occasion text and titles use other fonts; their digits are read only where they clear the same margins.

Needs numpy and Pillow (BSD-3-Clause, MIT-CMU; tools only, never part of the app) besides poppler-utils.

Used by tools/sources/iran/official_calendar_import.py; not a command of its own.
"""

import datetime
import subprocess
import tempfile

try:
    import numpy
    from PIL import Image
except ImportError:  # the import itself still works; reading glyphs needs numpy and Pillow
    numpy = Image = None

from official_calendar_pdf import (
    CELL,
    COLUMNS,
    FOOTNOTE,
    PERSIAN_MONTHS,
    REPO,
    WEEKDAYS,
    bands_of,
    groups_of,
    index_of,
    page_count,
    page_words,
    weekday_cells,
)

DPI = 300
SCALE = DPI / 72
MARGIN = 3
INK = 128
GRID = 16
SAME_GLYPH = 1.0
OTHER_GLYPH = 20.0
UNREADABLE = "؟"
MONTH_PAGES = 12
KNOWN_COLUMNS = ("persian_day", "gregorian_day", "gregorian_year")
AVAILABLE = numpy is not None


class RejectedYear(Exception):
    """A year whose digits cannot be read reliably; the message states the measured accuracy."""


def render(path, page):
    """The page as a boolean ink mask at [DPI]."""
    with tempfile.TemporaryDirectory() as directory:
        subprocess.run(["pdftoppm", "-r", str(DPI), "-gray", "-f", str(page), "-l", str(page), "-singlefile",
                        "-png", str(REPO / path), f"{directory}/page"], check=True)
        with Image.open(f"{directory}/page.png") as image:
            return numpy.asarray(image.convert("L")) < INK


def components(ink):
    """Connected ink components (8-neighbourhood) of a small crop: (label image, [x0, x1, y0, y1, label])."""
    height, width = ink.shape
    labels = numpy.zeros((height, width), dtype=numpy.int32)
    found = []
    for y, x in zip(*numpy.nonzero(ink)):
        if labels[y, x]:
            continue
        label = len(found) + 1
        labels[y, x] = label
        stack, xs, ys = [(y, x)], [], []
        while stack:
            cy, cx = stack.pop()
            xs.append(cx)
            ys.append(cy)
            for ny in range(max(cy - 1, 0), min(cy + 2, height)):
                for nx in range(max(cx - 1, 0), min(cx + 2, width)):
                    if ink[ny, nx] and not labels[ny, nx]:
                        labels[ny, nx] = label
                        stack.append((ny, nx))
        found.append([min(xs), max(xs) + 1, min(ys), max(ys) + 1, label])
    return labels, found


def glyph_features(image, word):
    """One feature vector per digit of [word], left to right, or None if its ink does not split into as many
    components as the word has characters. Components touching the crop's edge (table rules) are ignored, and
    components overlapping horizontally by more than half the narrower one are one glyph."""
    crop = image[int(word["y"] * SCALE) - MARGIN:int(word["y1"] * SCALE) + 1 + MARGIN,
                 int(word["x0"] * SCALE) - MARGIN:int(word["x1"] * SCALE) + 1 + MARGIN]
    labels, found = components(crop)
    height, width = crop.shape
    inner = sorted((part for part in found if part[0] > 0 and part[2] > 0 and part[1] < width and part[3] < height),
                   key=lambda part: part[0])
    glyphs = []
    for x0, x1, y0, y1, label in inner:
        if glyphs and min(x1, glyphs[-1][1]) - max(x0, glyphs[-1][0]) > 0.5 * min(x1 - x0, glyphs[-1][1] - glyphs[-1][0]):
            last = glyphs[-1]
            glyphs[-1] = [min(x0, last[0]), max(x1, last[1]), min(y0, last[2]), max(y1, last[3]), last[4] + [label]]
        else:
            glyphs.append([x0, x1, y0, y1, [label]])
    if len(glyphs) != len(word["text"]):
        return None
    return [feature(labels, glyph, height) for glyph in glyphs]


def feature(labels, glyph, height):
    x0, x1, y0, y1, members = glyph
    mask = numpy.isin(labels[y0:y1, x0:x1], members).astype(numpy.uint8) * 255
    shape = numpy.asarray(Image.fromarray(mask).resize((GRID, GRID), Image.BILINEAR), dtype=float) / 255
    return numpy.concatenate([shape.ravel(), [2 * (y1 - y0) / height, 2 * (x1 - x0) / height]])


def row_tokens(inside):
    """The first len(COLUMNS) cells of a day row as (text, digit words or None), like [table_cells] but keeping the
    words of every number, so a cell's digits can be matched to their glyphs."""
    tokens = []
    for group in groups_of(inside):
        text, digits = "", []
        for word in group["words"] + [None]:
            if word is not None and word["text"].isdigit():
                digits.insert(0, word)
                continue
            if digits or word is None:
                tokens += [(token, None) for token in CELL.findall(text.replace(FOOTNOTE, " ")) if token.strip()]
                text = ""
            if digits:
                tokens.append(("".join(digit["text"] for digit in digits), digits))
                digits = []
            if word is not None:
                text += word["text"]
        if len(tokens) >= len(COLUMNS):
            break
    return tokens[:len(COLUMNS)]


def month_page(words, first_day, month_lengths):
    """The labelled digit words of a month page — [(word, true digits)] — or None if the page is not one."""
    cells = weekday_cells(words)
    rows = []
    for (top, bottom), weekday in zip(bands_of(cells), cells):
        tokens = row_tokens([word for word in words if top <= word["y"] < bottom and word["x0"] < weekday["x0"]])
        if len(tokens) != len(COLUMNS):
            return None
        rows.append((weekday["text"], dict(zip(COLUMNS, tokens))))
    try:
        month = index_of(rows[0][1]["persian_month"][0], PERSIAN_MONTHS, "Solar Hijri month") if rows else None
    except ValueError:
        return None
    if month is None or len(rows) != month_lengths[month - 1]:
        return None
    start = first_day + datetime.timedelta(days=sum(month_lengths[:month - 1]))
    labelled = []
    for index, (weekday, cells_of_row) in enumerate(rows):
        date = start + datetime.timedelta(days=index)
        if index_of(weekday, WEEKDAYS, "weekday") != date.isoweekday():
            return None
        truth = {"persian_day": str(index + 1), "gregorian_day": str(date.day), "gregorian_year": str(date.year)}
        for column in KNOWN_COLUMNS:
            text, digit_words = cells_of_row[column]
            if digit_words is None:
                continue
            if len(text) != len(truth[column]):
                return None
            offset = 0
            for word in digit_words:
                labelled.append((word, truth[column][offset:offset + len(word["text"])]))
                offset += len(word["text"])
    return labelled


class Templates:
    """Labelled glyphs: (extracted character, true digit, features, page)."""

    def __init__(self, samples):
        self.samples = samples

    def read(self, character, features, excluded_page=None):
        """The digit the glyph shows, or None unless it clears [SAME_GLYPH] and [OTHER_GLYPH]."""
        nearest = {}
        for sample_character, digit, sample, page in self.samples:
            if sample_character == character and page != excluded_page:
                distance = float(numpy.abs(sample - features).sum())
                nearest[digit] = min(nearest.get(digit, distance), distance)
        ranked = sorted(nearest.items(), key=lambda item: (item[1], item[0]))
        if not ranked or ranked[0][1] > SAME_GLYPH or (len(ranked) > 1 and ranked[1][1] < OTHER_GLYPH):
            return None
        return ranked[0][0]


class DigitReader:
    """Reads one calendar's pages with every digit word replaced by the digits its glyphs show (see the module)."""

    def __init__(self, path, first_day, month_lengths):
        if not AVAILABLE:
            raise SystemExit(f"{path}: reading digits from glyphs needs numpy and Pillow (pip install numpy pillow)")
        self.path = path
        self.images, self.words, samples = {}, {}, []
        for page in range(1, page_count(path) + 1):
            self.images[page], self.words[page] = render(path, page), page_words(path, page)
            for word, digits in month_page(self.words[page], first_day, month_lengths) or []:
                features = glyph_features(self.images[page], word)
                if features is None:
                    raise RejectedYear(f"{path} page {page}: a labelled number does not split into its digits")
                samples += [(character, digit, glyph, page)
                            for character, digit, glyph in zip(word["text"], digits, features)]
        self.templates = Templates(samples)
        self.month_pages = sorted({sample[3] for sample in samples})
        self.validated = self.validate()

    def validate(self):
        """Leave-one-page-out reading of every labelled digit; the year is rejected unless all are read right."""
        right = sum(1 for character, digit, glyph, page in self.templates.samples
                    if self.templates.read(character, glyph, excluded_page=page) == digit)
        total = len(self.templates.samples)
        if len(self.month_pages) < MONTH_PAGES or right != total:
            raise RejectedYear(f"{self.path}: {len(self.month_pages)} month pages, {right} of {total} labelled "
                               f"digits read right with the other pages' glyphs ({100 * right / max(total, 1):.2f} %)")
        return right

    def page_words(self, _path, page):
        """[page_words] of the page with every digit word's text read from its glyphs."""
        return [dict(word, text=self.read_word(page, word)) if word["text"].isdigit() else word
                for word in self.words[page]]

    def read_word(self, page, word):
        features = glyph_features(self.images[page], word)
        if features is None:
            return UNREADABLE * len(word["text"])
        digits = [self.templates.read(character, glyph) for character, glyph in zip(word["text"], features)]
        return "".join(digit or UNREADABLE for digit in digits)
