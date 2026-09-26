#!/usr/bin/env python3
# Copyright (c) 2026 Saman Sohani. All Rights Reserved.
# Proprietary and confidential. See the LICENSE file in the repository root.
"""Adds GeoNames-sourced place names to data/location's cities.tsv (DT-020).

Natural Earth's "Populated Places" (natural_earth_cities.py's own source) has no name field at all for Pashto (ps),
Central Kurdish (ckb), Kurmanji (kmr), Azerbaijani (az), Nepali (ne), Tamil (ta), Tajik (tg), Uzbek (uz) or Malay
(ms) — this script closes that gap for all 9 from GeoNames' `alternateNamesV2` export (Dari `prs` is handled
separately, see NOTES).

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
     best name in each of GEONAMES_LANGUAGES, read from the export tag SOURCE_TAGS gives it: prefer non-historic
     over historic, then the name flagged `isPreferredName`, then a non-colloquial name, in that order; first one
     found breaks further ties.
  5. Write the same places in the same order, with 9 new column lines appended.

NOTES (docs/DATA_TODO.md DT-020):
  - Matching uses cities500 rather than GeoNames' full `allCountries` dump (which is multiple GB) because Natural
    Earth's populated places are, in practice, almost all above the 500-population cutoff; unmatched places (about
    13% of cities.tsv, checked 2026-09-25) simply carry no GeoNames-sourced name, which is the same "no published
    name" convention already used for missing Natural Earth languages.
  - `kmr` (Kurmanji) is NOT read from GeoNames' own `kmr` tag, which has only 5 rows in the entire global dump and
    matches no place bundled here (checked 2026-09-25 and again 2026-09-26). It is read from the generic `ku`
    macrolanguage tag instead, disambiguated by script: Kurmanji is written in the Latin "Hawar" alphabet and
    Central Kurdish in the Arabic script, so a `ku` name whose every letter is a Hawar letter is Kurmanji, and the
    Arabic-script `ku` names are Central Kurdish and are left to the separate `ckb` tag this script already reads.
    A Latin-script `ku` name is additionally dropped when it carries no Kurmanji information, i.e. when it folds to
    the English name (normalize()) without using any letter Kurmanji has and English does not (ç/ê/î/ş/û) — that is
    an untranslated exonym with its diacritics stripped, e.g. "Reykjavik" for Reykjavík, not a Kurmanji name. The
    result is 128 named places, checked name by name against Kurmanji Wikipedia (ku.wikipedia.org; Central Kurdish
    is ckb.wikipedia.org): 113 of the 132 candidates are attested there verbatim, 9 more differ only by an attested
    Kurmanji variant spelling, 8 have no Kurmanji Wikipedia entry but are plain Hawar renderings of the local name,
    and 1 was rejected by hand, see REJECTED_KURMANJI. Not a single surviving name turned out to be a romanized
    Central Kurdish form (those exist in the `ku` tag — "Shaxî", "Chemî", "Ṟûbarî …" — but only on streams and
    mountains, which this file does not carry).
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

# Grouped by script, matching cities.tsv's own convention (natural_earth_cities.py's LANGUAGES comment). Each maps
# to the `alternateNamesV2` language tag it is read from — the same code, except Kurmanji (see the NOTES above).
SOURCE_TAGS = {
    "ps": "ps",
    "ckb": "ckb",
    "az": "az",
    "uz": "uz",
    "ms": "ms",
    "tg": "tg",
    "ne": "ne",
    "ta": "ta",
    "kmr": "ku",
}
GEONAMES_LANGUAGES = list(SOURCE_TAGS)
MAX_DISTANCE_KM = 100.0
EARTH_RADIUS_KM = 6371.0

# The Kurmanji "Hawar" alphabet, and the five letters of it English does not have.
HAWAR_ALPHABET = set("abcçdeêfghiîjklmnopqrsştuûvwxyz")
KURMANJI_ONLY_LETTERS = set("çêîşû")

# Rejected by hand while spot-checking the `ku` names against Kurmanji Wikipedia (DT-020), keyed by geonameid.
REJECTED_KURMANJI = {
    # "Paolo" is the Italian spelling; Kurmanji Wikipedia has "São Paulo", as does GeoNames' own English name.
    "3448439": "Sao Paolo",
}

# Unicode format characters that carry no information in a name: bidi marks and embeddings, zero-width joiners and
# spaces, soft hyphen. GeoNames' crowd-sourced names pick these up (e.g. a trailing U+200E on Persian names).
FORMAT_CHARACTERS = dict.fromkeys(
    map(ord, "\u200b\u200c\u200d\u200e\u200f\u202a\u202b\u202c\u202d\u202e\u2066\u2067\u2068\u2069\u00ad"),
    None,
)


def is_kurmanji(name):
    """Whether a generic `ku` name is Kurmanji rather than Central Kurdish: written in the Latin Hawar alphabet."""
    letters = [character for character in name.lower() if character.isalpha()]
    return bool(letters) and all(character in HAWAR_ALPHABET for character in letters)


def carries_kurmanji_information(name, english_name):
    """False for an untranslated exonym: the English name with its non-Kurmanji diacritics and punctuation gone."""
    return normalize(name) != normalize(english_name) or bool(set(name.lower()) & KURMANJI_ONLY_LETTERS)


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
    tags = {}
    for language in wanted_languages:
        tags.setdefault(SOURCE_TAGS[language], []).append(language)
    best = {}
    for line in altnames_text.splitlines():
        fields = line.split("\t")
        if len(fields) < 4:
            continue
        while len(fields) < 10:
            fields.append("")
        _, geoname_id, tag, name = fields[:4]
        is_preferred, _, is_colloquial, is_historic = fields[4:8]
        if tag not in tags or geoname_id not in wanted_geoname_ids:
            continue
        name = name.translate(FORMAT_CHARACTERS).strip()
        if not name or any(c in name for c in "\t\r\n"):
            continue
        score = (
            0 if is_historic == "1" else 1,
            1 if is_preferred == "1" else 0,
            0 if is_colloquial == "1" else 1,
        )
        for language in tags[tag]:
            if language == "kmr" and not (is_kurmanji(name) and REJECTED_KURMANJI.get(geoname_id) != name):
                continue
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

    english_names = table["en"]

    def resolve(place, gid, lang):
        """The name for a place in a language, or "" — for `kmr` also "" when the name carries nothing English does
        not, the same "a name equal to the English one is left empty" convention natural_earth_cities.py uses."""
        name = resolved.get((gid, lang), "") if gid else ""
        if lang == "kmr" and name and not carries_kurmanji_information(name, english_names[place]):
            return ""
        return name

    new_columns = {
        lang: [resolve(place, gid, lang) for place, gid in enumerate(geoname_ids)]
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
        "# geonames-altnames-source: GeoNames alternateNamesV2 export, ps/ckb/az/uz/ms/tg/ne/ta names by geonameid, "
        "plus kmr from the Latin-script (Hawar-alphabet) half of the generic `ku` tag (DT-020; prs checked and not "
        "usable, see this file's generator docstring)",
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
