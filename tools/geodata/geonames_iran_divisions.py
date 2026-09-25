#!/usr/bin/env python3
# Copyright (c) 2026 Saman Sohani. All Rights Reserved.
# Proprietary and confidential. See the LICENSE file in the repository root.
"""Builds data/location's iran-divisions.tsv (DT-021) from the GeoNames gazetteer (CC BY 4.0).

Usage: geonames_iran_divisions.py <IR.zip> <geoname-url> <alternatenames-IR.zip> <altnames-url> <retrieved-date>
    <output.tsv>

Reads the two per-country GeoNames extracts for Iran (each a zip holding one 'IR.txt'):
  - the 'geoname' table dump (https://download.geonames.org/export/dump/IR.zip), from which the ADM1 (province) and
    ADM2 (county / shahrestan) rows are kept, with their own name, coordinates and admin1 code;
  - the alternate-names dump (https://download.geonames.org/export/dump/alternatenames/IR.zip), from which the
    Persian ('fa') name is kept for each division's geonameid (the alternate name flagged isPreferredName, or the
    first one when none is flagged).

GeoNames' admin3 (district / bakhsh) coverage for Iran is 7 records out of an estimated 1000+ districts (checked
2026-09-25) — far too sparse to publish, so no district level is written; see docs/DATA_TODO.md DT-021.

Nothing is translated, corrected or added: a county with no published Persian alternate name (3 of 435, checked
2026-09-25) gets an empty 'fa' field, the same convention cities.tsv uses for a name equal to the English one.
"""
import hashlib
import io
import sys
import zipfile

LEVELS = {"ADM1": "PROVINCE", "ADM2": "COUNTY"}
COLUMNS = ["code", "level", "parentCode", "en", "fa", "latitude", "longitude"]


def read_zip_member(path, member="IR.txt"):
    data = open(path, "rb").read()
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        text = archive.read(member).decode("utf-8")
    return data, text


def clean(value):
    text = value.strip()
    if any(c in text for c in "\t\r\n"):
        raise ValueError(f"control character in {text!r}")
    return text


def read_geoname_rows(text):
    """ADM1 and ADM2 rows of the geoname dump, keyed by geonameid."""
    rows = {}
    for line in text.splitlines():
        fields = line.split("\t")
        if len(fields) < 11:
            continue
        feature_code = fields[7]
        if feature_code not in LEVELS:
            continue
        rows[fields[0]] = {
            "geonameId": fields[0],
            "name": clean(fields[1]),
            "latitude": float(fields[4]),
            "longitude": float(fields[5]),
            "level": LEVELS[feature_code],
            "admin1Code": clean(fields[10]),
        }
    return rows


def read_persian_names(text, geoname_ids):
    """The best Persian ('fa') alternate name for each wanted geonameid: the preferred one, else the first."""
    names = {}
    for line in text.splitlines():
        fields = line.split("\t")
        if len(fields) < 4:
            continue
        geoname_id, language, name = fields[1], fields[2], fields[3]
        if language != "fa" or geoname_id not in geoname_ids:
            continue
        is_preferred = len(fields) > 4 and fields[4] == "1"
        if is_preferred or geoname_id not in names:
            names[geoname_id] = clean(name)
    return names


def rows(geoname_text, altnames_text):
    geoname_rows = read_geoname_rows(geoname_text)
    fa_names = read_persian_names(altnames_text, set(geoname_rows))
    provinces = {r["admin1Code"]: r for r in geoname_rows.values() if r["level"] == "PROVINCE"}
    missing_parent = [r for r in geoname_rows.values() if r["level"] == "COUNTY" and r["admin1Code"] not in provinces]
    if missing_parent:
        ids = ", ".join(r["geonameId"] for r in missing_parent)
        raise ValueError(f"county row(s) with no matching province admin1 code: {ids}")
    result = []
    for r in geoname_rows.values():
        code = r["admin1Code"] if r["level"] == "PROVINCE" else r["geonameId"]
        parent_code = "" if r["level"] == "PROVINCE" else r["admin1Code"]
        result.append(
            [
                code,
                r["level"],
                parent_code,
                r["name"],
                fa_names.get(r["geonameId"], ""),
                f"{r['latitude']:.5f}",
                f"{r['longitude']:.5f}",
            ]
        )
    return result


def sort_key(fields):
    index = {name: position for position, name in enumerate(COLUMNS)}
    level_rank = 0 if fields[index["level"]] == "PROVINCE" else 1
    return (level_rank, fields[index["parentCode"]], fields[index["en"]], fields[index["code"]])


def main(geoname_zip, geoname_url, altnames_zip, altnames_url, retrieved, output):
    geoname_bytes, geoname_text = read_zip_member(geoname_zip)
    altnames_bytes, altnames_text = read_zip_member(altnames_zip)
    body = sorted(rows(geoname_text, altnames_text), key=sort_key)
    provinces = sum(1 for r in body if r[1] == "PROVINCE")
    counties = sum(1 for r in body if r[1] == "COUNTY")
    header = [
        "# source: GeoNames gazetteer, Iran (IR) — administrative divisions, admin level 1 (province) and 2 "
        "(county / shahrestan)",
        f"# url: {geoname_url}",
        f"# sha256: {hashlib.sha256(geoname_bytes).hexdigest()}",
        "# altnames-source: GeoNames alternate names, Iran (IR) — Persian ('fa') names by geonameid",
        f"# altnames-url: {altnames_url}",
        f"# altnames-sha256: {hashlib.sha256(altnames_bytes).hexdigest()}",
        f"# retrieved: {retrieved}",
        "# licence: CC BY 4.0, https://creativecommons.org/licenses/by/4.0/ (GeoNames, https://www.geonames.org/"
        "about.html); config/license/allowed-licenses.json dataLicenses, ADR-0039",
        "# generator: tools/geodata/geonames_iran_divisions.py",
        "# notes: no district (admin level 3 / bakhsh) rows — GeoNames' Iran admin3 coverage is 7 records, far "
        "short of the real count (docs/DATA_TODO.md DT-021); a county with no published Persian alternate name "
        "(3 of 435, checked 2026-09-25) has an empty fa field",
        "# columns: " + ",".join(COLUMNS),
    ]
    lines = ["\t".join(fields) for fields in body]
    with open(output, "w", encoding="utf-8", newline="\n") as out:
        out.write("\n".join(header + lines) + "\n")
    print(f"{provinces} provinces and {counties} counties written to {output}")


if __name__ == "__main__":
    if len(sys.argv) != 7:
        sys.exit(__doc__)
    main(*sys.argv[1:])
