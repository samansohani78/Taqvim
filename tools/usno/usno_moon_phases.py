#!/usr/bin/env python3
"""Generate the T-403 USNO Moon phases golden (1700-2100) from the archived raw API responses."""
import datetime, hashlib, json, pathlib

repo = pathlib.Path(__file__).resolve().parents[2]
archive = repo / "docs/sources/usno/moon-phases-raw.json"
sums = repo / "docs/sources/usno/SHA256SUMS"
golden = repo / "core/astronomy/src/test/resources/golden/usno/moon-phases-1700-2100.csv"
source_file = "docs/sources/usno/moon-phases-raw.json"
quarters = {"New Moon": "new_moon", "First Quarter": "first_quarter", "Full Moon": "full_moon",
            "Last Quarter": "third_quarter"}
order = list(quarters.values())

data = archive.read_bytes()
digest = hashlib.sha256(data).hexdigest()
expected = {line.split()[1]: line.split()[0] for line in sums.read_text().splitlines() if line.strip()}
assert expected["moon-phases-raw.json"] == digest, "archive checksum mismatch"

responses = json.loads(data)
assert [r["year"] for r in responses] == list(range(1700, 2101)), "years must be 1700..2100 in order"
rows, previous = [], None
for response in responses:
    year = response["year"]
    assert response["apiversion"] == "4.0.1", year
    assert response["numphases"] == len(response["phasedata"]), year
    for record in response["phasedata"]:
        assert record["year"] == year, (year, record)
        hh, mm = (int(part) for part in record["time"].split(":"))
        instant = datetime.datetime(year, record["month"], record["day"], hh, mm, tzinfo=datetime.timezone.utc)
        quarter = quarters[record["phase"]]
        if previous is not None:
            gap = (instant - previous[0]).total_seconds() / 86400
            assert 5.5 < gap < 9.0, (previous, record, gap)
            assert order.index(quarter) == (order.index(previous[1]) + 1) % 4, (previous, record)
        previous = (instant, quarter)
        rows.append(f"{year},{quarter},{instant.strftime('%Y-%m-%dT%H:%M:00Z')},{source_file}")

header = [
    "# source: U.S. Naval Observatory, Astronomical Applications Department — API v4.0.1, Phases of the Moon "
    "(/api/moon/phases/year?year=YYYY), years 1700-2100",
    "# url: https://aa.usno.navy.mil/data/api.html",
    "# retrieved: 2026-09-15",
    "# reviewer: pending",
    "# notes: US government work (public domain, 17 U.S.C. 105); owner-retrieved JSON responses archived "
    f"byte-for-byte in {source_file} (sha256={digest}, listed in docs/sources/usno/SHA256SUMS); times are UT "
    "(tz 0) rounded to the minute; USNO's Last Quarter is third_quarter; generated from the archive, not typed",
    "gregorian_year,quarter,instant_ut,source_file",
]
golden.parent.mkdir(parents=True, exist_ok=True)
golden.write_text("\n".join(header + rows) + "\n")
print(len(rows), "rows;", golden)
