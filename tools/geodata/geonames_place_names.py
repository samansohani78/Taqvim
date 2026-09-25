#!/usr/bin/env python3
# Copyright (c) 2026 Saman Sohani. All Rights Reserved.
# Proprietary and confidential. See the LICENSE file in the repository root.
"""Adds GeoNames-sourced place names to data/location's cities.tsv (DT-020).

Natural Earth's "Populated Places" (natural_earth_cities.py's own source) has no name field at all for Pashto (ps),
Central Kurdish (ckb), Kurmanji (kmr), Azerbaijani (az), Nepali (ne), Tamil (ta), Tajik (tg), Uzbek (uz) or Malay
(ms) — this script closes that gap for the 8 of those 9 languages GeoNames' `alternateNamesV2` export has usable
coverage for (see NOTES below for why `kmr` is not one of them; Dari `prs` is handled separately, see NOTES).

Usage: geonames_place_names.py <cities.tsv> <cities500.zip> <cities500-url> <alternateNamesV2.zip> <altnames-url>
    <retrieved-date> <output.tsv>

Pipeline:
  1. Read the existing cities.tsv (column-major, written by natural_earth_cities.py), keeping every existing column
     and header line unchanged.
  2. Build an index of GeoNames' `cities500` gazetteer (every place with population >= 500, small enough to bundle
     as an intermediate at ~14 MB zipped) keyed by (ISO country code, normalized name) -> [(geonameid, lat, lon)].
  3. Match each place in cities.tsv to a GeoNames geonameid: same country code, matching name (diacritics and case
     folded) against either GeoNames' `name` or `asciiname`, picking the nearest by great-circle distance among
     candidates. A place with no matching (country, name) key, or whose nearest candidate is more than
     MAX_DISTANCE_KM away, gets no geonameid and so no GeoNames-sourced name in any language (its GEONAMES_LANGUAGES
     columns stay empty, same convention as a Natural Earth language whose name equals English).
  4. Read `alternateNamesV2` (the global per-place alternate-names dump) and, for each matched geonameid, keep the
     best name in each of GEONAMES_LANGUAGES: prefer non-historic over historic, then the name flagged
     `isPreferredName`, then a non-colloquial name, in that order; first one found breaks further ties.
  5. Write the same places in the same order, with 8 new column lines appended.

NOTES (docs/DATA_TODO.md DT-020):
  - Matching uses cities500 rather than GeoNames' full `allCountries` dump (which is multiple GB) because Natural
    Earth's populated places are, in practice, almost all above the 500-population cutoff; unmatched places (about
    13% of cities.tsv, checked 2026-09-25) simply carry no GeoNames-sourced name, which is the same "no published
    name" convention already used for missing Natural Earth languages.
  - `kmr` (Kurmanji) is not added: GeoNames' own `kmr` tag exists (as does a separate `ckb` tag, honoring the
    ckb/kmr distinction by GeoNames' own language tag rather than guessing from the generic `ku` macrolanguage tag),
    but only 5 `kmr` rows exist in the entire global dump and none of them match any place bundled here, checked
    2026-09-25.
  - `prs` (Dari) is not added here either: GeoNames' `prs` tag has only 6 rows worldwide, of which exactly 1 matches
    a bundled place (a Russian city, geonameid 563708) — far too little to be worth a dedicated column, and `City`
    already gives `prs` a same-script fallback to the `fa` name (`City.NAME_FALLBACKS`), which this single stray
    match would silently override for every other place if `prs` were added to `City.PUBLISHED_LANGUAGES`.
"""
import hashlib
import io
import re
import sys
import unicodedata
import zipfile

# Grouped by script, matching cities.tsv's own convention (natural_earth_cities.py's LANGUAGES comment).
GEONAMES_LANGUAGES = ["ps", "ckb", "az", "uz", "ms", "tg", "ne", "ta"]
MAX_DISTANCE_KM = 100.0
EARTH_RADIUS_KM = 6371.0


def normalize(name):
    decomposed = unicodedata.normalize("NFKD", name)
    stripped = "".join(c for c in decomposed if not unicodedata.combining(c))
    return re.sub(r"[^a-z0-9]+", " ", stripped.lower()).strip()


def haversine_km(lat1, lon1, lat2, lon2):
    import math

    p1, p2 = math.radians(lat1), math.radians(lat2)
    dphi = math.radians(lat2 - lat1)
    dlambda = math.radians(lon2 - lon1)
    a = math.sin(dphi / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dlambda / 2) ** 2
    return 2 * EARTH_RADIUS_KM * math.asin(min(1.0, math.sqrt(a)))


def read_zip_member(path, member):
    data = open(path, "rb").read()
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        text = archive.read(member).decode("utf-8")
    return data, text


def read_cities_tsv(path):
    lines = open(path, encoding="utf-8").read().splitlines()
    header, columns, body = [], None, []
    for line in lines:
        if line.startswith("# columns: "):
            columns = line[len("# columns: ") :].split(",")
        elif line.startswith("#"):
            header.append(line)
        elif line != "":
            body.append(line)
    if columns is None:
        raise ValueError(f"{path}: no '# columns: ' header line")
    if len(body) != len(columns):
        raise ValueError(f"{path}: {len(body)} column lines, expected {len(columns)}")
    table = dict(zip(columns, (line.split("\t") for line in body)))
    sizes = {len(values) for values in table.values()}
    if len(sizes) != 1:
        raise ValueError(f"{path}: columns have differing place counts: {sizes}")
    return header, columns, table


