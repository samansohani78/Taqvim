#!/usr/bin/env python3
# Copyright (c) 2026 Saman Sohani. All Rights Reserved.
# Proprietary and confidential. See the LICENSE file in the repository root.
"""Generates core/astronomy's golden/odeh/odeh-2004-table6.csv (T-1301) with the observation facts of Odeh's Table VI.

Usage: tools/crescent/odeh_table6_facts.py [--pdf PATH] [--check]

M. Sh. Odeh, "New Criterion for Lunar Crescent Visibility", Experimental Astronomy 18: 39-64 (2004), Table VI
(journal pages 44-60) is printed as scanned images with no text layer. Each page image is read here: text rows are
found from ink runs, glyphs are the connected components of a row (joined when they overlap horizontally, split when
several touching digits form one component), and each glyph gets the character of its nearest prototype in
odeh_table6_glyphs.json (mean shape features of the scan's own glyph classes, labelled once by eye).

Only observation facts are written (owner decision 2026-09-15, "facts only, app computes"): record number, reference
source letter, morning or evening, local date, east longitude, latitude, elevation and the naked-eye, binocular and
telescope marks. Odeh's computed columns (best-time JD, age, lag, ARCV, DAZ, ARCL, W, V and the zone groups) are read
only to check the transcription and are discarded:
- V must follow equation 2 from the row's ARCV and W (W is printed in arc seconds);
- ARCL must equal acos(cos ARCV cos DAZ);
- the best-time JD shifted by the longitude must fall on the printed local date;
- the age must be negative exactly for morning rows;
- V must not decrease through the table, which is sorted by V.
The table prints 578 records although the paper counts 737; the 159 absent numbers are the paper's own gap. Record 737
prints 02-11-2006 while its best-time JD 2453677.533 is 2005-11-02, the date written to the golden (see its note).

The PDF is not stored in the repository: pass --pdf with a local copy, or it is downloaded from URL; its SHA-256 must
equal SHA256. Needs `pdfimages` (poppler-utils) and numpy. --check regenerates in memory and compares.
"""
import argparse
import datetime
import hashlib
import json
import math
import pathlib
import re
import statistics
import subprocess
import sys
import tempfile
import urllib.request

import numpy as np

REPO = pathlib.Path(__file__).resolve().parents[2]
URL = "https://astronomycenter.net/pdf/2006_cri.pdf"
SHA256 = "7aac71168255c9576f20613c2230ce218e268c0f089f87fe4f0b865a25f26464"
GOLDEN = "core/astronomy/src/test/resources/golden/odeh/odeh-2004-table6.csv"
PROTOTYPES = pathlib.Path(__file__).with_name("odeh_table6_glyphs.json")
FIRST_PAGE, LAST_PAGE = 6, 22
PRINTED_ROWS = 578
PAPER_RECORDS = 737
# Record number -> (local date written to the golden, reason), for printed dates that contradict the row's own JD.
KNOWN_DATE_ERRORS = {737: ("2005-11-02", "printed 02-11-2006; the row's best-time JD 2453677.533 falls on 2005-11-02")}
# Record number -> note for printed facts that are implausible but kept as printed (checked against the scan).
KNOWN_ODDITIES = {720: "elevation printed as 12700 m"}
ABOVE, BELOW = 8, 50  # Pixels kept above a row's first ink and below it.
CELL_PITCH = 34.8  # Character pitch in pixels on the full-size pages; two pages are printed smaller.
GRID_Y, GRID_X = 12, 8
V_TOLERANCE = 0.12  # ARCV to 0.1 degree and W to 1 arc second.
ARCL_TOLERANCE = 0.2
DIGITS = str.maketrans({"O": "0", "o": "0", "I": "1", "l": "1"})
FIELD_PATTERNS = {
    "src": r"[ABCDFI]", "me": r"[ME]", "date": r"\d\d-\d\d-\d{4}", "lon": r"-?\d{3}\.\d", "lat": r"-?\d\d\.\d",
    "ele": r"\d+", "n": r"[IV]?", "b": r"[IV]?", "t": r"[IV]?", "jd": r"\d{7}\.\d{3}", "age": r"-?\d+\.\d",
    "lag": r"\d+", "arcv": r"-?\d+\.\d", "daz": r"-?\d+\.\d", "arcl": r"\d+\.\d", "w": r"\d+", "v": r"-?\d+\.\d\d",
}


