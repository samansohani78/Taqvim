#!/usr/bin/env python3
# Copyright (c) 2026 Saman Sohani. All Rights Reserved.
# Proprietary and confidential. See the LICENSE file in the repository root.
"""Builds data/location's iran-divisions.tsv (DT-021, DT-020) from the GeoNames gazetteer (CC BY 4.0).

Usage: geonames_iran_divisions.py <IR.zip> <geoname-url> <alternatenames-IR.zip> <altnames-url> <retrieved-date>
    <output.tsv>

Reads the two per-country GeoNames extracts for Iran (each a zip holding one 'IR.txt'):
  - the 'geoname' table dump (https://download.geonames.org/export/dump/IR.zip), from which the ADM1 (province) and
    ADM2 (county / shahrestan) rows are kept, with their own name, coordinates and admin1 code;
  - the alternate-names dump (https://download.geonames.org/export/dump/alternatenames/IR.zip), from which the
    Persian ('fa') name is kept for each division's geonameid (the alternate name flagged isPreferredName, or the
    first one when none is flagged), and likewise (DT-020) for Pashto (ps), Central Kurdish (ckb), Azerbaijani (az),
    Tamil (ta), Tajik (tg), Uzbek (uz), Malay (ms), Nepali (ne) and Kurmanji (kmr) — the same per-country dump
    already carries every language's alternate names for Iran's geonameids, not just Persian's, so no extra download
    is needed for these.

GeoNames' admin3 (district / bakhsh) coverage for Iran is 7 records out of an estimated 1000+ districts (checked
2026-09-25) — far too sparse to publish, so no district level is written; see docs/DATA_TODO.md DT-021.

Kurmanji (`kmr`) is NOT read from GeoNames' own `kmr` tag, which has 5 rows in the entire global dump and none for
Iran. It is read from the generic `ku` macrolanguage tag, disambiguated by script exactly as
geonames_place_names.py does it (that file's NOTES has the reasoning and the spot-check): Kurmanji is written in the
Latin "Hawar" alphabet and Central Kurdish in the Arabic script, so a `ku` name whose every letter is a Hawar letter
is Kurmanji, and the Arabic-script `ku` names are left to the separate `ckb` tag. All 31 provinces get one, and they
are unmistakably Kurmanji rather than romanized Central Kurdish — 'Xorasana Bakur', 'Azerbaycana Rojhilat' and
'Çarmihal û Bextiyarî' use the Kurmanji izafe and conjunction, not the Sorani ones.

Dari (`prs`) is still NOT written: GeoNames' `prs` tag has 6 rows worldwide and none for any Iran division (checked
2026-09-25 and again 2026-09-26), and there is no `fa-AF` tag at all. Dari and Iranian Persian write Iranian place
names identically, so `IranDivision` keeps its `prs`->`fa` same-script fallback instead (DT-020).

Nothing is translated, corrected or added: a division with no published alternate name in a given language gets an
empty field for it, the same convention cities.tsv uses for a name equal to the English one. Coverage of the 9
DT-020 languages is sparse and almost entirely limited to the 31 provinces (checked 2026-09-25: between 30 and 32 of
the 466 divisions have a name in each of ps/ckb/az/uz/ms/tg/ta/kmr, and only 1 has one in ne).
"""
import hashlib
import io
import sys
import zipfile

LEVELS = {"ADM1": "PROVINCE", "ADM2": "COUNTY"}
# DT-020: Pashto, Central Kurdish, Azerbaijani, Uzbek, Malay, Tajik, Nepali, Tamil, Kurmanji, grouped by script like
# cities.tsv. Each maps to the alternate-names language tag it is read from — the same code, except Kurmanji.
SOURCE_TAGS = {"ps": "ps", "ckb": "ckb", "az": "az", "uz": "uz", "ms": "ms", "tg": "tg", "ne": "ne", "ta": "ta",
               "kmr": "ku", "fa": "fa"}
DT_020_LANGUAGES = ["ps", "ckb", "az", "uz", "ms", "tg", "ne", "ta", "kmr"]
LOCALIZED_LANGUAGES = ["fa"] + DT_020_LANGUAGES

# The Kurmanji "Hawar" alphabet; a generic `ku` name written entirely in it is Kurmanji, not Central Kurdish.
HAWAR_ALPHABET = set("abcçdeêfghiîjklmnopqrsştuûvwxyz")

# Unicode format characters that carry no information in a name: bidi marks and embeddings, zero-width joiners and
# spaces, soft hyphen. GeoNames' crowd-sourced names pick these up — 7 of Iran's Persian province names ended with a
# stray U+200E before this was stripped.
FORMAT_CHARACTERS = dict.fromkeys(
    map(ord, "\u200b\u200c\u200d\u200e\u200f\u202a\u202b\u202c\u202d\u202e\u2066\u2067\u2068\u2069\u00ad"),
    None,
)