def build_cities500_index(text):
    index = {}
    for line in text.splitlines():
        fields = line.split("\t")
        if len(fields) < 15:
            continue
        geoname_id, name, asciiname = fields[0], fields[1], fields[2]
        lat, lon, country = float(fields[4]), float(fields[5]), fields[8]
        for candidate_name in {name, asciiname}:
            key = (country, normalize(candidate_name))
            if key[1]:
                index.setdefault(key, []).append((geoname_id, lat, lon))
    return index


def match_geoname_ids(columns, index):
    """One geonameid (or None) per place in cities.tsv, in file order."""
    countries, english_names = columns["country"], columns["en"]
    latitudes, longitudes = columns["latitude"], columns["longitude"]
    ids = []
    for place in range(len(english_names)):
        country = countries[place]
        key = (country, normalize(english_names[place]))
        candidates = index.get(key)
        if not candidates:
            ids.append(None)
            continue
        lat, lon = float(latitudes[place]), float(longitudes[place])
        best_id, best_lat, best_lon = min(
            candidates, key=lambda c: haversine_km(lat, lon, c[1], c[2])
        )
        if haversine_km(lat, lon, best_lat, best_lon) > MAX_DISTANCE_KM:
            ids.append(None)
        else:
            ids.append(best_id)
    return ids


def resolve_alternate_names(altnames_text, wanted_geoname_ids, wanted_languages):
    """Best name per (geonameid, language): prefer non-historic, then preferred, then non-colloquial."""
    best = {}
    for line in altnames_text.splitlines():
        fields = line.split("\t")
        if len(fields) < 4:
            continue
        while len(fields) < 10:
            fields.append("")
        _, geoname_id, language, name = fields[:4]
        is_preferred, _, is_colloquial, is_historic = fields[4:8]
        if language not in wanted_languages or geoname_id not in wanted_geoname_ids:
            continue
        name = name.strip()
        if not name or any(c in name for c in "\t\r\n"):
            continue
        score = (
            0 if is_historic == "1" else 1,
            1 if is_preferred == "1" else 0,
            0 if is_colloquial == "1" else 1,
        )
        key = (geoname_id, language)
        if key not in best or score > best[key][0]:
            best[key] = (score, name)
    return {key: name for key, (score, name) in best.items()}


def main(cities_tsv, cities500_zip, cities500_url, altnames_zip, altnames_url, retrieved, output):
    header, columns, table = read_cities_tsv(cities_tsv)
    place_count = len(table["en"])

    cities500_bytes, cities500_text = read_zip_member(cities500_zip, "cities500.txt")
    index = build_cities500_index(cities500_text)
    geoname_ids = match_geoname_ids(table, index)
    matched = [g for g in geoname_ids if g is not None]

    altnames_bytes, altnames_text = read_zip_member(altnames_zip, "alternateNamesV2.txt")
    resolved = resolve_alternate_names(altnames_text, set(matched), set(GEONAMES_LANGUAGES))

    new_columns = {
        lang: [resolved.get((gid, lang), "") if gid else "" for gid in geoname_ids]
        for lang in GEONAMES_LANGUAGES
    }
    for lang in GEONAMES_LANGUAGES:
        non_empty = sum(1 for v in new_columns[lang] if v)
        print(f"{lang}: {non_empty} / {place_count} places named", file=sys.stderr)
    print(f"matched to a geonameid: {len(matched)} / {place_count}", file=sys.stderr)

    new_header_lines = [
        "# geonames-source: GeoNames gazetteer, cities500 (places with population >= 500), used only to match "
        "existing places to a GeoNames geonameid by name and coordinate",
        f"# geonames-url: {cities500_url}",
        f"# geonames-sha256: {hashlib.sha256(cities500_bytes).hexdigest()}",
        "# geonames-altnames-source: GeoNames alternateNamesV2 export, ps/ckb/az/uz/ms/tg/ne/ta names by geonameid "
        "(DT-020; kmr and prs checked and not usable, see this file's generator docstring)",
        f"# geonames-altnames-url: {altnames_url}",
        f"# geonames-altnames-sha256: {hashlib.sha256(altnames_bytes).hexdigest()}",
        f"# geonames-retrieved: {retrieved}",
        "# geonames-licence: CC BY 4.0, https://creativecommons.org/licenses/by/4.0/ (GeoNames, "
        "https://www.geonames.org/about.html); config/license/allowed-licenses.json dataLicenses, ADR-0039",
        f"# geonames-notes: matched {len(matched)}/{place_count} places to a geonameid (same country code, "
        "normalized name match, nearest of any candidates within 100 km); an unmatched place, or a matched place "
        "with no alternate name in a language, has an empty field for that language, the same convention already "
        "used for a Natural Earth language whose name equals the English one",
        "# generator: tools/geodata/geonames_place_names.py",
    ]
    final_columns_names = columns + GEONAMES_LANGUAGES
    final_header = (
        header
        + new_header_lines
        + ["# columns: " + ",".join(final_columns_names)]
    )
    body_lines = ["\t".join(table[c]) for c in columns] + [
        "\t".join(new_columns[lang]) for lang in GEONAMES_LANGUAGES
    ]
    with open(output, "w", encoding="utf-8", newline="\n") as out:
        out.write("\n".join(final_header + body_lines) + "\n")
    print(f"{place_count} places written to {output}", file=sys.stderr)


if __name__ == "__main__":
    if len(sys.argv) != 8:
        sys.exit(__doc__)
    main(*sys.argv[1:])