def pdf_bytes(path):
    if path:
        data = pathlib.Path(path).read_bytes()
    else:
        with urllib.request.urlopen(URL, timeout=120) as response:
            data = response.read()
    digest = hashlib.sha256(data).hexdigest()
    if digest != SHA256:
        raise SystemExit(f"PDF sha256 {digest} is not the reviewed {SHA256}")
    return data


def page_bitmaps(data, tmp):
    pdf = pathlib.Path(tmp) / "odeh.pdf"
    pdf.write_bytes(data)
    prefix = pathlib.Path(tmp) / "page"
    subprocess.run(["pdfimages", "-f", str(FIRST_PAGE), "-l", str(LAST_PAGE), str(pdf), str(prefix)], check=True)
    files = sorted(pathlib.Path(tmp).glob("page-*.pbm"))
    if len(files) != LAST_PAGE - FIRST_PAGE + 1:
        raise SystemExit(f"expected one table image per page, found {len(files)}")
    return [read_pbm(file.read_bytes()) for file in files]


def read_pbm(data):
    magic, width, height, raw = data.split(maxsplit=3)
    if magic != b"P4":
        raise SystemExit("table image is not a bilevel PBM")
    width, height = int(width), int(height)
    stride = (width + 7) // 8
    pad = stride * 8 - width
    return width, [int.from_bytes(raw[i * stride:(i + 1) * stride], "big") >> pad for i in range(height)]


def row_tops(width, rows):
    """First ink line of every text row, found in the record-number column; full-width rules are skipped."""
    rules = {y for y, row in enumerate(rows) if row.bit_count() > width * 0.8}
    shift = width - 140
    tops, inside = [], False
    for y, row in enumerate(rows):
        ink = (row >> shift).bit_count() > 0
        if ink and not inside and not any(r - 3 <= y <= r + 5 for r in rules):
            tops.append(y)
        inside = ink
    return tops


def components(width, rows, y0, y1):
    black = set()
    for y in range(y0, y1):
        row = rows[y]
        while row:
            bit = row.bit_length() - 1
            black.add((y, width - 1 - bit))
            row &= ~(1 << bit)
    seen, found = set(), []
    for start in black:
        if start in seen:
            continue
        stack, pixels = [start], []
        seen.add(start)
        while stack:
            y, x = stack.pop()
            pixels.append((y, x))
            for q in ((y + dy, x + dx) for dy in (-1, 0, 1) for dx in (-1, 0, 1)):
                if q in black and q not in seen:
                    seen.add(q)
                    stack.append(q)
        found.append(pixels)
    return found


def glyphs_of_row(width, rows, top, bottom, pitch):
    spans = sorted(
        ((min(x for _, x in c), max(x for _, x in c), c) for c in components(width, rows, top - ABOVE, bottom)),
        key=lambda s: s[0],
    )
    groups = []
    for x0, x1, pixels in spans:
        if x1 - x0 >= 100:
            continue  # rule fragments
        if groups and x0 <= groups[-1][1] + 1:
            g0, g1, gp = groups[-1]
            groups[-1] = (g0, max(g1, x1), gp + pixels)
        else:
            groups.append((x0, x1, pixels))
    glyphs = []
    for x0, x1, pixels in groups:
        height = max(y for y, _ in pixels) - min(y for y, _ in pixels)
        if len(pixels) < 40 and max(y for y, _ in pixels) - top < 14:
            continue  # descender tail of the row above
        if height <= 8 and x1 - x0 > pitch * 1.3:
            continue  # piece of a rule under the column headings
        parts = max(1, round((x1 - x0 + 1) / (pitch * 0.72))) if x1 - x0 > pitch * 1.3 else 1
        step = (x1 - x0 + 1) / parts
        for k in range(parts):
            lo, hi = x0 + k * step, x0 + (k + 1) * step
            part = [(y - top, x) for y, x in pixels if lo <= x < hi]
            if part:
                glyphs.append((min(x for _, x in part), max(x for _, x in part), part))
    return glyphs