def is_kurmanji(name):
    """Whether a generic `ku` name is Kurmanji rather than Central Kurdish: written in the Latin Hawar alphabet."""
    letters = [character for character in name.lower() if character.isalpha()]
    return bool(letters) and all(character in HAWAR_ALPHABET for character in letters)
COLUMNS = ["code", "level", "parentCode", "en", "latitude", "longitude"] + LOCALIZED_LANGUAGES


def read_zip_member(path, member="IR.txt"):
    data = open(path, "rb").read()
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        text = archive.read(member).decode("utf-8")
    return data, text


def clean(value):
    text = value.translate(FORMAT_CHARACTERS).strip()
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


def read_localized_names(text, geoname_ids, languages):
    """The best name per (geonameid, language): prefer non-historic, then isPreferredName, then non-colloquial."""
    tags = {}
    for language in languages:
        tags.setdefault(SOURCE_TAGS[language], []).append(language)
    best = {}
    for line in text.splitlines():
        fields = line.split("\t")
        if len(fields) < 4:
            continue
        while len(fields) < 10:
            fields.append("")
        _, geoname_id, tag, name = fields[:4]
        is_preferred, _, is_colloquial, is_historic = fields[4:8]
        if tag not in tags or geoname_id not in geoname_ids:
            continue
        name = clean(name)
        if not name:
            continue
        score = (0 if is_historic == "1" else 1, 1 if is_preferred == "1" else 0, 0 if is_colloquial == "1" else 1)
        for language in tags[tag]:
            if language == "kmr" and not is_kurmanji(name):
                continue
            key = (geoname_id, language)
            if key not in best or score > best[key][0]:
                best[key] = (score, name)
    names = {}
    for (geoname_id, language), (_, name) in best.items():
        names.setdefault(geoname_id, {})[language] = name
    return names


def rows(geoname_text, altnames_text):
    geoname_rows = read_geoname_rows(geoname_text)
    localized = read_localized_names(altnames_text, set(geoname_rows), set(LOCALIZED_LANGUAGES))
    provinces = {r["admin1Code"]: r for r in geoname_rows.values() if r["level"] == "PROVINCE"}
    missing_parent = [r for r in geoname_rows.values() if r["level"] == "COUNTY" and r["admin1Code"] not in provinces]
    if missing_parent:
        ids = ", ".join(r["geonameId"] for r in missing_parent)
        raise ValueError(f"county row(s) with no matching province admin1 code: {ids}")
    result = []
    for r in geoname_rows.values():
        code = r["admin1Code"] if r["level"] == "PROVINCE" else r["geonameId"]
        parent_code = "" if r["level"] == "PROVINCE" else r["admin1Code"]
        names = localized.get(r["geonameId"], {})
        result.append(
            [
                code,
                r["level"],
                parent_code,
                r["name"],
                f"{r['latitude']:.5f}",
                f"{r['longitude']:.5f}",
            ]
            + [names.get(language, "") for language in LOCALIZED_LANGUAGES]
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
    coverage = {
        language: sum(1 for r in body if r[COLUMNS.index(language)]) for language in LOCALIZED_LANGUAGES
    }
    header = [
        "# source: GeoNames gazetteer, Iran (IR) — administrative divisions, admin level 1 (province) and 2 "
        "(county / shahrestan)",
        f"# url: {geoname_url}",
        f"# sha256: {hashlib.sha256(geoname_bytes).hexdigest()}",
        "# altnames-source: GeoNames alternate names, Iran (IR) — fa/ps/ckb/az/uz/ms/tg/ne/ta names by geonameid, "
        "plus kmr from the Latin-script (Hawar-alphabet) half of the generic `ku` tag (DT-020; prs checked and not "
        "usable, see this file's generator docstring)",
        f"# altnames-url: {altnames_url}",
        f"# altnames-sha256: {hashlib.sha256(altnames_bytes).hexdigest()}",
        f"# retrieved: {retrieved}",
        "# licence: CC BY 4.0, https://creativecommons.org/licenses/by/4.0/ (GeoNames, https://www.geonames.org/"
        "about.html); config/license/allowed-licenses.json dataLicenses, ADR-0039",
        "# generator: tools/geodata/geonames_iran_divisions.py",
        "# notes: no district (admin level 3 / bakhsh) rows — GeoNames' Iran admin3 coverage is 7 records, far "
        "short of the real count (docs/DATA_TODO.md DT-021); a division with no published alternate name in a "
        "language has an empty field for it; coverage per language (of 466 divisions): "
        + ", ".join(f"{language}={count}" for language, count in coverage.items()),
        "# columns: " + ",".join(COLUMNS),
    ]
    lines = ["\t".join(fields) for fields in body]
    with open(output, "w", encoding="utf-8", newline="\n") as out:
        out.write("\n".join(header + lines) + "\n")
    print(f"{provinces} provinces and {counties} counties written to {output}")
    print(f"coverage: {coverage}")


if __name__ == "__main__":
    if len(sys.argv) != 7:
        sys.exit(__doc__)
    main(*sys.argv[1:])
