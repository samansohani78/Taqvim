#!/usr/bin/env python3
"""Generate the A-15 tithi golden from the Rashtriya Panchang (DT-014).

The Rashtriya Panchang is the panchang the Government of India publishes — India Meteorological Department,
Positional Astronomy Centre — "to promote panchang calculations on modern scientific principles", from the same
Indian Astronomical Ephemeris the department computes. Its tithi entries are *ending moments* in Indian Standard
Time, and the Panchang states they are geocentric and so "require no correction for other places", which is what
makes them comparable with `Tithi.changes`, whose boundaries are geocentric too.

The Panchang is a priced publication (Rs. 225) of the Government of India, so its PDF is cited by URL and SHA-256
rather than archived here (docs/sources/MANIFEST.md); only the factual instants are committed. Point --pdf at a
copy of RP_1945SE.pdf to regenerate, or run --check to verify the committed golden parses and is self-consistent.

Hours past 24 in the Panchang mean the following civil day: "h. 25-59" is 01:59 the next morning.
"""

import argparse
import datetime
import hashlib
import pathlib
import re
import subprocess

repo = pathlib.Path(__file__).resolve().parents[3]
golden = repo / "core/astronomy/src/test/resources/golden/panchang/tithi-1945se.csv"

PDF_SHA256 = "9ba9dd3970a45787edafd619787b91d83557fa4b7c9344738316e8b8d73757c4"
ZIP_SHA256 = "38c50c8d11071369e895ec21e09120be49ab576f6a40334965da7eb22b7b8522"
SOURCE_URL = (
    "https://web.archive.org/web/20230130074258/http://packolkata.gov.in/download/EnglishRP2324.zip"
)
IST_OFFSET = datetime.timedelta(hours=5, minutes=30)

# The Panchang's tithi names, in order within a paksha. Spellings are the Panchang's own, including "Chaturti".
TITHI_NAMES = [
    "Pratipad",
    "Dvitiya",
    "Tritiya",
    "Chaturti",
    "Panchami",
    "Shashthi",
    "Saptami",
    "Ashtami",
    "Navami",
    "Dasami",
    "Ekadasi",
    "Dvadasi",
    "Trayodasi",
    "Chaturdasi",
]
DAY_BLOCK = re.compile(r"(?=Julian Day:\s*\d{7}\.5)")
JULIAN_DAY = re.compile(r"Julian Day:\s*(\d{7})\.5")
TITHI_SEGMENT = re.compile(r"Tithi:\s*(.*?)(?=Nakshatra:)", re.S)
# A paksha marker applies to the endings that follow it: a line can read "(… Sukla) Purnima h. 7-06 then
# (Krishna) Pratipad h. 27-19" when the full moon falls that day, so the two endings sit in different pakshas.
TOKEN = re.compile(r"\((?:[^)]*\s)?(?P<paksha>Sukla|Krishna)\)|(?P<name>[A-Za-z]+)\s*h\.\s*(?P<h>\d{1,2})-(?P<m>\d{2})")


def tithi_number(name: str, paksha: str) -> int | None:
    """1‥30 for a tithi named in [paksha], or None when the name is not one the Panchang uses."""
    if name in ("Purnima", "Amavasya"):
        return 15 if name == "Purnima" else 30
    if name not in TITHI_NAMES:
        return None
    within = TITHI_NAMES.index(name) + 1
    return within if paksha == "Sukla" else within + 15


def civil_date(julian_day: int) -> datetime.date:
    """The civil date of a Julian day number whose .5 falls at midnight IST, as the Panchang prints it."""
    # JD 2460025.5 is 00:00 UT on 22 March 2023, and the Panchang prints that day's JD as the .5 value, so the
    # civil day is the one that starts at it: JD 2440587.5 is 1970-01-01.
    return datetime.date(1970, 1, 1) + datetime.timedelta(days=julian_day - 2440587)


def rows_from(text: str) -> list[str]:
    """One CSV row per printed tithi ending moment, in date order."""
    out: list[str] = []
    for block in DAY_BLOCK.split(text)[1:]:
        day = JULIAN_DAY.search(block)
        segment = TITHI_SEGMENT.search(block)
        if not day or not segment:
            continue
        flat = " ".join(segment.group(1).split())
        date = civil_date(int(day.group(1)))
        paksha = None
        for token in TOKEN.finditer(flat):
            if token.group("paksha"):
                paksha = token.group("paksha")
                continue
            # "ahoratra": a tithi covering the whole day prints no ending moment, so nothing is emitted for it.
            if paksha is None:
                continue
            name, hour, minute = token.group("name"), token.group("h"), token.group("m")
            number = tithi_number(name, paksha)
            if number is None:
                continue
            end = datetime.datetime.combine(date, datetime.time()) + datetime.timedelta(
                hours=int(hour), minutes=int(minute)
            )
            utc = end - IST_OFFSET
            out.append(
                ",".join(
                    [
                        date.isoformat(),
                        paksha,
                        str(number),
                        name,
                        end.strftime("%Y-%m-%dT%H:%M"),
                        utc.strftime("%Y-%m-%dT%H:%MZ"),
                    ]
                )
            )
    return out


def header(count: int) -> list[str]:
    return [
        "# source: Rashtriya Panchang (English), Saka Era 1945 (2023-2024 A.D.), Positional Astronomy Centre,"
        " India Meteorological Department, Ministry of Earth Sciences — tithi ending moments, geocentric, in IST",
        f"# url: {SOURCE_URL}",
        "# retrieved: 2026-09-25",
        "# reviewer: pending",
        f"# notes: {count} ending moments. A priced Government of India publication (Rs. 225), so it is cited rather"
        f" than archived and only these factual instants are kept; sha256 EnglishRP2324.zip {ZIP_SHA256},"
        f" RP_1945SE.pdf {PDF_SHA256}. Hours past 24 in the Panchang mean the next civil day.",
        "date_ist,paksha,tithi,name,ends_ist,ends_utc",
    ]


def generate(pdf: pathlib.Path) -> list[str]:
    data = pdf.read_bytes()
    digest = hashlib.sha256(data).hexdigest()
    assert digest == PDF_SHA256, f"unexpected PDF: {digest}"
    text = subprocess.run(
        ["pdftotext", "-layout", str(pdf), "-"], capture_output=True, text=True, check=True
    ).stdout
    return rows_from(text)


def committed() -> list[str]:
    return [line for line in golden.read_text().splitlines() if line and not line.startswith("#")][1:]


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--pdf", type=pathlib.Path, help="RP_1945SE.pdf to regenerate from")
    parser.add_argument("--check", action="store_true", help="verify the committed golden only")
    args = parser.parse_args()

    if args.check or not args.pdf:
        rows = committed()
        seen = set()
        for row in rows:
            date, paksha, number, name, ends, ends_utc = row.split(",")
            assert paksha in ("Sukla", "Krishna"), row
            assert 1 <= int(number) <= 30, row
            assert tithi_number(name, paksha) == int(number), row
            key = (date, number)
            assert key not in seen, f"duplicate {key}"
            seen.add(key)
            local = datetime.datetime.fromisoformat(ends)
            utc = datetime.datetime.fromisoformat(ends_utc.rstrip("Z"))
            assert local - utc == IST_OFFSET, row
        print(f"{len(rows)} tithi ending moments, all self-consistent")
        return 0

    rows = generate(args.pdf)
    golden.parent.mkdir(parents=True, exist_ok=True)
    golden.write_text("\n".join(header(len(rows)) + rows) + "\n")
    print(f"wrote {len(rows)} rows to {golden}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