def features(pixels, scale):
    ys = np.array([p[0] for p in pixels])
    xs = np.array([p[1] for p in pixels])
    y0, y1, x0, x1 = ys.min(), ys.max(), xs.min(), xs.max()
    crop = np.zeros((y1 - y0 + 1, x1 - x0 + 1))
    crop[ys - y0, xs - x0] = 1
    h, w = crop.shape
    gy = np.minimum((np.arange(h) * GRID_Y) // h, GRID_Y - 1)
    gx = np.minimum((np.arange(w) * GRID_X) // w, GRID_X - 1)
    ink, cells = np.zeros((GRID_Y, GRID_X)), np.zeros((GRID_Y, GRID_X))
    index = (gy[:, None].repeat(w, 1), gx[None, :].repeat(h, 0))
    np.add.at(ink, index, crop)
    np.add.at(cells, index, 1)
    return (ink / np.maximum(cells, 1)).ravel(), np.array([h, w, y0, y1]) / scale


def classify(glyphs, scale, prototypes, labels):
    shapes, geometry = zip(*(features(pixels, scale) for _, _, pixels in glyphs))
    geometry = np.stack(geometry)
    tall = geometry[geometry[:, 0] > 30, 3]
    baseline = float(np.median(tall)) if len(tall) else 40.0
    geometry[:, 2:] -= baseline
    vectors = np.concatenate([np.stack(shapes), geometry / 12.0], axis=1)
    nearest = np.abs(vectors[:, None, :] - prototypes[None, :, :]).sum(2).argmin(1)
    return [(x0, x1, labels[k]) for (x0, x1, _), k in zip(glyphs, nearest)]


def tokens(chars):
    centers = [((x0 + x1) / 2, c) for x0, x1, c in chars]
    gaps = sorted(b[0] - a[0] for a, b in zip(centers, centers[1:]))
    pitch = statistics.median(gaps[: max(1, len(gaps) * 2 // 3)])
    words = [[centers[0]]]
    for a, b in zip(centers, centers[1:]):
        (words.append([b]) if b[0] - a[0] > 1.55 * pitch else words[-1].append(b))
    return [("".join(c for _, c in word), (word[0][0] + word[-1][0]) / 2) for word in words]


def text_rows(bitmaps):
    table = json.loads(PROTOTYPES.read_text())
    prototypes = np.array([p["features"] for p in table["prototypes"]])
    labels = [p["char"] for p in table["prototypes"]]
    for page, (width, rows) in enumerate(bitmaps, start=FIRST_PAGE):
        scale = 32.78 / CELL_PITCH if width < 4300 else 1.0
        tops = row_tops(width, rows)
        for n, top in enumerate(tops):
            bottom = min(len(rows), top + BELOW, tops[n + 1] - 4 if n + 1 < len(tops) else len(rows))
            glyphs = glyphs_of_row(width, rows, top, bottom, CELL_PITCH * scale)
            if glyphs:
                yield page, tokens(classify(glyphs, scale, prototypes, labels))


def record(words, marks):
    texts = [w for w, _ in words]
    lon_at = next((k for k in range(4, len(texts)) if re.fullmatch(r"-?\d{3}\.\d", texts[k].translate(DIGITS))), None)
    if lon_at is None:
        raise SystemExit(f"no longitude in row {' | '.join(texts)}")
    fields = dict(zip(("no", "src", "me", "date"), texts[:4]))
    fields.update(zip(("lon", "lat", "ele"), texts[lon_at:lon_at + 3]))
    fields.update(zip(("jd", "age", "lag", "arcv", "daz", "arcl", "w", "v"), texts[-8:]))
    fields = {k: (v if k in ("src", "me") else v.translate(DIGITS)) for k, v in fields.items()}
    fields.update(n="", b="", t="")
    for word, x in words[lon_at + 3:len(words) - 8]:
        fields[min(marks, key=lambda k: abs(marks[k] - x))] = word.upper()
    bad = [k for k, pattern in FIELD_PATTERNS.items() if not re.fullmatch(pattern, fields[k])]
    if bad or not fields["no"].isdigit():
        raise SystemExit(f"unreadable {bad} in row {' | '.join(texts)}")
    return fields


def parsed_records(bitmaps):
    """Data rows; the mark columns are located from the column-number line (9, 10, 11) under each heading."""
    records, marks = [], {}
    for page, words in text_rows(bitmaps):
        texts = [w for w, _ in words]
        if len(texts) == 19 and sum(re.sub(r"\D", "", t) == str(n) for n, t in enumerate(texts, start=1)) >= 15:
            marks = {"n": words[8][1], "b": words[9][1], "t": words[10][1]}
        elif texts[0].translate(DIGITS).isdigit() and marks:
            records.append(dict(record(words, marks), page=page))
    return records


def local_date(jd, longitude):
    return (datetime.datetime(2000, 1, 1, 12) + datetime.timedelta(days=jd - 2451545.0 + longitude / 360.0)).date()


def check(records):
    """Fails unless every row's computed columns agree with each other; see the module documentation."""
    problems = []
    for r in records:
        no, arcv, w, v = int(r["no"]), float(r["arcv"]), float(r["w"]) / 60, float(r["v"])
        if abs(v - (arcv - (-0.1018 * w**3 + 0.7319 * w**2 - 6.3226 * w + 7.1651))) > V_TOLERANCE:
            problems.append(f"{no}: V {v} does not follow ARCV {arcv} and W {r['w']}")
        arcl = math.degrees(math.acos(math.cos(math.radians(arcv)) * math.cos(math.radians(float(r["daz"])))))
        if abs(arcl - float(r["arcl"])) > ARCL_TOLERANCE:
            problems.append(f"{no}: ARCL {r['arcl']} does not follow ARCV and DAZ")
        on_jd = local_date(float(r["jd"]), float(r["lon"])).isoformat()
        printed = "-".join(reversed(r["date"].split("-")))
        if on_jd != KNOWN_DATE_ERRORS.get(no, (printed,))[0]:
            problems.append(f"{no}: printed date {r['date']} but JD {r['jd']} falls on {on_jd}")
        if (float(r["age"]) < 0) != (r["me"] == "M"):
            problems.append(f"{no}: {r['me']} row with age {r['age']}")
    values = [float(r["v"]) for r in records]
    problems += [f"V decreases after row {i}" for i, (a, b) in enumerate(zip(values, values[1:])) if b < a]
    numbers = [int(r["no"]) for r in records]
    if len(records) != PRINTED_ROWS or len(set(numbers)) != PRINTED_ROWS or not set(numbers) <= set(range(1, 738)):
        problems.append(f"expected {PRINTED_ROWS} distinct record numbers in 1-{PAPER_RECORDS}, read {len(records)}")
    if problems:
        raise SystemExit("\n".join(problems))


def golden_text(records):
    numbers = sorted(int(r["no"]) for r in records)
    absent = sorted(set(range(1, PAPER_RECORDS + 1)) - set(numbers))
    header = [
        '# source: M. Sh. Odeh, "New Criterion for Lunar Crescent Visibility", Experimental Astronomy 18: 39-64 (2004), '
        "Table VI: the observation facts of its 578 printed records",
        f"# url: {URL}",
        "# retrieved: 2026-09-15",
        "# page: 44-60",
        "# reviewer: pending",
        f"# notes: PDF sha256={SHA256} (not stored); generated by tools/crescent/odeh_table6_facts.py from the scanned "
        "table images; facts only (owner decision 2026-09-15): record number, reference source (A Schaefer, B Stamm, "
        "C SAAO, D Mirsaeed, F Mehrani, I ICOP), M(orning)/E(vening), local date, latitude and east longitude in "
        "degrees, elevation in metres, and visibility with the naked eye, binoculars and a telescope (V visible, "
        "I invisible, empty not tried); Odeh's computed columns are checked and not stored; the paper counts 737 "
        f"records but prints 578, absent numbers: {' '.join(map(str, absent))}",
        "no,source,m_or_e,local_date,latitude,longitude,elevation_m,naked_eye,binoculars,telescope,note",
    ]
    lines = []
    for r in sorted(records, key=lambda r: int(r["no"])):
        no = int(r["no"])
        date, note = KNOWN_DATE_ERRORS.get(no, ("-".join(reversed(r["date"].split("-"))), KNOWN_ODDITIES.get(no, "")))
        values = [no, r["src"], r["me"], date, f"{float(r['lat']):.1f}", f"{float(r['lon']):.1f}", int(r["ele"])]
        lines.append(",".join(map(str, values + [r["n"], r["b"], r["t"], note])))
    return "\n".join(header + lines) + "\n"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--pdf")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    with tempfile.TemporaryDirectory() as tmp:
        records = parsed_records(page_bitmaps(pdf_bytes(args.pdf), tmp))
    check(records)
    text = golden_text(records)
    target = REPO / GOLDEN
    if args.check:
        if target.read_text() != text:
            sys.exit(f"{GOLDEN} is out of date; rerun tools/crescent/odeh_table6_facts.py")
        print(f"{GOLDEN} is up to date")
        return
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(text)
    print(f"{len(records)} rows written to {GOLDEN}")


if __name__ == "__main__":
    main()
